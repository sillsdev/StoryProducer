/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sil.storyproducer.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import org.sil.storyproducer.FileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Generate an MP4 file using OpenGL ES drawing commands.  Demonstrates the use of MediaMuxer
 * and MediaCodec with Surface input.
 * <p>
 * This uses various features first available in Android "Jellybean" 4.3 (API 18).  There is
 * no equivalent functionality in previous releases.
 * <p>
 * (This was derived from bits and pieces of CTS tests, and is packaged as such, but is not
 * currently part of CTS.)
 */
public class MyEncodeAndMuxTest {
    private static final String TAG = "EncodeAndMuxTest";
    private static final boolean VERBOSE = true;           // lots of logging

    // QVGA at 2Mbps
    // size of a frame, in pixels
    private static final int mWidth = 320;
    private static final int mHeight = 240;
    private Rect mScreenRect = new Rect(0, 0, mWidth, mHeight);
    // bit rate, in bits per second
    private static final int mVideoBitRate = 2000000;

    //not sure where this number comes from
    private static final int AUDIO_OFFSET = 100;

    // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
    private static final File OUTPUT_DIR = new File(FileSystem.getStoryPath("Fiery Furnace"));
    private static final Bitmap TEST_BITMAP = FileSystem.getImage("Fiery Furnace", 1);
    private static final String TEST_AUDIO_PATH = OUTPUT_DIR.getPath() + "/TestSound.mp3"; //"/recording1.mp3", "/narration0.wav"
    // Output filename.  Ideally this would use Context.getFilesDir() rather than a
    // hard-coded output directory.
    private static final String VIDEO_OUTPUT_PATH = new File(OUTPUT_DIR,
            "test." + mWidth + "x" + mHeight + ".mp4").toString();

    // parameters for the encoder
    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int VIDEO_FRAME_RATE = 15;               // 15fps
    private static final int VIDEO_IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int VIDEO_NUM_FRAMES = 30;               // two seconds of video

    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 8000;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_BITRATE = 64000;
    private static final int AUDIO_INPUT_BUFFER_SIZE = 16 * 1024;

    private static final int TIMEOUT_USEC = 10000;

    // encoder / muxer state
    private MediaCodec mAudioDecoder;
    private MediaCodec mAudioEncoder;
    private MediaCodec mVideoEncoder;
    private Surface mVideoSurface;
    private MediaMuxer mMuxer;
    private int mVideoTrackIndex = -1;

    private MediaExtractor mAudioExtractor;
    private int mAudioTrackIndex = -1;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private boolean mVideoOutputFormatEstablished = false;
    private boolean mAudioOutputFormatEstablished = false;
    private boolean mMuxerStarted = false;

    private int mVideoCurrentFrame = 0;
    private boolean mVideoGeneratorDone = false;
    private boolean mVideoEncoderDone = false;
    private boolean mVideoEOSSent = false;

    private boolean mAudioExtractorDone = false;
    private boolean mAudioDecoderDone = false;
    private boolean mAudioEncoderDone = false;

    /**
     * Tests encoding of AVC video from a Surface.  The output is saved as an MP4 file.
     */
    public void testEncodeVideoToMp4() {
        try {
            prepareVideoResources();
            prepareAudioResources();
            prepareMuxer();

            while(!mVideoEncoderDone || !mAudioEncoderDone) {
                videoDrawFrame();
                videoDrainEncoder(mVideoGeneratorDone);

                audioReadSample();
                audioDrainDecoder();
                audioDrainEncoder();
            }

//            for (int i = 0; i < VIDEO_NUM_FRAMES; i++) {
//                // Feed any pending encoder output into the muxer.
//                videoDrainEncoder(false);
//                videoDrawFrame();
//            }

            // send end-of-stream to encoder, and drain remaining output
//            videoDrainEncoder(true);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            // release encoder and muxer
            releaseVideoResources();
            releaseAudioResources();
            releaseMuxer();
        }

        // To test the result, open the file with MediaExtractor, and get the format.  Pass
        // that into the MediaCodec decoder configuration, along with a SurfaceTexture surface,
        // and examine the output with glReadPixels.
    }

    private void audioReadSample() {
        if(mAudioOutputFormatEstablished && !mVideoOutputFormatEstablished) {
            return;
        }
        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
//        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
//
//        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
//        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();

        if(!mAudioExtractorDone) {
            int inputBufIndex = mAudioDecoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (VERBOSE) Log.d(TAG, "inputBufIndex=" + inputBufIndex);
            if (inputBufIndex >= 0) {
                mBufferInfo.offset = AUDIO_OFFSET;
                ByteBuffer inputBuf = audioDecoderInputBuffers[inputBufIndex];
                mBufferInfo.size = mAudioExtractor.readSampleData(inputBuf, AUDIO_OFFSET);
                mBufferInfo.presentationTimeUs = mAudioExtractor.getSampleTime();
                if (mBufferInfo.size < 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "saw input EOS.");
                    }
                    mAudioDecoder.queueInputBuffer(inputBufIndex, 0, 0, mBufferInfo.presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mAudioExtractorDone = true;
                } else {
                    mAudioDecoder.queueInputBuffer(inputBufIndex, 0, mBufferInfo.size,
                            mBufferInfo.presentationTimeUs, /*0*/mAudioExtractor.getSampleFlags() /*audioBufferInfo.flags*/);
                    mAudioExtractor.advance();
                    if (VERBOSE) Log.d(TAG, "passed " + mBufferInfo.size + " bytes to decoder"
                            + (mAudioExtractorDone ? " (EOS)" : ""));
                }
            } else {
                // either all in use, or we timed out during initial setup
                if (VERBOSE) Log.d(TAG, "input buffer not available");
            }
        }
    }

    private void audioDrainDecoder() {
        if(mAudioOutputFormatEstablished && !mVideoOutputFormatEstablished) {
            return;
        }
//        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();

        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
//        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();

        while(!mAudioDecoderDone) {
            int decoderStatus = mAudioDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (VERBOSE) Log.d(TAG, "no output from decoder available");
                break;
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // The storage associated with the direct ByteBuffer may already be unmapped,
                // so attempting to access data through the old output buffer array could
                // lead to a native crash.
                if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // this happens before the first frame is returned
                MediaFormat decoderOutputFormat = mAudioDecoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                        decoderOutputFormat);
            } else if (decoderStatus < 0) {
                System.out.println("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
            } else {  // decoderStatus >= 0
                ByteBuffer decodedData = audioDecoderOutputBuffers[decoderStatus];
                if (decodedData == null) {
                    System.out.println("audioDecoderOutputBuffer " + decoderStatus + " was null");
                }
                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                decodedData.position(mBufferInfo.offset);
                decodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    throw new RuntimeException("Audio encoder already configured!");

                    // Codec config info.  Only expected on first packet.  One way to
                    // handle this is to manually stuff the data into the MediaFormat
                    // and pass that to configure().  We do that here to exercise the API.

//                                if (audioEncoderConfigured)
//                                    System.out.println("Encoder already configured!");
//
//                                MediaFormat encoderFormat =
//                                        MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, 60, 1);
//                                encoderFormat.setByteBuffer("csd-0", decodedData);
//                                audioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//                                audioEncoder.start();
//                                audioEncoderInputBuffers = audioEncoder.getInputBuffers();
//                                audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
//                                audioEncoderConfigured = true;
//                                if (VERBOSE)
//                                    Log.d(TAG, "decoder configured (" + mBufferInfo.size + " bytes)");
                } else {
//                                if (!audioEncoderConfigured)
//                                    System.out.println("Encoder not configured!");

                    // Get an encoder input buffer, blocking until it's available.
                    int inputBufIndex = mAudioEncoder.dequeueInputBuffer(-1);
                    ByteBuffer inputBuf = audioEncoderInputBuffers[inputBufIndex];

                    inputBuf.clear();
                    inputBuf.put(decodedData);
                    mAudioEncoder.queueInputBuffer(inputBufIndex, 0, mBufferInfo.size,
                            mBufferInfo.presentationTimeUs, mBufferInfo.flags);
                    mAudioDecoderDone = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    if (VERBOSE)
                        Log.d(TAG, "passed " + mBufferInfo.size + " bytes to encoder"
                                + (mAudioDecoderDone ? " (EOS)" : ""));
                }
                mAudioDecoder.releaseOutputBuffer(decoderStatus, false);
            }
        }
    }

    private void audioDrainEncoder() {
        if(mAudioOutputFormatEstablished && !mVideoOutputFormatEstablished) {
            return;
        }
//        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
//        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
//
//        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();

        while(!mAudioEncoderDone) {
            int audioEncoderStatus = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (audioEncoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (VERBOSE) Log.d(TAG, "no output from audio encoder available");
                break;
            } else if (audioEncoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();
                if (VERBOSE) Log.d(TAG, "encoder output buffers changed");
            } else if (audioEncoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newEncoderFormat = mAudioEncoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newEncoderFormat);
                // should happen before receiving buffers, and should only happen once
                if (mAudioOutputFormatEstablished) {
                    throw new RuntimeException("format changed twice");
                }

                // now that we have the Magic Goodies, start the muxer
                mAudioTrackIndex = mMuxer.addTrack(newEncoderFormat);
                mAudioOutputFormatEstablished = true;

                if(mVideoOutputFormatEstablished) {
                    mMuxer.start();
                    mMuxerStarted = true;
                }
                else {
                    //Don't start muxing until muxer is started.
                    break;
                }
            } else if (audioEncoderStatus < 0) {
                System.out.println("unexpected result from encoder.dequeueOutputBuffer: " + audioEncoderStatus);
            } else { // encoderStatus >= 0
                ByteBuffer encodedData = audioEncoderOutputBuffers[audioEncoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + audioEncoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mAudioTrackIndex, encodedData, mBufferInfo);
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mAudioEncoder.releaseOutputBuffer(audioEncoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                                if (!endOfStream) {
//                                    Log.w(TAG, "reached end of stream unexpectedly");
//                                } else {
                    if (VERBOSE) Log.d(TAG, "end of stream reached");
//                                }
                    mAudioEncoderDone = true;      // out of while
                }
            }
        }
    }

    private void videoDrawFrame() {
        if(mVideoOutputFormatEstablished && !mAudioOutputFormatEstablished) {
            return;
        }
        if(!mVideoGeneratorDone) {
            // Generate a new frame of input.
            Canvas cnv = mVideoSurface.lockCanvas(null);
            cnv.drawBitmap(TEST_BITMAP, new Rect(0, 0, 50 + 10 * mVideoCurrentFrame, 50 + 5 * mVideoCurrentFrame), mScreenRect, null);

            // Submit it to the encoder.  The eglSwapBuffers call will block if the input
            // is full, which would be bad if it stayed full until we dequeued an output
            // buffer (which we can't do, since we're stuck here).  So long as we fully drain
            // the encoder before supplying additional input, the system guarantees that we
            // can supply another frame without blocking.
            if (VERBOSE) Log.d(TAG, "sending frame " + mVideoCurrentFrame + " to encoder");
            mVideoSurface.unlockCanvasAndPost(cnv);

            mVideoCurrentFrame++;
            if (mVideoCurrentFrame >= VIDEO_NUM_FRAMES) {
                mVideoGeneratorDone = true;
            }
        }
    }

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private void prepareVideoResources() {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoSurface = mVideoEncoder.createInputSurface();
//            mInputSurface = new CodecInputSurface(mVideoEncoder.createInputSurface());
            mVideoEncoder.start();
        } catch (IOException ioe) {
            throw new RuntimeException("MediaCodec creation failed", ioe);
        }
    }

    private void prepareAudioResources() {
        // Set up MediaExtractor to read from the source.
        mAudioExtractor = new MediaExtractor();
        try {
            mAudioExtractor.setDataSource(TEST_AUDIO_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAudioExtractor.selectTrack(0);

        MediaFormat audioFormat = mAudioExtractor.getTrackFormat(0);
        try {
            mAudioDecoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
            mAudioDecoder.configure(audioFormat, null, null, 0);

            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);

            MediaFormat encoderFormat =
                    MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
            encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
            //encoder input buffers are too small by default
            encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_INPUT_BUFFER_SIZE);
            try {
                mAudioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        mAudioDecoder.start();
        mAudioEncoder.start();
    }

    private void prepareMuxer() {
        Log.d(TAG, "output file is " + VIDEO_OUTPUT_PATH);

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            mMuxer = new MediaMuxer(VIDEO_OUTPUT_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseVideoResources() {
        if (VERBOSE) Log.d(TAG, "releasing video encoder");
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
    }

    private void releaseAudioResources() {
        if (VERBOSE) Log.d(TAG, "releasing audio resources");
        if (mAudioExtractor != null) {
            mAudioExtractor.release();
            mAudioExtractor = null;
        }
        if (mAudioDecoder != null) {
            mAudioDecoder.stop();
            mAudioDecoder.release();
            mAudioDecoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
    }

    private void releaseMuxer() {
        if (VERBOSE) Log.d(TAG, "releasing muxer");
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    private void videoDrainEncoder(boolean endOfStream) {
        if(mVideoOutputFormatEstablished && !mAudioOutputFormatEstablished) {
            return;
        }

        if (VERBOSE) Log.d(TAG, "videoDrainEncoder(" + endOfStream + ")");

        if (endOfStream && !mVideoEOSSent) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
            mVideoEOSSent = true;
        }

        ByteBuffer[] videoEncoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        while (true) {
            int videoEncoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (videoEncoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
//                // no output available yet
//                if (!endOfStream) {
//                    break;      // out of while
//                } else {
//                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
//                }
            } else if (videoEncoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                videoEncoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            } else if (videoEncoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);
                // should happen before receiving buffers, and should only happen once
                if (mVideoOutputFormatEstablished) {
                    throw new RuntimeException("format changed twice");
                }

                // now that we have the Magic Goodies, start the muxer
                mVideoTrackIndex = mMuxer.addTrack(newFormat);
                mVideoOutputFormatEstablished = true;

                if(mAudioOutputFormatEstablished) {
                    mMuxer.start();
                    mMuxerStarted = true;
                }
                else {
                    //Don't start muxing until muxer is started.
                    break;
                }
            } else if (videoEncoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        videoEncoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = videoEncoderOutputBuffers[videoEncoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + videoEncoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mVideoEncoder.releaseOutputBuffer(videoEncoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    mVideoEncoderDone = true;
                    break;      // out of while
                }
            }
        }
    }
}
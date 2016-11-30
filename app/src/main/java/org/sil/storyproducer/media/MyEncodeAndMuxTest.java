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

package org.sil.storyproducer.media;

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
import org.sil.storyproducer.media.pipe.PipedAudioConcatenator;
import org.sil.storyproducer.media.pipe.PipedAudioMixer;
import org.sil.storyproducer.media.pipe.PipedAudioResampler;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceSource;
import org.sil.storyproducer.media.pipe.PipedMediaDecoder;
import org.sil.storyproducer.media.pipe.PipedMediaEncoder;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.media.pipe.PipedMediaExtractor;
import org.sil.storyproducer.media.pipe.PipedMediaMuxer;
import org.sil.storyproducer.media.pipe.SourceUnacceptableException;
import org.sil.storyproducer.media.videostory.StoryPage;
import org.sil.storyproducer.media.videostory.VideoStoryMaker;

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

    //not sure where this number comes from
    private static final int AUDIO_OFFSET = 0;

    // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
    private static final File OUTPUT_DIR = new File(FileSystem.getStoryPath("Fiery Furnace"));
    private static final Bitmap TEST_BITMAP = FileSystem.getImage("Fiery Furnace", 1);
    private static final String TEST_IMG_1 = OUTPUT_DIR.getPath() + "/1.jpg";
    private static final String TEST_IMG_2 = OUTPUT_DIR.getPath() + "/4.jpg";
    private static final String TEST_AUDIO_PATH = OUTPUT_DIR.getPath() + "/TestSound.mp3"; //"/recording1.mp3", "/narration0.wav"
    private static final String TEST_AUDIO_PATH_2 = OUTPUT_DIR.getPath() + "/narration0.wav";
    private static final String TEST_AUDIO_PATH_3 = OUTPUT_DIR.getPath() + "/narration1.wav";
    // Output filename.  Ideally this would use Context.getFilesDir() rather than a
    // hard-coded output directory.
    private String mOutputPath = new File(OUTPUT_DIR,
            "test2." + mWidth + "x" + mHeight + ".mp4").toString();

    // parameters for the encoder
    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int VIDEO_LENGTH_SECONDS = 16;
    private static final long VIDEO_LENGTH_US = 1000000L * VIDEO_LENGTH_SECONDS;
    private static final int VIDEO_FRAME_RATE = 30;               // 30fps
    private static final int VIDEO_IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int VIDEO_NUM_FRAMES = VIDEO_LENGTH_SECONDS * VIDEO_FRAME_RATE;
    // bit rate, in bits per second
    private static final int VIDEO_BIT_RATE = 32 * mWidth * mHeight * VIDEO_FRAME_RATE / 100;

    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 48000;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_BITRATE = 64000;

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

    MediaCodec.BufferInfo videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
    MediaCodec.BufferInfo audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
    MediaCodec.BufferInfo audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

    // We will get this from the decoder when notified of a format change.
    private MediaFormat decoderOutputAudioFormat = null;
    // We will get these from the encoders when notified of a format change.
    private MediaFormat encoderOutputVideoFormat = null;
    private MediaFormat encoderOutputAudioFormat = null;

    // The audio decoder output buffer to process, -1 if none.
    private int pendingAudioDecoderOutputBufferIndex = -1;

    //For error correction (http://stackoverflow.com/questions/26504386/mediacodec-audio-video-muxing-issues-ond-android)
    private long audioPresentationTimeUsLast = 0;

    private boolean mUseAudio = true;
    private boolean mUseVideo = true;

    private boolean mMuxerStarted = false;

    private int mVideoGeneratorFrame = 0;
    private int mVideoEncoderFrame = 0;
    private boolean mVideoGeneratorDone = false;
    private boolean mVideoEncoderDone = false;
    private boolean mVideoEOSSent = false;

    private boolean mAudioExtractorDone = false;
    private boolean mAudioDecoderDone = false;
    private boolean mAudioEncoderDone = false;

    private static boolean started = false;

    public void runTest() {
        //Bandaid for run on startup
//        if(started) {
//            return;
//        }
        started = true;

//        testEncodeVideoToMp4();
//        runPipedTest();
        runVideoStoryMakerTest();
    }

    public void runVideoStoryMakerTest() {
        String outputPath = new File(OUTPUT_DIR,
                "testPipe" + (mUseAudio ? "A" : "") + (mUseVideo ? "V" : "") + "." + mWidth + "x" + mHeight + ".mp4").toString();
        int outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        MediaFormat audioFormat = MediaHelper.createFormat(AUDIO_MIME_TYPE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AUDIO_SAMPLE_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT);

        KenBurnsEffect kbfx1 = new KenBurnsEffect(new Rect(20, 20, 40, 40), BitmapManager.getDimensions(TEST_IMG_1));
        Rect r2 = BitmapManager.getDimensions(TEST_IMG_2);
        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "image 2 rectangle: (" + r2.left + ", " + r2.top + ", "
                    + r2.right + ", " + r2.bottom + ")");
        }
        KenBurnsEffect kbfx2 = new KenBurnsEffect(new Rect(0, 0, r2.right - 50, r2.bottom), new Rect(50, 0, r2.right, r2.bottom));

        StoryPage[] pages = {
                new StoryPage(new File(TEST_IMG_1), new File(TEST_AUDIO_PATH_2), kbfx1),
                new StoryPage(new File(TEST_IMG_2), new File(TEST_AUDIO_PATH_3), kbfx2),
        };

        File soundtrack = new File(OUTPUT_DIR.getPath() + "/TestSound.mp3");

        long slideTransitionUs = 3000000;
        long audioTransitionUs = 500000;

        VideoStoryMaker maker = new VideoStoryMaker(new File(outputPath), outputFormat, videoFormat, audioFormat,
                pages, soundtrack, audioTransitionUs, slideTransitionUs);
        maker.churn();
    }

    public void runPipedTest() {
        String outputPath = new File(OUTPUT_DIR,
                "testPipe" + (mUseAudio ? "A" : "") + (mUseVideo ? "V" : "") + "." + mWidth + "x" + mHeight + ".mp4").toString();

        PipedMediaExtractor audioExtractor = null;
        PipedMediaDecoder audioDecoder = null;
        PipedMediaExtractor audioExtractor2 = null;
        PipedMediaDecoder audioDecoder2 = null;
        PipedMediaExtractor audioExtractor3 = null;
        PipedMediaDecoder audioDecoder3 = null;

        PipedAudioConcatenator audioConcatenator = null;

        PipedAudioResampler audioResampler = null;
        PipedAudioResampler audioResampler2 = null;
        PipedAudioResampler audioResampler3 = null;
        PipedAudioMixer audioMixer = null;

        PipedMediaEncoder audioEncoder = null;

        PipedVideoSurfaceEncoder videoEncoder = null;

        PipedMediaMuxer muxer = null;

        try {
            muxer = new PipedMediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            if(mUseAudio) {
                audioExtractor = new PipedMediaExtractor(TEST_AUDIO_PATH, MediaHelper.MediaType.AUDIO);
                audioExtractor2 = new PipedMediaExtractor(TEST_AUDIO_PATH_2, MediaHelper.MediaType.AUDIO);
                audioExtractor3 = new PipedMediaExtractor(TEST_AUDIO_PATH_3, MediaHelper.MediaType.AUDIO);

                audioDecoder = new PipedMediaDecoder();
                audioDecoder.addSource(audioExtractor);

                audioDecoder2 = new PipedMediaDecoder();
                audioDecoder2.addSource(audioExtractor2);

                audioDecoder3 = new PipedMediaDecoder();
                audioDecoder3.addSource(audioExtractor3);

                final int sampleRate = 48000;
                audioResampler = new PipedAudioResampler(sampleRate, 2);
                audioResampler.addSource(audioDecoder);

                audioResampler2 = new PipedAudioResampler(sampleRate, 2);
                audioResampler2.addSource(audioDecoder2);

                audioResampler3 = new PipedAudioResampler(sampleRate, 2);
                audioResampler3.addSource(audioDecoder3);

                audioConcatenator = new PipedAudioConcatenator(1000000);
//                audioConcatenator.addSource(audioResampler2);
//                audioConcatenator.addSource(audioResampler3);

                audioMixer = new PipedAudioMixer();
                audioMixer.addSource(audioResampler);
                audioMixer.addSource(audioConcatenator);

                MediaFormat audioFormat = MediaHelper.createFormat(AUDIO_MIME_TYPE);
                audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
                if (VERBOSE) Log.d(TAG, "audio format: " + audioFormat);

                audioEncoder = new PipedMediaEncoder(audioFormat);
//                audioEncoder.addSource(audioDecoder);
//                audioEncoder.addSource(audioResampler);
                audioEncoder.addSource(audioMixer);

                muxer.addSource(audioEncoder);
            }
            if(mUseVideo) {
                PipedVideoSurfaceSource videoDrawer = new PipedVideoSurfaceSource() {
                    @Override
                    public void close() {

                    }

                    @Override
                    public void setup() throws IOException {

                    }

                    @Override
                    public MediaFormat getOutputFormat() {
                        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);

                        // Set some properties.  Failing to specify some of these can cause the MediaCodec
                        // configure() call to throw an unhelpful exception.
                        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

                        if (MediaHelper.VERBOSE) Log.d(TAG, "video format: " + format);

                        return format;
                    }

                    @Override
                    public boolean isDone() {
                        return mVideoGeneratorFrame >= VIDEO_NUM_FRAMES;
                    }

                    @Override
                    public long fillCanvas(Canvas canv) {
                        float percent = mVideoGeneratorFrame / (float) VIDEO_NUM_FRAMES;
                        int x = (int) (percent * TEST_BITMAP.getWidth());
                        int y = (int) (percent * TEST_BITMAP.getHeight());
                        canv.drawBitmap(TEST_BITMAP, new Rect(0, 0, x, y), mScreenRect, null);

                        if (MediaHelper.VERBOSE)
                            Log.d(TAG, "sending frame " + mVideoGeneratorFrame + " to encoder");

                        mVideoGeneratorFrame++;

                        return (long) (percent * VIDEO_LENGTH_US);
                    }
                };

                videoEncoder = new PipedVideoSurfaceEncoder();
                videoEncoder.addSource(videoDrawer);

                muxer.addSource(videoEncoder);
            }

            muxer.crunch();
            Log.d(TAG, "muxer.crunch complete");
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (SourceUnacceptableException e) {
            e.printStackTrace();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        finally {
            if(muxer != null) {
                muxer.close();
            }
            if(audioEncoder != null) {
                audioEncoder.close();
            }
            if(audioDecoder != null) {
                audioDecoder.close();
            }
            if(audioExtractor != null) {
                audioExtractor.close();
            }
            if(videoEncoder != null) {
                videoEncoder.close();
            }
        }
    }

    /**
     * Tests encoding of AVC video from a Surface.  The output is saved as an MP4 file.
     */
    public void testEncodeVideoToMp4() {
        if(started) {
            return;
        }
        started = true;
        try {
            prepareVideoResources();
            prepareAudioResources();
            prepareMuxer();

            while((mUseVideo && !mVideoEncoderDone) || (mUseAudio && !mAudioEncoderDone)) {
                videoDrawFrame();
                videoDrainEncoder(mVideoGeneratorDone);

                audioReadSample();
                audioDrainDecoder();
                audioDrainEncoder();

                if (!mMuxerStarted
                        && (!mUseAudio || encoderOutputAudioFormat != null)
                        && (!mUseVideo || encoderOutputVideoFormat != null)) {
                    if (mUseVideo) {
                        Log.d(TAG, "muxer: adding video track.");
                        mVideoTrackIndex = mMuxer.addTrack(encoderOutputVideoFormat);
                    }
                    if (mUseAudio) {
                        Log.d(TAG, "muxer: adding audio track.");
                        mAudioTrackIndex = mMuxer.addTrack(encoderOutputAudioFormat);
                    }
                    Log.d(TAG, "muxer: starting");
                    mMuxer.start();
                    mMuxerStarted = true;
                }
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
        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
//        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
//
//        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
//        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();

        // Extract audio from file and feed to decoder.
        // Do not extract audio if we have determined the output format but we are not yet
        // ready to mux the frames.
        while (mUseAudio && !mAudioExtractorDone
                && (encoderOutputAudioFormat == null || mMuxerStarted)) {
            int decoderInputBufferIndex = mAudioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "no audio decoder input buffer");
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: returned input buffer: " + decoderInputBufferIndex);
            }
            ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
            decoderInputBuffer.clear();
            int size = mAudioExtractor.readSampleData(decoderInputBuffer, 0);
            long presentationTime = mAudioExtractor.getSampleTime();
            if (VERBOSE) {
                Log.d(TAG, "audio extractor: returned buffer of size " + size);
                Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
            }
            if (size >= 0) {
                mAudioDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        size,
                        presentationTime,
                        mAudioExtractor.getSampleFlags());
            }
            mAudioExtractorDone = !mAudioExtractor.advance();
            if (mAudioExtractorDone) {
                if (VERBOSE) Log.d(TAG, "audio extractor: EOS");
                mAudioDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        0,
                        0, //presentationTime?
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
//            audioExtractedFrameCount++;
            // We extracted a frame, let's try something else next.
//            break;
        }





//        if(mUseAudio && !mAudioExtractorDone
//                && (encoderOutputAudioFormat == null || mMuxerStarted)) {
//            int inputBufIndex = mAudioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
//
////            if (VERBOSE) Log.d(TAG, "inputBufIndex=" + inputBufIndex);
//            if (inputBufIndex >= 0) {
//                ByteBuffer inputBuf = audioDecoderInputBuffers[inputBufIndex];
//                int size = mAudioExtractor.readSampleData(inputBuf, AUDIO_OFFSET);
//                long presentationTimeUs = mAudioExtractor.getSampleTime();
//                if (size < 0) {
//                    if (VERBOSE) {
//                        Log.d(TAG, "saw input EOS.");
//                    }
//                    size = 0;
//                    mAudioDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0,
//                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    mAudioExtractorDone = true;
//                } else {
//                    mAudioDecoder.queueInputBuffer(inputBufIndex, 0, size,
//                            presentationTimeUs, /*0*/mAudioExtractor.getSampleFlags() /*audioBufferInfo.flags*/);
//                    mAudioExtractor.advance();
//                }
//                if (VERBOSE) Log.d(TAG, "passed " + size + " bytes to decoder"
//                        + (mAudioExtractorDone ? " (EOS)" : ""));
//            } else {
//                // either all in use, or we timed out during initial setup
//                if (VERBOSE) Log.d(TAG, "input buffer not available");
//            }
//        }
    }

    private void audioDrainDecoder() {
//        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();

        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
//        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();


        // Poll output frames from the audio decoder.
        // Do not poll if we already have a pending buffer to feed to the encoder.
        while (mUseAudio && !mAudioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1
                && (encoderOutputAudioFormat == null || mMuxerStarted)) {
            int decoderOutputBufferIndex =
                    mAudioDecoder.dequeueOutputBuffer(
                            audioDecoderOutputBufferInfo, TIMEOUT_USEC);
            if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "no audio decoder output buffer");
                break;
            }
            if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (VERBOSE) Log.d(TAG, "audio decoder: output buffers changed");
                audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
                break;
            }
            if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                decoderOutputAudioFormat = mAudioDecoder.getOutputFormat();
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: output format changed: "
                            + decoderOutputAudioFormat);
                }
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: returned output buffer: "
                        + decoderOutputBufferIndex);
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: returned buffer of size "
                        + audioDecoderOutputBufferInfo.size);
            }
            ByteBuffer decoderOutputBuffer =
                    audioDecoderOutputBuffers[decoderOutputBufferIndex];
            if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    != 0) {
                if (VERBOSE) Log.d(TAG, "audio decoder: codec config buffer");
                mAudioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: returned buffer for time "
                        + audioDecoderOutputBufferInfo.presentationTimeUs);
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: output buffer is now pending: "
                        + pendingAudioDecoderOutputBufferIndex);
            }
            pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
//            audioDecodedFrameCount++;
            // We extracted a pending frame, let's try something else next.
//            break;
        }




        // Feed the pending decoded audio buffer to the audio encoder.
        while (mUseAudio && pendingAudioDecoderOutputBufferIndex != -1) {
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: attempting to process pending buffer: "
                        + pendingAudioDecoderOutputBufferIndex);
            }
            int encoderInputBufferIndex = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
            if (encoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "no audio encoder input buffer");
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio encoder: returned input buffer: " + encoderInputBufferIndex);
            }
            ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
//            encoderInputBuffer.clear();
            int size = audioDecoderOutputBufferInfo.size;
            long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: processing pending buffer: "
                        + pendingAudioDecoderOutputBufferIndex);
            }
            if (VERBOSE) {
                Log.d(TAG, "audio decoder: pending buffer of size " + size);
                Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);
            }
            if (size >= 0) {
                ByteBuffer decoderOutputBuffer =
                        audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex]
                                .duplicate();
                decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
//                encoderInputBuffer.position(0);
                encoderInputBuffer.clear();
                encoderInputBuffer.put(decoderOutputBuffer);
                mAudioEncoder.queueInputBuffer(
                        encoderInputBufferIndex,
                        0,
                        size,
                        presentationTime,
                        audioDecoderOutputBufferInfo.flags);
            }
            mAudioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
            pendingAudioDecoderOutputBufferIndex = -1;
            if ((audioDecoderOutputBufferInfo.flags
                    & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (VERBOSE) Log.d(TAG, "audio decoder: EOS");
                mAudioDecoderDone = true;
            }
            // We enqueued a pending frame, let's try something else next.
//            break;
        }






//        while(!mAudioDecoderDone) {
//            int decoderStatus = mAudioDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
//            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                // no output available yet
//                if (VERBOSE) Log.d(TAG, "no output from decoder available");
//                break;
//            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                // The storage associated with the direct ByteBuffer may already be unmapped,
//                // so attempting to access data through the old output buffer array could
//                // lead to a native crash.
//                if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
//                audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
//            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                // this happens before the first frame is returned
//                MediaFormat decoderOutputFormat = mAudioDecoder.getOutputFormat();
//                if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
//                        decoderOutputFormat);
//            } else if (decoderStatus < 0) {
//                System.out.println("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
//            } else {  // decoderStatus >= 0
//                ByteBuffer decodedData = audioDecoderOutputBuffers[decoderStatus];
//                if (decodedData == null) {
//                    System.out.println("audioDecoderOutputBuffer " + decoderStatus + " was null");
//                }
//                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
//                decodedData.position(mBufferInfo.offset);
//                decodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    throw new RuntimeException("Audio encoder already configured!");
//
//                    // Codec config info.  Only expected on first packet.  One way to
//                    // handle this is to manually stuff the data into the MediaFormat
//                    // and pass that to configure().  We do that here to exercise the API.
//
////                                if (audioEncoderConfigured)
////                                    System.out.println("Encoder already configured!");
////
////                                MediaFormat encoderFormat =
////                                        MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, 60, 1);
////                                encoderFormat.setByteBuffer("csd-0", decodedData);
////                                audioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
////                                audioEncoder.start();
////                                audioEncoderInputBuffers = audioEncoder.getInputBuffers();
////                                audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
////                                audioEncoderConfigured = true;
////                                if (VERBOSE)
////                                    Log.d(TAG, "decoder configured (" + mBufferInfo.size + " bytes)");
//                } else {
////                                if (!audioEncoderConfigured)
////                                    System.out.println("Encoder not configured!");
//
//                    // Get an encoder input buffer, blocking until it's available.
//                    int inputBufIndex = mAudioEncoder.dequeueInputBuffer(-1);
//                    ByteBuffer inputBuf = audioEncoderInputBuffers[inputBufIndex];
//
//                    inputBuf.clear();
//                    inputBuf.put(decodedData);
//                    mAudioEncoder.queueInputBuffer(inputBufIndex, 0, mBufferInfo.size,
//                            mBufferInfo.presentationTimeUs, mBufferInfo.flags);
//                    mAudioDecoderDone = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
//                    if (VERBOSE)
//                        Log.d(TAG, "passed " + mBufferInfo.size + " bytes to encoder"
//                                + (mAudioDecoderDone ? " (EOS)" : ""));
//                }
//                mAudioDecoder.releaseOutputBuffer(decoderStatus, false);
//            }
//        }
    }

    private void audioDrainEncoder() {
//        ByteBuffer[] audioDecoderInputBuffers = mAudioDecoder.getInputBuffers();
//        ByteBuffer[] audioDecoderOutputBuffers = mAudioDecoder.getOutputBuffers();
//
//        ByteBuffer[] audioEncoderInputBuffers = mAudioEncoder.getInputBuffers();
        ByteBuffer[] audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();



        // Poll frames from the audio encoder and send them to the muxer.
        while (mUseAudio && !mAudioEncoderDone
                && (encoderOutputAudioFormat == null || mMuxerStarted)) {
            int encoderOutputBufferIndex = mAudioEncoder.dequeueOutputBuffer(
                    audioEncoderOutputBufferInfo, TIMEOUT_USEC);
            if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, "no audio encoder output buffer");
                break;
            }
            if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (VERBOSE) Log.d(TAG, "audio encoder: output buffers changed");
                audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();
                break;
            }
            if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (VERBOSE) Log.d(TAG, "audio encoder: output format changed");
                if (mAudioTrackIndex >= 0) {
                    throw new RuntimeException("audio encoder changed its output format again?");
                }
                encoderOutputAudioFormat = mAudioEncoder.getOutputFormat();
                break;
            }

            if (VERBOSE) {
                Log.d(TAG, "audio encoder: returned output buffer: "
                        + encoderOutputBufferIndex);
                Log.d(TAG, "audio encoder: returned buffer of size "
                        + audioEncoderOutputBufferInfo.size);
            }
            ByteBuffer encoderOutputBuffer =
                    audioEncoderOutputBuffers[encoderOutputBufferIndex];
            if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                    != 0) {
                if (VERBOSE) Log.d(TAG, "audio encoder: codec config buffer");
                // Simply ignore codec config buffers.
                mAudioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                break;
            }
            if (VERBOSE) {
                Log.d(TAG, "audio encoder: returned buffer for time "
                        + audioEncoderOutputBufferInfo.presentationTimeUs);

//                long nextTimeExpected = audioEncoderOutputBufferInfo.presentationTimeUs
//                        + (audioEncoderOutputBufferInfo.size / 2 / encoderOutputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) * ();
//                Log.d(TAG, "audio encoder: expecting next time " + nextTimeExpected);
            }

            if (audioPresentationTimeUsLast > audioEncoderOutputBufferInfo.presentationTimeUs) {
                if(VERBOSE) Log.d(TAG, "audio encoder: correcting presentation time from "
                        + audioEncoderOutputBufferInfo.presentationTimeUs + " to " + (audioPresentationTimeUsLast + 1));
                audioEncoderOutputBufferInfo.presentationTimeUs = audioPresentationTimeUsLast + 1;
            }
            audioPresentationTimeUsLast = audioEncoderOutputBufferInfo.presentationTimeUs;

            // Write data
            if (audioEncoderOutputBufferInfo.size != 0) {
                mMuxer.writeSampleData(
                        mAudioTrackIndex, encoderOutputBuffer, audioEncoderOutputBufferInfo);
            }

            if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    != 0) {
                if (VERBOSE) Log.d(TAG, "audio encoder: EOS");
                mAudioEncoderDone = true;
            }
            mAudioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
//            audioEncodedFrameCount++;
            // We enqueued an encoded frame, let's try something else next.
//            break;
        }




//        while(!mAudioEncoderDone) {
//            int audioEncoderStatus = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
//            if (audioEncoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                // no output available yet
//                if (VERBOSE) Log.d(TAG, "no output from audio encoder available");
//                break;
//            } else if (audioEncoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                // not expected for an encoder
//                audioEncoderOutputBuffers = mAudioEncoder.getOutputBuffers();
//                if (VERBOSE) Log.d(TAG, "encoder output buffers changed");
//            } else if (audioEncoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                // not expected for an encoder
//                MediaFormat newEncoderFormat = mAudioEncoder.getOutputFormat();
//                if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newEncoderFormat);
//                // should happen before receiving buffers, and should only happen once
//                if (mAudioOutputFormatEstablished) {
//                    throw new RuntimeException("format changed twice");
//                }
//
//                // now that we have the Magic Goodies, start the muxer
//                mAudioTrackIndex = mMuxer.addTrack(newEncoderFormat);
//                mAudioOutputFormatEstablished = true;
//
//                if(mVideoOutputFormatEstablished) {
//                    mMuxer.start();
//                    mMuxerStarted = true;
//                }
//                else {
//                    //Don't start muxing until muxer is started.
//                    break;
//                }
//            } else if (audioEncoderStatus < 0) {
//                System.out.println("unexpected result from encoder.dequeueOutputBuffer: " + audioEncoderStatus);
//            } else { // encoderStatus >= 0
//                ByteBuffer encodedData = audioEncoderOutputBuffers[audioEncoderStatus];
//                if (encodedData == null) {
//                    throw new RuntimeException("encoderOutputBuffer " + audioEncoderStatus +
//                            " was null");
//                }
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    // The codec config data was pulled out and fed to the muxer when we got
//                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
//                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
//                    mBufferInfo.size = 0;
//                }
//
//                if (mBufferInfo.size != 0) {
//                    if (!mMuxerStarted) {
//                        throw new RuntimeException("muxer hasn't started");
//                    }
//
//                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
//                    encodedData.position(mBufferInfo.offset);
//                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//
//                    mMuxer.writeSampleData(mAudioTrackIndex, encodedData, mBufferInfo);
//                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
//                }
//
//                mAudioEncoder.releaseOutputBuffer(audioEncoderStatus, false);
//
//                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
////                                if (!endOfStream) {
////                                    Log.w(TAG, "reached end of stream unexpectedly");
////                                } else {
//                    if (VERBOSE) Log.d(TAG, "end of stream reached");
////                                }
//                    mAudioEncoderDone = true;      // out of while
//                }
//            }
//        }
    }

    private void videoDrawFrame() {
        if(mUseVideo && !mVideoGeneratorDone
                && (encoderOutputVideoFormat == null || mMuxerStarted)) {
            // Generate a new frame of input.
            Canvas cnv = mVideoSurface.lockCanvas(null);
            float percent = mVideoGeneratorFrame /(float)VIDEO_NUM_FRAMES;
            int x = (int)(percent*TEST_BITMAP.getWidth());
            int y = (int)(percent*TEST_BITMAP.getHeight());
            Log.d(TAG, "percent: " + percent +" of (" + TEST_BITMAP.getWidth() + ", " + TEST_BITMAP.getHeight() + ")");
            Log.d(TAG, "(" + 0 + ", " + 0 +") -> (" + x + ", " + y + ")");
            cnv.drawBitmap(TEST_BITMAP, new Rect(0, 0, x, y), mScreenRect, null);

            // Submit it to the encoder.  The eglSwapBuffers call will block if the input
            // is full, which would be bad if it stayed full until we dequeued an output
            // buffer (which we can't do, since we're stuck here).  So long as we fully drain
            // the encoder before supplying additional input, the system guarantees that we
            // can supply another frame without blocking.
            if (VERBOSE) Log.d(TAG, "sending frame " + mVideoGeneratorFrame + " to encoder");
            mVideoSurface.unlockCanvasAndPost(cnv);

            mVideoGeneratorFrame++;
            if (mVideoGeneratorFrame >= VIDEO_NUM_FRAMES) {
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
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
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

            MediaFormat encoderFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE,
                    audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
//            MediaFormat encoderFormat =
//                    MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
            encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
            //encoder input buffers are too small by default
            encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);
            try {
                mAudioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
            catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to configure audio encoder");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create audio encoder/decoder");
        }


        mAudioDecoder.start();
        mAudioEncoder.start();
    }

    private void prepareMuxer() {
        mOutputPath = new File(OUTPUT_DIR,
                "test" + (mUseAudio ? "A" : "") + (mUseVideo ? "V" : "") + "." + mWidth + "x" + mHeight + ".mp4").toString();
        Log.d(TAG, "output file is " + mOutputPath);

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            mMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
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
//        if (VERBOSE) Log.d(TAG, "videoDrainEncoder(" + endOfStream + ")");

        if (endOfStream && !mVideoEOSSent) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
            mVideoEOSSent = true;
        }

        ByteBuffer[] videoEncoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        while (mUseVideo && !mVideoEncoderDone
                && (encoderOutputVideoFormat == null || mMuxerStarted)) {
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
                if (mVideoTrackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }

                encoderOutputVideoFormat = mVideoEncoder.getOutputFormat();
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

                    float percent = mVideoEncoderFrame/(float)VIDEO_NUM_FRAMES;
                    mBufferInfo.presentationTimeUs = (long)(percent*VIDEO_LENGTH_US);
                    mVideoEncoderFrame++;

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
//                    break;      // out of while
                }
            }
        }
    }
}
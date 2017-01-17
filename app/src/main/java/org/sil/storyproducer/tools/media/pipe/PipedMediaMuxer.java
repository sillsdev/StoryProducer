package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>This media pipeline component multiplexes encoded audio and video streams into an output file.
 * This class primarily encapsulates a {@link MediaMuxer}.</p>
 * <p>Components commonly used in conjunction with this class are {@link PipedMediaCodec}
 * (particularly its subclasses {@link PipedMediaEncoder} and {@link PipedVideoSurfaceEncoder})
 * and {@link PipedMediaExtractor}.</p>
 */
public class PipedMediaMuxer implements Closeable, PipedMediaByteBufferDest {
    private static final String TAG = "PipedMediaMuxer";

    private final String mPath;
    private final int mFormat;

    private MediaMuxer mMuxer = null;

    private PipedMediaByteBufferSource mAudioSource = null;
    private int mAudioTrackIndex = -1;
    private PipedMediaByteBufferSource mVideoSource = null;
    private int mVideoTrackIndex = -1;

    /**
     * Create a muxer.
     * @param path the output media file.
     * @param format the format of the output media file
     *               (from {@link android.media.MediaMuxer.OutputFormat}).
     */
    public PipedMediaMuxer(String path, int format) {
        mPath = path;
        mFormat = format;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(src.getMediaType() == MediaHelper.MediaType.AUDIO) {
            if(mAudioSource == null) {
                mAudioSource = src;
            }
            else {
                throw new SourceUnacceptableException("audio source already provided");
            }
        }
        else if(src.getMediaType() == MediaHelper.MediaType.VIDEO) {
            if(mVideoSource == null) {
                mVideoSource = src;
            }
            else {
                throw new SourceUnacceptableException("video source already provided");
            }
        }
    }

    private void start() throws IOException, SourceUnacceptableException {
        File output = new File(mPath);
        if(!output.exists()) {
            output.createNewFile();
        }
        mMuxer = new MediaMuxer(mPath, mFormat);

        if (mAudioSource != null) {
            if(MediaHelper.VERBOSE) { Log.v(TAG, "setting up audio track."); }
            mAudioSource.setup();
            if(MediaHelper.VERBOSE) { Log.v(TAG, "adding audio track."); }
            mAudioTrackIndex = mMuxer.addTrack(mAudioSource.getOutputFormat());
        }
        if (mVideoSource != null) {
            if(MediaHelper.VERBOSE) { Log.v(TAG, "setting up video track."); }
            mVideoSource.setup();
            if(MediaHelper.VERBOSE) { Log.v(TAG, "adding video track."); }
            mVideoTrackIndex = mMuxer.addTrack(mVideoSource.getOutputFormat());
        }
        if(MediaHelper.VERBOSE) { Log.v(TAG, "starting"); }
        mMuxer.start();
    }

    /**
     * Set the muxer in motion.
     * @throws IOException
     * @throws SourceUnacceptableException
     */
    public void crunch() throws IOException, SourceUnacceptableException {
        start();

        StreamThread audioThread = null;
        StreamThread videoThread = null;
        if(mAudioSource != null) {
            audioThread = new StreamThread(mMuxer, mAudioSource, mAudioTrackIndex);
            audioThread.start();
        }

        if(mVideoSource != null) {
            videoThread = new StreamThread(mMuxer, mVideoSource, mVideoTrackIndex);
            videoThread.start();
        }

        if(audioThread != null) {
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "Audio thread did not end!", e);
            }
        }

        if(videoThread != null) {
            try {
                videoThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "Video thread did not end!", e);
            }
        }

        close();
    }

    private class StreamThread extends Thread {
        private final MediaMuxer mMuxer;
        private final PipedMediaByteBufferSource mSource;
        private final int mTrackIndex;

        public StreamThread(MediaMuxer muxer, PipedMediaByteBufferSource src, int trackIndex) {
            mMuxer = muxer;
            mSource = src;
            mTrackIndex = trackIndex;
        }

        @Override
        public void run() {
            ByteBuffer buffer;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!mSource.isDone()) {
                buffer = mSource.getBuffer(info);
                if (MediaHelper.VERBOSE) {
                    Log.v(TAG, "[track " + mTrackIndex + "] writing output buffer of size "
                            + info.size + " for time " + info.presentationTimeUs);
                }
                synchronized (mMuxer) {
                    mMuxer.writeSampleData(mTrackIndex, buffer, info);
                }
                mSource.releaseBuffer(buffer);
            }
        }
    }

    @Override
    public void close() {
        //Close sources.
        if(mAudioSource != null) {
            mAudioSource.close();
            mAudioSource = null;
        }
        if(mVideoSource != null) {
            mVideoSource.close();
            mVideoSource = null;
        }

        //Close self.
        if(mMuxer != null) {
            try {
                mMuxer.stop();
            }
            catch(IllegalStateException e) {
                Log.d(TAG, "Failed to stop MediaMuxer!", e);
            }
            finally {
                mMuxer.release();
            }
            mMuxer = null;
        }
    }
}

package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.media.MediaHelper;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>A media pipeline component for multiplexing encoded audio and video streams into an output file.
 * This class primarily encapsulates a {@link MediaMuxer}.</p>
 * <p>Components commonly used in conjunction with this class are {@link PipedMediaCodec}
 * (particularly its subclasses {@link PipedMediaEncoder} and {@link PipedVideoSurfaceEncoder})
 * and {@link PipedMediaExtractor}.</p>
 */
public class PipedMediaMuxer implements Closeable, PipedMediaByteBufferDest {
    private static final String TAG = "PipedMediaMuxer";

    private MediaMuxer mMuxer = null;

    private PipedMediaByteBufferSource mAudioSource = null;
    private int mAudioTrackIndex = -1;
    private PipedMediaByteBufferSource mVideoSource = null;
    private int mVideoTrackIndex = -1;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    /**
     * @param path the output media file.
     * @param format the format of the output media file
     *               (from {@link android.media.MediaMuxer.OutputFormat}).
     * @throws IOException if failed to open the file for write
     */
    public PipedMediaMuxer(String path, int format) throws IOException {
        mMuxer = new MediaMuxer(path, format);
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
        if (mAudioSource != null) {
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: setting up audio track."); }
            mAudioSource.setup();
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: adding audio track."); }
            mAudioTrackIndex = mMuxer.addTrack(mAudioSource.getOutputFormat());
        }
        if (mVideoSource != null) {
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: setting up video track."); }
            mVideoSource.setup();
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: adding video track."); }
            mVideoTrackIndex = mMuxer.addTrack(mVideoSource.getOutputFormat());
        }
        if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: starting"); }
        mMuxer.start();
    }

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
                //TODO: handle exception
                e.printStackTrace();
            }
        }

        if(videoThread != null) {
            try {
                videoThread.join();
            } catch (InterruptedException e) {
                //TODO: handle exception
                e.printStackTrace();
            }
        }

        //TODO: close sources
    }

    private class StreamThread extends Thread {
        private MediaMuxer mMuxer;
        private PipedMediaByteBufferSource mSource;
        private int mTrackIndex;

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
                    Log.d(TAG, "muxer: writing output buffer of size " + info.size + " for time " + info.presentationTimeUs);
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
        //TODO: close sources
        if(mMuxer != null) {
            try {
                mMuxer.stop();
            }
            finally {
                mMuxer.release();
            }
        }
    }
}

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
 * (particularly its subclasses {@link PipedMediaEncoderBuffer} and {@link PipedMediaEncoderSurface})
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
            mAudioTrackIndex = mMuxer.addTrack(mAudioSource.getFormat());
        }
        if (mVideoSource != null) {
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: setting up video track."); }
            mVideoSource.setup();
            if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: adding video track."); }
            mVideoTrackIndex = mMuxer.addTrack(mVideoSource.getFormat());
        }
        if(MediaHelper.VERBOSE) { Log.d(TAG, "muxer: starting"); }
        mMuxer.start();
    }

    public void crunch() throws IOException, SourceUnacceptableException {
        start();

        while ((mAudioSource != null && !mAudioSource.isDone()) || (mVideoSource != null && !mVideoSource.isDone())) {
            ByteBuffer buffer;
            if(mAudioSource != null && !mAudioSource.isDone()) {
                buffer = mAudioSource.getBuffer(mInfo);
                if (MediaHelper.VERBOSE) {
                    Log.d(TAG, "muxer: writing audio output buffer of size " + mInfo.size + " for time " + mInfo.presentationTimeUs);
                }
                mMuxer.writeSampleData(mAudioTrackIndex, buffer, mInfo);
                mAudioSource.releaseBuffer(buffer);
            }

            if(mVideoSource != null && !mVideoSource.isDone()) {
                buffer = mVideoSource.getBuffer(mInfo);
                if (MediaHelper.VERBOSE) {
                    Log.d(TAG, "muxer: writing video output buffer of size " + mInfo.size + " for time " + mInfo.presentationTimeUs);
                }
                mMuxer.writeSampleData(mVideoTrackIndex, buffer, mInfo);
                mVideoSource.releaseBuffer(buffer);
            }
        }

    }

    @Override
    public void close() {
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

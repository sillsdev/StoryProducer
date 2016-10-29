package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PipedMediaMuxer implements Closeable, MediaByteBufferDest {
    private static final String TAG = "PipedMediaMuxer";

    private MediaMuxer mMuxer = null;

    private MediaByteBufferSource[] mSources = {null, null};

    private MediaByteBufferSource mAudioSource = null;
    private int mAudioTrackIndex = -1;
    private MediaByteBufferSource mVideoSource = null;
    private int mVideoTrackIndex = -1;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public PipedMediaMuxer(String path, int format) throws IOException {
        mMuxer = new MediaMuxer(path, format);
    }

    @Override
    public void addSource(MediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSources[0] == null) {
            mSources[0] = src;
        }
        else if(mSources[1] == null) {
            mSources[1] = src;
        }
        else {
            throw new SourceUnacceptableException("two sources already provided");
        }
    }

    private void actuallyAddSource(MediaByteBufferSource src) throws SourceUnacceptableException {
        //TODO: defer calling getType
        if(src.getType() == MediaHelper.MediaType.AUDIO) {
            if(mAudioSource == null) {
                mAudioSource = src;
            }
            else {
                throw new SourceUnacceptableException("audio source already provided");
            }
        }
        else if(src.getType() == MediaHelper.MediaType.VIDEO) {
            if(mVideoSource == null) {
                mVideoSource = src;
            }
            else {
                throw new SourceUnacceptableException("video source already provided");
            }
        }
    }

    private void start() throws IOException, SourceUnacceptableException {
        for(int i = 0; i < mSources.length; i++) {
            if(mSources == null) {
                break;
            }
            actuallyAddSource(mSources[i]);
        }

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

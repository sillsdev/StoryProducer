package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public abstract class PipedMediaCodecBuffer extends PipedMediaCodec implements MediaByteBufferDest {
    private static final String TAG = "PipedMediaCodecBuffer";

//    protected MediaFormat mSourceFormat;
    protected MediaByteBufferSource mSource;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

//    @Override
//    public void setup() {
//        mSource.setup();
//    }

//    @Override
//    public MediaFormat getFormat() {
//        if(mSource == null) {
//            throw new RuntimeException("No source specified for encoder!");
//        }
//
//        mSourceFormat = mSource.getFormat();
//
//        return super.getFormat();
//    }

    @Override
    protected void spinInput() {
        if(mSource == null) {
            throw new RuntimeException("No source specified for encoder!");
        }

        int pollCode = mCodec.dequeueInputBuffer(MediaHelper.TIMEOUT_USEC);
        if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
            //TODO: Can this ever happen?
            if (MediaHelper.VERBOSE) Log.d(TAG, getComponentName() + ": no input buffer");
        }
        else {
            if (MediaHelper.VERBOSE) {
                Log.d(TAG, getComponentName() + ": returned input buffer: " + pollCode);
            }
            ByteBuffer inputBuffer = mInputBuffers[pollCode];
            mSource.fillBuffer(inputBuffer, mInfo);
            mCodec.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags);
        }
    }

    @Override
    public void addSource(MediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("One source already supplied!");
        }
        mSource = src;
    }
}

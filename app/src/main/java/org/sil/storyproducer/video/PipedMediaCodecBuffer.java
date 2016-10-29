package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class PipedMediaCodecBuffer extends PipedMediaCodec implements MediaByteBufferDest {
    private static final String TAG = "PipedMediaCodecBuffer";

    private MediaByteBufferSource mSource = null;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public PipedMediaCodecBuffer(MediaFormat format) {
        super(format);
    }

    @Override
    protected void spinInput() {
        if(mSource == null) {
            throw new RuntimeException("No source specified for encoder!");
        }
        //TODO: What is the loop condition?
//        while (true) {
            int pollCode = mCodec.dequeueInputBuffer(MediaHelper.TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //TODO: Can this ever happen?
                if (VERBOSE) Log.d(TAG, getComponentName() + ": no input buffer");
//                break;
            }
            else {
                if (VERBOSE) {
                    Log.d(TAG, getComponentName() + ": returned input buffer: " + pollCode);
                }
                ByteBuffer inputBuffer = mInputBuffers[pollCode];
                mSource.fillBuffer(inputBuffer, mInfo);
                mCodec.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags);
                // We enqueued a pending frame, let's try something else next.
//                break;
            }
//        }
    }

    @Override
    public void addSource(MediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("One source already supplied!");
        }
        mSource = src;
    }
}

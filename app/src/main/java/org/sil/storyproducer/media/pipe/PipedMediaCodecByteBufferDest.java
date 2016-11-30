package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.media.MediaHelper;

import java.nio.ByteBuffer;

public abstract class PipedMediaCodecByteBufferDest extends PipedMediaCodec implements PipedMediaByteBufferDest {
    private static final String TAG = "PipedMediaCodecBBDest";

    protected PipedMediaByteBufferSource mSource;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    protected void spinInput() {
        if(mSource == null) {
            throw new RuntimeException("No source specified for encoder!");
        }

        while(mComponentState != State.CLOSED && !mSource.isDone()) {
            int pollCode = mCodec.dequeueInputBuffer(MediaHelper.TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //Do nothing.
            } else {
                if (MediaHelper.VERBOSE) {
                    Log.d(TAG, getComponentName() + ": returned input buffer: " + pollCode);
                }
                ByteBuffer inputBuffer = mInputBuffers[pollCode];
                mSource.fillBuffer(inputBuffer, mInfo);
                mCodec.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags);
            }
        }

        mSource.close();
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("One source already supplied!");
        }
        mSource = src;
    }
}

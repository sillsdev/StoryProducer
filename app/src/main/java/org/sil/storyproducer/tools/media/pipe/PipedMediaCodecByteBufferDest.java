package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.nio.ByteBuffer;

/**
 * <p>This abstract media pipeline component provides a base for most encoders/decoders which work with
 * {@link ByteBuffer} input.</p>
 */
public abstract class PipedMediaCodecByteBufferDest extends PipedMediaCodec implements PipedMediaByteBufferDest {
    private static final String TAG = "PipedMediaCodecBBDest";

    protected PipedMediaByteBufferSource mSource;
    private final MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("One source already supplied!");
        }
        mSource = src;
    }

    @Override
    protected void spinInput() {
        if(mSource == null) {
            throw new RuntimeException("No source specified for encoder!");
        }

        if(MediaHelper.VERBOSE) Log.v(TAG, getComponentName() + ".spinInput starting...");

        while(mComponentState != State.CLOSED && !mSource.isDone()) {
            int pollCode = mCodec.dequeueInputBuffer(MediaHelper.TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (MediaHelper.VERBOSE) Log.v(TAG, getComponentName() + ".spinInput: no input buffer");
                //Do nothing.
            } else {
                if (MediaHelper.VERBOSE) Log.v(TAG, getComponentName() + ".spinInput: returned input buffer: " + pollCode);

                long durationNs;
                if(MediaHelper.DEBUG) {
                    durationNs = -System.nanoTime();
                }

                ByteBuffer inputBuffer = mInputBuffers[pollCode];
                mSource.fillBuffer(inputBuffer, mInfo);
                mCodec.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags);

                if (MediaHelper.DEBUG) {
                    durationNs += System.nanoTime();
                    double sec = durationNs / 1E9;
                    Log.d(TAG, getComponentName() + ".spinInput: fill/queue input buffer after "
                            + MediaHelper.getDecimal(sec) + " seconds: " + pollCode
                            + " of size " + mInfo.size + " for time " + mInfo.presentationTimeUs);
                }
            }
        }

        if(MediaHelper.VERBOSE) Log.v(TAG, getComponentName() + ".spinInput complete!");

        mSource.close();
    }
}

package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.Closeable;
import java.nio.ByteBuffer;

public abstract class PipedMediaCodec implements Closeable, MediaByteBufferSource {
    protected static final boolean VERBOSE = true;
    private static final String TAG = "PipedMediaCodec";

    @Deprecated
    protected abstract String getComponentName();

    private MediaFormat mConfigureFormat;

    protected MediaCodec mCodec;
    protected ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private MediaFormat mOutputFormat = null;

    private boolean mIsDone = false;
    private long mPresentationTimeUsLast = 0;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public PipedMediaCodec(MediaFormat format) {
        mConfigureFormat = format;
    }

    @Override
    public MediaFormat getFormat() {
        if(mOutputFormat == null) {
            spinOutput(mInfo, true);
            if(mOutputFormat == null) {
                throw new RuntimeException("format was not retrieved from loop");
            }
        }
        return mOutputFormat;
    }

    @Override
    public MediaHelper.MediaType getType() {
        return MediaHelper.getTypeFromFormat(mConfigureFormat);
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        ByteBuffer outputBuffer = spinOutput(info, false);
        buffer.clear();
        buffer.put(outputBuffer);
        releaseBuffer(outputBuffer);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        return spinOutput(info, false);
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        for(int i = 0; i < mOutputBuffers.length; i++) {
            if(mOutputBuffers[i] == buffer) {
                mCodec.releaseOutputBuffer(i, false);
                return;
            }
        }
        throw new InvalidBufferException("I don't own that buffer!");
    }

    protected void start() {
        mCodec.start();
        mInputBuffers = mCodec.getInputBuffers();
        mOutputBuffers = mCodec.getOutputBuffers();
    }

    private ByteBuffer spinOutput(MediaCodec.BufferInfo info, boolean stopWithFormat) {
        if(mIsDone) {
            throw new RuntimeException("spinOutput called after depleted");
        }

        while (!mIsDone) {
            int pollCode = mCodec.dequeueOutputBuffer(
                    info, MediaHelper.TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": no output buffer");

                spinInput();
            }
            else if (pollCode == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": output buffers changed");
                mOutputBuffers = mCodec.getOutputBuffers();
            }
            else if (pollCode == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": output format changed");
                if (mOutputFormat != null) {
                    throw new RuntimeException("changed output format again?");
                }
                mOutputFormat = mCodec.getOutputFormat();
                if(stopWithFormat) {
                    return null;
                }
            }
            else if((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                if (VERBOSE) Log.d(TAG, getComponentName() + ": codec config buffer");
                //TODO: make sure this is ok
                // Simply ignore codec config buffers.
                mCodec.releaseOutputBuffer(pollCode, false);
            }
            else {
                if (VERBOSE) {
                    Log.d(TAG, getComponentName() + ": returned output buffer: " + pollCode + " of size " + info.size + " for time " + info.presentationTimeUs);
                }

                ByteBuffer buffer = mOutputBuffers[pollCode];

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, getComponentName() + ": EOS");
                    mIsDone = true;
                }
                else {
                    correctTime(info);
                    buffer.position(info.offset);
                    buffer.limit(info.offset + info.size);
                }
                return buffer;
            }
        }

        return null;
    }

    protected void correctTime(MediaCodec.BufferInfo info) {
        if (mPresentationTimeUsLast > info.presentationTimeUs) {
            throw new RuntimeException("buffer presentation time out of order!");
//                    info.presentationTimeUs = mPresentationTimeUsLast + 1;
        }
        mPresentationTimeUsLast = info.presentationTimeUs;
    }

    protected abstract void spinInput();

    @Override
    public void close() {
        if(mCodec != null) {
            try {
                mCodec.stop();
            }
            finally {
                mCodec.release();
            }
        }
    }
}

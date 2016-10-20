package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public abstract class PipedMediaCodec implements MediaByteBufferSource, MediaByteBufferDest {
    private static final boolean VERBOSE = true;
    private static final String TAG = "PipedMediaCodec";
    protected abstract String getComponentName();
    private static final long TIMEOUT_USEC = 1000;

    protected MediaCodec mCodec;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private MediaFormat mOutputFormat = null;
    private int mCurrentOutputBufferIndex = -1;

    private MediaByteBufferSource mSource = null;

    private boolean mIsDone = false;
    private long mPresentationTimeUsLast = 0;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public PipedMediaCodec() {
        //do nothing
    }

    @Override
    public MediaFormat getFormat() {
        if(mOutputFormat == null) {
            spinOutput(mInfo);
            if(mOutputFormat == null) {
                throw new RuntimeException("format was not retrieved from loop");
            }
        }
        return mOutputFormat;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        spinOutput(info);
        buffer.clear();
        buffer.put(mOutputBuffers[mCurrentOutputBufferIndex]);
        mCodec.releaseOutputBuffer(mCurrentOutputBufferIndex, false);
        mCurrentOutputBufferIndex = -1;
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        spinOutput(info);
        int index = mCurrentOutputBufferIndex;
        mCurrentOutputBufferIndex = -1;
        return mOutputBuffers[index];
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

    private void spinOutput(MediaCodec.BufferInfo info) {
        // Poll frames from the audio encoder and send them to the muxer.
        while (!mIsDone) {
            int pollCode = mCodec.dequeueOutputBuffer(
                    info, TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": no output buffer");
                spinInput();
//                break;
            }
            else if (pollCode == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": output buffers changed");
                mOutputBuffers = mCodec.getOutputBuffers();
//                break;
            }
            else if (pollCode == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (VERBOSE) Log.d(TAG, getComponentName() + ": output format changed");
                if (mOutputFormat != null) {
                    throw new RuntimeException("changed output format again?");
                }
                mOutputFormat = mCodec.getOutputFormat();
                break;
            }
            else if((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                if (VERBOSE) Log.d(TAG, getComponentName() + ": codec config buffer");
                //TODO: make sure this is ok
                // Simply ignore codec config buffers.
                mCodec.releaseOutputBuffer(pollCode, false);
//                break;
            }
            else {
                //TODO: make sure this never happens
                if(mCurrentOutputBufferIndex != -1) {
                    throw new RuntimeException("attempting to get second output buffer before previous buffer handled");
                }
                if (VERBOSE) {
                    Log.d(TAG, getComponentName() + ": returned output buffer: " + pollCode + " of size " + info.size + " for time " + info.presentationTimeUs);
                }

                mCurrentOutputBufferIndex = pollCode;

                if (mPresentationTimeUsLast > info.presentationTimeUs) {
                    throw new RuntimeException("buffer presentation time out of order!");
//                    info.presentationTimeUs = mPresentationTimeUsLast + 1;
                }
                mPresentationTimeUsLast = info.presentationTimeUs;

                ByteBuffer buffer = mOutputBuffers[pollCode];
                buffer.position(info.offset);
                buffer.limit(info.offset + info.size);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, getComponentName() + ": EOS");
                    mIsDone = true;
                }
                break;
            }
        }
    }

    private void spinInput() {
        //TODO: prevent this earlier
        if(mSource == null) {
            throw new RuntimeException("No source specified for encoder!");
        }
        //TODO: What is the loop condition?
        while (true) {
            int pollCode = mCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //TODO: Can this ever happen?
                if (VERBOSE) Log.d(TAG, getComponentName() + ": no input buffer");
                break;
            }
            else {
                if (VERBOSE) {
                    Log.d(TAG, getComponentName() + ": returned input buffer: " + pollCode);
                }
                ByteBuffer inputBuffer = mInputBuffers[pollCode];
                mSource.fillBuffer(inputBuffer, mInfo);
                mCodec.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags);
                // We enqueued a pending frame, let's try something else next.
                break;
            }
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

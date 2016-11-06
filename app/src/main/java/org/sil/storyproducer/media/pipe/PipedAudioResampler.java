package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import org.sil.storyproducer.media.ByteBufferPool;
import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class PipedAudioResampler implements PipedMediaByteBufferSource, PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioResampler";

    private PipedMediaByteBufferSource mSource;
    private MediaFormat mFormat;

    private ByteBufferPool mBufferPool = new ByteBufferPool();

    private static final float PERCENT_BUFFER_FILL = .75f;

    private int mChannelCount;

    private short[] mLeftSamples;
    private short[] mRightSamples;

    private ByteBuffer mInputBuffer;
    private ShortBuffer mInputShortBuffer;
    private MediaCodec.BufferInfo mInputInfo = new MediaCodec.BufferInfo();

    private boolean mIsDone = false;
    private long mSeekTime = 0;
    private long mLeftSeekTime = 0;
    private long mRightSeekTime = 0;
    private int mAbsoluteSampleIndex = 0;
    private int mAbsoluteRightSampleIndex = -1;
    private int mSampleRate;
    private int mInputSampleRate;

    private float mInputUsPerSample;
//    private float mUsPerSample;

    private long getTimeFromIndex(long sampleRate, int index) {
        return index * 1000000L / sampleRate;
    }

    public PipedAudioResampler(int sampleRate) {
        mSampleRate = sampleRate;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("audio source already added!");
        }
        mSource = src;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.AUDIO;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        spinOutput(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        ByteBuffer buffer = mBufferPool.get();
        spinOutput(buffer, info);
        return buffer;
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        mBufferPool.release(buffer);
    }

    @Override
    public void setup() throws IOException {
        mSource.setup();
        mFormat = mSource.getFormat();

        //Get input sample rate
        mInputSampleRate = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mInputUsPerSample = 1000000f/mInputSampleRate;

        //Set new sample rate
        mFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);

        mChannelCount = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mLeftSamples = new short[mChannelCount];
        mRightSamples = new short[mChannelCount];
        for(int i = 0; i < mChannelCount; i++) {
            mLeftSamples[i] = 0;
            mRightSamples[i] = 0;
        }

        //get the first input buffer
        fetchInputBuffer();
    }

    @Override
    public MediaFormat getFormat() {
        return mFormat;
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    private void spinOutput(ByteBuffer outBuffer, MediaCodec.BufferInfo info) {
        outBuffer.clear();

        info.size = 0;
        info.offset = 0;
        info.presentationTimeUs = mSeekTime;
        info.flags = 0;

        ShortBuffer outShortBuffer = outBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
        outShortBuffer.clear();

        while(!mIsDone) {
            for(int i = 0; i < mChannelCount; i++) {
                short sample = getSampleForTime(mSeekTime, i);
                outShortBuffer.put(sample);

                //increment by short size (2 bytes)
                info.size += 2;
            }

            mAbsoluteSampleIndex++;
            mSeekTime = getTimeFromIndex(mSampleRate, mAbsoluteSampleIndex);

            if(info.size > PERCENT_BUFFER_FILL * outBuffer.capacity()) {
                break;
            }
        }
        outBuffer.position(info.offset);
        outBuffer.limit(info.offset + info.size);
        if(mIsDone) {
            info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        }
        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "resampler: returned output buffer: size " + info.size + " for time " + info.presentationTimeUs);
        }

    }

    private short getSampleForTime(long time, int channel) {
        while(time >= mRightSeekTime) {
            fetchNewLeftAndRight();
        }

        short left = mLeftSamples[channel];
        short right = mRightSamples[channel];

        if(time == mLeftSeekTime) {
            return left;
        }
        else {
            float rightWeight = (time - mLeftSeekTime) / mInputUsPerSample;
            float leftWeight = 1 - rightWeight;

            return (short) ((short) (leftWeight * left) + (short) (rightWeight * right));
        }
    }

    private void releaseInputBuffer() {
        mSource.releaseBuffer(mInputBuffer);
        mInputBuffer = null;
        mInputShortBuffer = null;
    }

    private void fetchNewLeftAndRight() {
        short[] temp = mLeftSamples;
        mLeftSamples = mRightSamples;
        mRightSamples = temp;

        mLeftSeekTime = mRightSeekTime;

        mAbsoluteRightSampleIndex++;
        mRightSeekTime = getTimeFromIndex(mInputSampleRate, mAbsoluteRightSampleIndex);

        while(mInputShortBuffer != null && mInputShortBuffer.remaining() <= 0) {
            releaseInputBuffer();
            fetchInputBuffer();
        }
        //If we hit the end of input, use 0 as the last right sample value.
        if(mInputShortBuffer == null) {
           mIsDone = true;

            for(int i = 0; i < mChannelCount; i++) {
                mRightSamples[i] = 0;
            }
        }
        else {
            for (int i = 0; i < mChannelCount; i++) {
                mRightSamples[i] = mInputShortBuffer.get();
            }
        }
    }

    private void fetchInputBuffer() {
        if(mSource.isDone()) {
            return;
        }

        //pull in new buffer
        mInputBuffer = mSource.getBuffer(mInputInfo);
        mInputShortBuffer = mInputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
    }
}

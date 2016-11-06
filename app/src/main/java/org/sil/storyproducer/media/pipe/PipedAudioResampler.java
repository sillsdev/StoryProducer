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

/**
 * This media pipeline component resamples (converts sample rate of) raw audio using linear interpolation.
 */
public class PipedAudioResampler implements PipedMediaByteBufferSource, PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioResampler";

    private PipedMediaByteBufferSource mSource;
    private MediaFormat mSourceFormat;
    private MediaFormat mOutputFormat;

    private ByteBufferPool mBufferPool = new ByteBufferPool();

    //Only fill output buffer this much. This prevents buffer overflow.
    private static final float PERCENT_BUFFER_FILL = .75f;

    private int mInputSampleRate;
    private float mInputUsPerSample;

    //variables for our sliding window of source data
    private short[] mLeftSamples;
    private short[] mRightSamples;
    private long mLeftSeekTime = 0;
    private long mRightSeekTime = 0;
    //N.B. Starting at -1 ensures starting with right and left from source.
    private int mAbsoluteRightSampleIndex = -1;

    private ByteBuffer mInputBuffer;
    private ShortBuffer mInputShortBuffer;
    private MediaCodec.BufferInfo mInputInfo = new MediaCodec.BufferInfo();

    private int mSampleRate;
    private int mChannelCount;
    private long mSeekTime = 0;
    private int mAbsoluteSampleIndex = 0;

    private boolean mIsDone = false;

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

        mSourceFormat = mSource.getOutputFormat();
        mInputSampleRate = mSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mInputUsPerSample = 1000000f / mInputSampleRate; //1000000 us/s
        mChannelCount = mSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mOutputFormat = MediaHelper.createFormat("audio/raw");
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mInputSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        //Initialize sample data to 0.
        mLeftSamples = new short[mChannelCount];
        mRightSamples = new short[mChannelCount];
        for(int i = 0; i < mChannelCount; i++) {
            mLeftSamples[i] = 0;
            mRightSamples[i] = 0;
        }

        //Get the first input buffer.
        fetchInputBuffer();
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    private void spinOutput(ByteBuffer outBuffer, MediaCodec.BufferInfo info) {
        //reset output buffer
        outBuffer.clear();

        //reset output buffer info
        info.size = 0;
        info.offset = 0;
        //N.B. mSeekTime is currently the time of the first sample in this buffer.
        info.presentationTimeUs = mSeekTime;
        info.flags = 0;

        //prepare a ShortBuffer view of the output buffer
        ShortBuffer outShortBuffer = outBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
        outShortBuffer.clear();

        while(!mIsDone) {
            //interleave channels
            //N.B. Always put all samples (of different channels) of the same time in the same buffer.
            for(int i = 0; i < mChannelCount; i++) {
                short sample = getSampleForTime(mSeekTime, i);
                outShortBuffer.put(sample);

                //increment by short size (2 bytes)
                info.size += 2;
            }

            //Keep track of the current presentation time in the output audio stream.
            mAbsoluteSampleIndex++;
            mSeekTime = getTimeFromIndex(mSampleRate, mAbsoluteSampleIndex);

            //Don't overflow the buffer!
            if(info.size > PERCENT_BUFFER_FILL * outBuffer.capacity()) {
                break;
            }
        }

        //just to be sure
        outBuffer.position(info.offset);
        outBuffer.limit(info.offset + info.size);

        if(mIsDone) {
            info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        }

        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "resampler: returned output buffer: size " + info.size + " for time " + info.presentationTimeUs);
        }
    }

    /**
     * <p>Get the sample time from the sample index given a sample rate.</p>
     *
     * <p>Note: This method provides more accurate timestamps than simply keeping track
     * of the current timestamp and incrementing it by the time per sample.</p>
     * @param sampleRate
     * @param index
     * @return
     */
    private long getTimeFromIndex(long sampleRate, int index) {
        return index * 1000000L / sampleRate;
    }

    /**
     * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
     *
     * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
     * @param time
     * @param channel
     * @return
     */
    private short getSampleForTime(long time, int channel) {
        //Move window forward until left and right appropriately surround the given time.
        //N.B. Left is inclusive; right is exclusive.
        while(time >= mRightSeekTime) {
            advanceWindow();
        }

        short left = mLeftSamples[channel];
        short right = mRightSamples[channel];

        if(time == mLeftSeekTime) {
            return left;
        }
        else {
            //Perform linear interpolation.
            float rightWeight = (time - mLeftSeekTime) / mInputUsPerSample;
            float leftWeight = 1 - rightWeight;

            return (short) ((short) (leftWeight * left) + (short) (rightWeight * right));
        }
    }

    /**
     * Slide the window forward by one sample.
     */
    private void advanceWindow() {
        //Set left's values to be right's current values.
        short[] temp = mLeftSamples;
        mLeftSamples = mRightSamples;
        mRightSamples = temp;

        mLeftSeekTime = mRightSeekTime;

        //Update right's time.
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
            //Get right's values from the input buffer.
            for (int i = 0; i < mChannelCount; i++) {
                mRightSamples[i] = mInputShortBuffer.get();
            }
        }
    }

    private void releaseInputBuffer() {
        mSource.releaseBuffer(mInputBuffer);
        mInputBuffer = null;
        mInputShortBuffer = null;
    }

    private void fetchInputBuffer() {
        //If our source has no more output, leave the buffers as null (assumed from releaseInputBuffer).
        if(mSource.isDone()) {
            return;
        }

        //Pull in new buffer.
        mInputBuffer = mSource.getBuffer(mInputInfo);
        mInputShortBuffer = mInputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();
    }
}

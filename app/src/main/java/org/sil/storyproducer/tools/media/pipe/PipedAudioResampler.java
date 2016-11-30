package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * <p>This media pipeline component resamples (converts sample rate of) raw audio using linear interpolation.</p>
 * <p>This component also optionally changes the channel count and/or volume of the raw audio stream.</p>
 */
public class PipedAudioResampler extends PipedAudioShortManipulator implements PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioResampler";

    private float mVolumeModifier = 1f;

    private PipedMediaByteBufferSource mSource;
    private MediaFormat mSourceFormat;
    private MediaFormat mOutputFormat;

    private int mSourceSampleRate;
    private float mSourceUsPerSample;
    private int mSourceChannelCount;

    //variables for our sliding window of source data
    private short[] mLeftSamples;
    private short[] mRightSamples;
    private long mLeftSeekTime = 0;
    private long mRightSeekTime = 0;
    //N.B. Starting at -1 ensures starting with right and left from source.
    private int mAbsoluteRightSampleIndex = -1;

    private ByteBuffer mSourceBuffer;
    private ShortBuffer mSourceShortBuffer;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private boolean mIsDone = false;

    /**
     * Create resampler, maintaining channel count from source audio stream.
     * @param sampleRate sample rate of the new, resampled audio stream.
     */
    public PipedAudioResampler(int sampleRate) {
        this(sampleRate, 0);
    }

    /**
     * Create resampler changing channel count from the source channel count to the specified channel count.
     * @param sampleRate sample rate of the new, resampled audio stream.
     * @param channelCount number of channels in the new, resampled audio stream.
     */
    public PipedAudioResampler(int sampleRate, int channelCount) {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
    }

    /**
     * Modify all samples by multiplying applying a constant (multiplication).
     * @param volumeModifier constant to multiply all samples by.
     */
    public void setVolumeModifier(float volumeModifier) {
        mVolumeModifier = volumeModifier;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("Audio source already added!");
        }
        mSource = src;
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mSource == null) {
            throw new SourceUnacceptableException("Source cannot be null!");
        }

        mSource.setup();

        mSourceFormat = mSource.getOutputFormat();
        mSourceSampleRate = mSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mSourceUsPerSample = 1000000f / mSourceSampleRate; //1000000 us/s
        mSourceChannelCount = mSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        if(mSourceChannelCount != 1 && mSourceChannelCount != 2) {
            throw new SourceUnacceptableException("Source audio is neither mono nor stereo!");
        }

        if(mChannelCount == 0) {
            mChannelCount = mSourceChannelCount;
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        //Initialize sample data to 0.
        mLeftSamples = new short[mSourceChannelCount];
        mRightSamples = new short[mSourceChannelCount];
        for(int i = 0; i < mSourceChannelCount; i++) {
            mLeftSamples[i] = 0;
            mRightSamples[i] = 0;
        }

        //Get the first input buffer.
        fetchSourceBuffer();
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    /**
     * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
     *
     * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
     * @param time
     * @param channel
     * @return
     */
    @Override
    protected short getSampleForTime(long time, int channel) {
        //Move window forward until left and right appropriately surround the given time.
        //N.B. Left is inclusive; right is exclusive.
        while(time >= mRightSeekTime) {
            advanceWindow();
        }

        short left, right;

        if(mChannelCount == mSourceChannelCount) {
            left = mLeftSamples[channel];
            right = mRightSamples[channel];
        }
        else if(mChannelCount == 1/* && mSourceChannelCount == 2*/) {
            left = (short) (mLeftSamples[0]/2 + mLeftSamples[1]/2);
            right = (short) (mRightSamples[0]/2 + mRightSamples[1]/2);
        }
        else { //mChannelCount == 2 && mSourceChannelCount == 1
            left = mLeftSamples[0];
            right = mRightSamples[0];
        }

        if(time == mLeftSeekTime) {
            return left;
        }
        else {
            //Perform linear interpolation.
            float rightWeight = (time - mLeftSeekTime) / mSourceUsPerSample;
            float leftWeight = 1 - rightWeight;

            short interpolatedSample = (short) ((short) (leftWeight * left) + (short) (rightWeight * right));

            return (short) (mVolumeModifier * interpolatedSample);
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
        mRightSeekTime = getTimeFromIndex(mSourceSampleRate, mAbsoluteRightSampleIndex);

        while(mSourceShortBuffer != null && mSourceShortBuffer.remaining() <= 0) {
            releaseSourceBuffer();
            fetchSourceBuffer();
        }
        //If we hit the end of input, use 0 as the last right sample value.
        if(mSourceShortBuffer == null) {
            mIsDone = true;

            for(int i = 0; i < mSourceChannelCount; i++) {
                mRightSamples[i] = 0;
            }
        }
        else {
            //Get right's values from the input buffer.
            for (int i = 0; i < mSourceChannelCount; i++) {
                mRightSamples[i] = mSourceShortBuffer.get();
            }
        }
    }

    private void releaseSourceBuffer() {
        mSource.releaseBuffer(mSourceBuffer);
        mSourceBuffer = null;
        mSourceShortBuffer = null;
    }

    private void fetchSourceBuffer() {
        //If our source has no more output, leave the buffers as null (assumed from releaseSourceBuffer).
        if(mSource.isDone()) {
            return;
        }

        //Pull in new buffer.
        mSourceBuffer = mSource.getBuffer(mInfo);
        mSourceShortBuffer = MediaHelper.getShortBuffer(mSourceBuffer);
    }

    @Override
    public void close() {
        if(mSource != null) {
            mSource.close();
        }
    }
}

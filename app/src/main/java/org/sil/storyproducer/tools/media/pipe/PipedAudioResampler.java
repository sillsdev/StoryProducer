package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

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
    @Override
    protected String getComponentName() {
        return TAG;
    }

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

    private static final int SOURCE_BUFFER_CAPACITY = MediaHelper.MAX_INPUT_BUFFER_SIZE;

    private final short[] mSourceBufferA = new short[SOURCE_BUFFER_CAPACITY / 2];
    private boolean mHasBuffer = false;

    private long mSeekTime = 0;

    private int mSourcePos;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private int mSourceSize;

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
     * <p>Gets a {@link PipedMediaByteBufferSource} with the correct sampling.</p>
     * <p>This method works by checking the output format of an already setup source
     * against the desired sampling parameters. If the source already matches, it is merely
     * returned. Otherwise, a resampler is inserted into the pipeline.</p>
     */
    public static PipedMediaByteBufferSource correctSampling(PipedMediaByteBufferSource src, int sampleRate, int channelCount)
            throws IOException, SourceUnacceptableException {
        MediaFormat format = src.getOutputFormat();

        boolean isSamplingCorrect = (sampleRate == 0 || format.getInteger(MediaFormat.KEY_SAMPLE_RATE) == sampleRate)
                && (channelCount == 0 || format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == channelCount);

        if(!isSamplingCorrect) {
            PipedAudioResampler resampler = new PipedAudioResampler(sampleRate, channelCount);
            resampler.addSource(src);
            return resampler;
        }
        else {
            return src;
        }
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
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
        if(mComponentState != State.UNINITIALIZED) {
            return;
        }

        if(mSource == null) {
            throw new SourceUnacceptableException("Source cannot be null!");
        }

        mSource.setup();

        validateSource(mSource, 0, 0);

        mSourceFormat = mSource.getOutputFormat();
        mSourceSampleRate = mSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mSourceUsPerSample = 1000000f / mSourceSampleRate; //1000000 us/s
        mSourceChannelCount = mSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

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
        try {
            fetchSourceBuffer();
        } catch (SourceClosedException e) {
            //This case should not happen.
            throw new SourceUnacceptableException("First fetchSourceBuffer failed! Strange", e);
        }

        mComponentState = State.SETUP;

        start();
    }

    /**
     * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
     *
     * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
     * @param channel which channel to get sample for current time
     * @return sample for current time and channel
     */
    @Override
    protected short getSampleForChannel(int channel) {
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

        if(mSeekTime == mLeftSeekTime) {
            return left;
        }
        else {
            //Perform linear interpolation.
            float rightWeight = (mSeekTime - mLeftSeekTime) / mSourceUsPerSample;
            float leftWeight = 1 - rightWeight;

            short interpolatedSample = (short) ((short) (leftWeight * left) + (short) (rightWeight * right));

            return (short) (mVolumeModifier * interpolatedSample);
        }
    }

    @Override
    protected boolean loadSamplesForTime(long time) throws SourceClosedException {
        boolean moreInput = true;

        mSeekTime = time;

        //Move window forward until left and right appropriately surround the given time.
        //N.B. Left is inclusive; right is exclusive.
        while(mSeekTime >= mRightSeekTime) {
            moreInput = advanceWindow();
        }

        return moreInput;
    }

    /**
     * Slide the window forward by one sample.
     */
    private boolean advanceWindow() throws SourceClosedException {
        boolean isDone = false;

        //Set left's values to be right's current values.
        short[] temp = mLeftSamples;
        mLeftSamples = mRightSamples;
        mRightSamples = temp;

        mLeftSeekTime = mRightSeekTime;

        //Update right's time.
        mAbsoluteRightSampleIndex++;
        mRightSeekTime = getTimeFromIndex(mSourceSampleRate, mAbsoluteRightSampleIndex);

        while(mHasBuffer && mSourcePos >= mSourceSize) {
            fetchSourceBuffer();
        }
        //If we hit the end of input, use 0 as the last right sample value.
        if(!mHasBuffer) {
            isDone = true;

            mSource.close();
            mSource = null;

            for(int i = 0; i < mSourceChannelCount; i++) {
                mRightSamples[i] = 0;
            }
        }
        else {
            //Get right's values from the input buffer.
            for (int i = 0; i < mSourceChannelCount; i++) {
                try {
                    mRightSamples[i] = mSourceBufferA[mSourcePos++];
                }
                catch(ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "Tried to read beyond buffer", e);
                }
            }
        }

        return !isDone;
    }

    private void fetchSourceBuffer() throws SourceClosedException {
        mHasBuffer = false;

        if(mSource.isDone()) {
            return;
        }

        //buffer of bytes
        ByteBuffer tempSourceBuffer = mSource.getBuffer(mInfo);

        if(MediaHelper.VERBOSE) {
            Log.v(TAG, "Received " + (tempSourceBuffer.isDirect() ? "direct" : "non-direct")
                    + " buffer of size " + mInfo.size
                    + " with" + (tempSourceBuffer.hasArray() ? "" : "out") + " array");
        }

        //buffer of shorts (16-bit samples)
        ShortBuffer tempShortBuffer = MediaHelper.getShortBuffer(tempSourceBuffer);

        mSourcePos = 0;
        mSourceSize = tempShortBuffer.remaining();
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        tempShortBuffer.get(mSourceBufferA, mSourcePos, mSourceSize);

        //Release buffer since data was copied.
        mSource.releaseBuffer(tempSourceBuffer);

        mHasBuffer = true;
    }

    @Override
    public void close() {
        super.close();
        if(mSource != null) {
            mSource.close();
            mSource = null;
        }
    }
}

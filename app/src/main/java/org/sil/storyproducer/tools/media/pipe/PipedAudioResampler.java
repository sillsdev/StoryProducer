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
public class PipedAudioResampler implements PipedMediaByteBufferSource, PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioResampler";

    private float mVolumeModifier = 1f;

    private final int mOuterSampleRate;
    private final int mOuterChannelCount;

    private PipedMediaByteBufferSource mBase;

    /**
     * Create resampler, maintaining channel count from source audio stream.
     *
     * @param sampleRate sample rate of the new, resampled audio stream.
     */
    public PipedAudioResampler(int sampleRate) {
        this(sampleRate, 0);
    }

    /**
     * Create resampler changing channel count from the source channel count to the specified channel count.
     *
     * @param sampleRate   sample rate of the new, resampled audio stream.
     * @param channelCount number of channels in the new, resampled audio stream.
     */
    public PipedAudioResampler(int sampleRate, int channelCount) {
        mOuterSampleRate = sampleRate;
        mOuterChannelCount = channelCount;
    }

    /**
     * Modify all samples by multiplying applying a constant (multiplication).
     *
     * @param volumeModifier constant to multiply all samples by.
     */
    public void setVolumeModifier(float volumeModifier) {
        mVolumeModifier = volumeModifier;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if (mBase != null) {
            throw new SourceUnacceptableException("Audio source already added!");
        }
        mBase = src;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return mBase.getMediaType();
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if (mBase == null) {
            throw new SourceUnacceptableException("Source cannot be null!");
        }

        mBase.setup();

        //Get information about source format.
        MediaFormat sourceFormat = mBase.getOutputFormat();
        int sourceSampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int sourceChannelCount = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        //Check if source format matches specified sample rate and channel count.
        boolean isSourceMatch = (mOuterChannelCount == 0 || mOuterChannelCount == sourceChannelCount)
                && (mOuterSampleRate == 0 || mOuterSampleRate == sourceSampleRate);

        if(isSourceMatch) {
            if(MediaHelper.VERBOSE) {
                Log.v(TAG, "Optimized away resampler!");
            }
        }
        else {
            //If source format does not match specs, actually use a resampler.
            InnerResampler resampler = new InnerResampler(mOuterSampleRate, mOuterChannelCount, mBase);
            resampler.setup();
            mBase = resampler;
        }
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mBase.getOutputFormat();
    }

    @Override
    public boolean isDone() {
        return mBase.isDone();
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) throws SourceClosedException {
        mBase.fillBuffer(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) throws SourceClosedException {
        return mBase.getBuffer(info);
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException, SourceClosedException {
        mBase.releaseBuffer(buffer);
    }

    @Override
    public void close() {
        if(mBase != null) {
            mBase.close();
        }
    }

    /**
     * This class holds the actual logic for the resampling, but it is contained within this inner
     * class to allow for optimization when it is unnecessary.
     */
    private class InnerResampler extends PipedAudioShortManipulator {
        @Override
        protected String getComponentName() {
            return TAG;
        }

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

        private PipedMediaByteBufferSource mSource;

        private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

        private int mSourceSize;

        /**
         * Create resampler changing channel count from the source channel count to the specified channel count.
         *
         * @param sampleRate   sample rate of the new, resampled audio stream.
         * @param channelCount number of channels in the new, resampled audio stream.
         */
        public InnerResampler(int sampleRate, int channelCount, PipedMediaByteBufferSource src) {
            mSampleRate = sampleRate;
            mChannelCount = channelCount;
            mSource = src;
        }

        @Override
        public MediaFormat getOutputFormat() {
            return mOutputFormat;
        }

        @Override
        public void setup() throws IOException, SourceUnacceptableException {
            validateSource(mSource, 0, 0);

            mSourceFormat = mSource.getOutputFormat();
            mSourceSampleRate = mSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            mSourceUsPerSample = 1000000f / mSourceSampleRate; //1000000 us/s
            mSourceChannelCount = mSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            if (mChannelCount == 0) {
                mChannelCount = mSourceChannelCount;
            }

            mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
            mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
            mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

            //Initialize sample data to 0.
            mLeftSamples = new short[mSourceChannelCount];
            mRightSamples = new short[mSourceChannelCount];
            for (int i = 0; i < mSourceChannelCount; i++) {
                mLeftSamples[i] = 0;
                mRightSamples[i] = 0;
            }

            //Get the first input buffer.
            try {
                fetchSourceBuffer();
            } catch (SourceClosedException e) {
                //This case should not happen.
                throw new SourceUnacceptableException("First fetchSourceBuffer failed! Strange...", e);
            }

            start();
        }

        /**
         * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
         * <p>
         * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
         *
         * @param channel which channel to get sample for current time
         * @return sample for current time and channel
         */
        @Override
        protected short getSampleForChannel(int channel) {
            short left, right;

            if (mChannelCount == mSourceChannelCount) {
                left = mLeftSamples[channel];
                right = mRightSamples[channel];
            } else if (mChannelCount == 1/* && mSourceChannelCount == 2*/) {
                left = (short) (mLeftSamples[0] / 2 + mLeftSamples[1] / 2);
                right = (short) (mRightSamples[0] / 2 + mRightSamples[1] / 2);
            } else { //mOuterChannelCount == 2 && mSourceChannelCount == 1
                left = mLeftSamples[0];
                right = mRightSamples[0];
            }

            if (mSeekTime == mLeftSeekTime) {
                return left;
            } else {
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
            while (mSeekTime >= mRightSeekTime) {
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

            while (mHasBuffer && mSourcePos >= mSourceSize) {
                fetchSourceBuffer();
            }
            //If we hit the end of input, use 0 as the last right sample value.
            if (!mHasBuffer) {
                isDone = true;

                mSource.close();
                mSource = null;

                for (int i = 0; i < mSourceChannelCount; i++) {
                    mRightSamples[i] = 0;
                }
            } else {
                //Get right's values from the input buffer.
                for (int i = 0; i < mSourceChannelCount; i++) {
                    try {
                        mRightSamples[i] = mSourceBufferA[mSourcePos++];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(TAG, "Tried to read beyond buffer", e);
                    }
                }
            }

            return !isDone;
        }

        private void fetchSourceBuffer() throws SourceClosedException {
            mHasBuffer = false;

            if (mSource.isDone()) {
                return;
            }

            //buffer of bytes
            ByteBuffer tempSourceBuffer = mSource.getBuffer(mInfo);

            if (MediaHelper.VERBOSE) {
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
            if (mSource != null) {
                mSource.close();
                mSource = null;
            }
        }
    }
}

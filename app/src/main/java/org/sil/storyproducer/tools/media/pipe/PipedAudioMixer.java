package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>This media pipeline component mixes raw audio streams together.</p>
 * <p>This component also optionally changes the volume of the raw audio stream.</p>
 */
public class PipedAudioMixer extends PipedAudioShortManipulator implements PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioMixer";
    @Override
    protected String getComponentName() {
        return TAG;
    }

    private MediaFormat mOutputFormat;

    private final List<PipedMediaByteBufferSource> mSources = new ArrayList<>();
    private final List<Float> mSourceVolumeModifiers = new ArrayList<>();

    private final List<short[]> mSourceBufferAs = new ArrayList<>();
    private final List<Integer> mSourcePos = new ArrayList<>();
    private final List<Integer> mSourceSizes = new ArrayList<>();

    private short[] mCurrentSample;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        addSource(src, 1);
    }

    /**
     * Specify a predecessor of this component in the pipeline with a specified volume scaling factor.
     * @param src the preceding component of the pipeline.
     * @param volumeModifier volume scaling factor.
     * @throws SourceUnacceptableException if source is null.
     */
    public void addSource(PipedMediaByteBufferSource src, float volumeModifier) throws SourceUnacceptableException {
        if(src == null) {
            throw new SourceUnacceptableException("Source cannot be null!");
        }

        mSources.add(src);
        mSourceVolumeModifiers.add(volumeModifier);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mComponentState != State.UNINITIALIZED) {
            return;
        }

        if(mSources.isEmpty()) {
            throw new SourceUnacceptableException("No sources specified!");
        }

        for (int i = 0; i < mSources.size(); i++) {
            PipedMediaByteBufferSource source = mSources.get(i);
            source.setup();
            validateSource(source, mChannelCount, mSampleRate);

            MediaFormat format = source.getOutputFormat();
            if (mChannelCount == 0) {
                mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            }
            if (mSampleRate == 0) {
                mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            }

            mSourceBufferAs.add(new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2]);
            mSourcePos.add(0);
            mSourceSizes.add(0);
            try {
                fetchSourceBuffer(i);
            } catch (SourceClosedException e) {
                //This case should not happen.
                throw new SourceUnacceptableException("First fetchSourceBuffer failed! Strange", e);
            }
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        mCurrentSample = new short[mChannelCount];

        mComponentState = State.SETUP;

        start();
    }

    protected short getSampleForChannel(int channel) {
        return mCurrentSample[channel];
    }

    @Override
    protected boolean loadSamplesForTime(long time) throws SourceClosedException {
        for(int i = 0; i < mChannelCount; i++) {
            mCurrentSample[i] = 0;
        }

        //Loop through all sources and add samples.
        for(int iSource = 0; iSource < mSources.size(); iSource++) {
            int size = mSourceSizes.get(iSource);
            int pos = mSourcePos.get(iSource);
            short[] buffer = mSourceBufferAs.get(iSource);
            float volumeModifier = mSourceVolumeModifiers.get(iSource);
            while(buffer != null && pos >= size) {
                fetchSourceBuffer(iSource);

                size = mSourceSizes.get(iSource);
                pos = mSourcePos.get(iSource);
                buffer = mSourceBufferAs.get(iSource);
            }
            if(buffer != null) {
                for(int iChannel = 0; iChannel < mChannelCount; iChannel++) {
                    mCurrentSample[iChannel] += buffer[pos++] * volumeModifier;
                }
                mSourcePos.set(iSource, pos);
            }
            else {
                //Remove depleted sources from the lists.
                mSources.remove(iSource);
                mSourceBufferAs.remove(iSource);
                mSourcePos.remove(iSource);
                mSourceSizes.remove(iSource);

                //Decrement iSource so that former source iSource + 1 is not skipped.
                iSource--;
            }
        }

        //If sources are all gone, this component is done.
        return !mSources.isEmpty();
    }

    private void fetchSourceBuffer(int sourceIndex) throws SourceClosedException {
        PipedMediaByteBufferSource source = mSources.get(sourceIndex);
        if(source.isDone()) {
            source.close();
            mSourceBufferAs.set(sourceIndex, null);
            return;
        }

        //buffer of bytes
        ByteBuffer buffer = source.getBuffer(mInfo);
        //buffer of shorts (16-bit samples)
        ShortBuffer sBuffer = MediaHelper.getShortBuffer(buffer);

        int pos = 0;
        int size = sBuffer.remaining();
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mSourceBufferAs.get(sourceIndex), pos, size);
        mSourcePos.set(sourceIndex, pos);
        mSourceSizes.set(sourceIndex, size);

        //Release buffer since data was copied.
        source.releaseBuffer(buffer);
    }

    @Override
    public void close() {
        super.close();
        while(!mSources.isEmpty()) {
            PipedMediaByteBufferSource source = mSources.remove(0);
            source.close();
        }
    }
}
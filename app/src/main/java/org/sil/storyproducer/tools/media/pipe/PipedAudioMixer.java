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

    private MediaFormat mOutputFormat;

    private List<PipedMediaByteBufferSource> mSources = new ArrayList<>();
    private List<Float> mSourceVolumeModifiers = new ArrayList<>();

    private List<short[]> mSourceBufferAs = new ArrayList<>();
    private List<Integer> mSourcePos = new ArrayList<>();
    private List<Integer> mSourceSizes = new ArrayList<>();

    private short[] mCurrentSample;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    public PipedAudioMixer() {
        //empty default constructor
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
        for (int i = 0; i < mSources.size(); i++) {
            PipedMediaByteBufferSource source = mSources.get(i);
            source.setup();
            MediaFormat format = source.getOutputFormat();

            if (source.getMediaType() != MediaHelper.MediaType.AUDIO) {
                throw new SourceUnacceptableException("Source must be audio!");
            }

            if(!format.getString(MediaFormat.KEY_MIME).equals(MediaHelper.MIMETYPE_RAW_AUDIO)) {
                throw new SourceUnacceptableException("Source audio must be a raw audio stream!");
            }

            if (mChannelCount == 0) {
                mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            } else if (mChannelCount != format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                throw new SourceUnacceptableException("Source audio channel counts don't match!");
            }

            if (mSampleRate == 0) {
                mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            } else if (mSampleRate != format.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
                throw new SourceUnacceptableException("Source audio sample rates don't match!");
            }

            mSourceBufferAs.add(new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2]);
            mSourcePos.add(0);
            mSourceSizes.add(0);
            fetchSourceBuffer(i);
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        mCurrentSample = new short[mChannelCount];

        start();
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    protected short getSampleForChannel(int channel) {
        return mCurrentSample[channel];
    }

    @Override
    protected boolean loadSamplesForTime(long time) {
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
                releaseSourceBuffer(iSource);
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

    private void fetchSourceBuffer(int sourceIndex) {
        PipedMediaByteBufferSource source = mSources.get(sourceIndex);
        if(source.isDone()) {
            mSourceBufferAs.set(sourceIndex, null);
            return;
        }
        ByteBuffer buffer = source.getBuffer(mInfo);
        ShortBuffer sBuffer = MediaHelper.getShortBuffer(buffer);
        int size = sBuffer.remaining();
        sBuffer.get(mSourceBufferAs.get(sourceIndex), 0, size);
        mSourcePos.set(sourceIndex, 0);
        mSourceSizes.set(sourceIndex, size);
        source.releaseBuffer(buffer);
    }

    private void releaseSourceBuffer(int sourceIndex) {
//        mSources.get(sourceIndex).releaseBuffer(mSourceBuffers.get(sourceIndex));
//        mSourceBuffers.set(sourceIndex, null);
//        mSourceShortBuffers.set(sourceIndex, null);
    }

    @Override
    public void close() {
        super.close();
        for(PipedMediaByteBufferSource source : mSources) {
            source.close();
        }
    }
}
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

    private boolean mIsDone = false;

    private List<PipedMediaByteBufferSource> mSources = new ArrayList<>();
    private List<Float> mSourceVolumeModifiers = new ArrayList<>();
//    private List<ByteBuffer> mSourceBuffers = new ArrayList<>();
//    private List<ShortBuffer> mSourceShortBuffers = new ArrayList<>();

    private List<short[]> mSourceBufferAs = new ArrayList<>();
    private List<Integer> mSourcePos = new ArrayList<>();
    private List<Integer> mSourceSizes = new ArrayList<>();

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
//        mSourceBuffers.add(null);
//        mSourceShortBuffers.add(null);
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
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    protected short getSampleForTime(long time, int channel) {
        int sum = 0;

        //Loop through all sources and add samples.
        for(int i = 0; i < mSources.size(); i++) {
//            ShortBuffer sBuffer = mSourceShortBuffers.get(i);
            int size = mSourceSizes.get(i);
            int pos = mSourcePos.get(i);
            short[] buffer = mSourceBufferAs.get(i);
            float volumeModifier = mSourceVolumeModifiers.get(i);
            while(buffer != null && pos >= size) {
//                while(sBuffer != null && sBuffer.remaining() <= 0) {
                releaseSourceBuffer(i);
                fetchSourceBuffer(i);

                size = mSourceSizes.get(i);
                pos = mSourcePos.get(i);
                buffer = mSourceBufferAs.get(i);
//                sBuffer = mSourceShortBuffers.get(i);
            }
            if(buffer != null) {
//                if(sBuffer != null) {
//                sum += sBuffer.get() * volumeModifier;
                sum += buffer[pos++] * volumeModifier;
                mSourcePos.set(i, pos);
            }
            else {
                //Remove depleted sources from the lists.
                mSources.remove(i);
                mSourceBufferAs.remove(i);
                mSourcePos.remove(i);
                mSourceSizes.remove(i);
//                mSourceBuffers.remove(i);
//                mSourceShortBuffers.remove(i);

                //Decrement i so that former source i + 1 is not skipped.
                i--;
            }
        }

        //If sources are all gone, this component is done.
        if(mSources.isEmpty()) {
            mIsDone = true;
        }

        //Clip the sum to a short.
        return (short) sum;
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
//        mSourceBuffers.set(sourceIndex, buffer);
//        mSourceShortBuffers.set(sourceIndex, MediaHelper.getShortBuffer(buffer));
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
package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class PipedAudioMixer extends PipedAudioShortManipulator implements PipedMediaByteBufferDest {
    private static final String TAG = "PipedAudioMixer";

    private MediaFormat mOutputFormat;

    private boolean mIsDone = false;

    private List<PipedMediaByteBufferSource> mSources = new ArrayList<>();
    private List<ByteBuffer> mSourceBuffers = new ArrayList<>();
    private List<ShortBuffer> mSourceShortBuffers = new ArrayList<>();

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        mSources.add(src);
        mSourceBuffers.add(null);
        mSourceShortBuffers.add(null);
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

    /**
     * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
     *
     * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
     * @param time
     * @param channel
     * @return
     */
    protected short getSampleForTime(long time, int channel) {
        int sum = 0;

        //Loop through all sources and add samples.
        for(int i = 0; i < mSources.size(); i++) {
            ShortBuffer sBuffer = mSourceShortBuffers.get(i);
            while(sBuffer != null && sBuffer.remaining() <= 0) {
                releaseSourceBuffer(i);
                fetchSourceBuffer(i);
                sBuffer = mSourceShortBuffers.get(i);
            }
            if(sBuffer != null) {
                sum += sBuffer.get();
            }
            else {
                //Remove depleted sources from the lists.
                mSources.remove(i);
                mSourceBuffers.remove(i);
                mSourceShortBuffers.remove(i);

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
            return;
        }
        ByteBuffer buffer = source.getBuffer(mInfo);
        mSourceBuffers.set(sourceIndex, buffer);
        mSourceShortBuffers.set(sourceIndex, MediaHelper.getShortBuffer(buffer));
    }

    private void releaseSourceBuffer(int sourceIndex) {
        mSources.get(sourceIndex).releaseBuffer(mSourceBuffers.get(sourceIndex));
        mSourceBuffers.set(sourceIndex, null);
        mSourceShortBuffers.set(sourceIndex, null);
    }

    @Override
    public void close() throws IOException {
        super.close();
        for(PipedMediaByteBufferSource source : mSources) {
            source.close();
        }
    }
}
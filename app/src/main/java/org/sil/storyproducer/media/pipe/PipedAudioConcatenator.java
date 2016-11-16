package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

//TODO: class is very much incomplete
public class PipedAudioConcatenator extends PipedAudioShortManipulator  {

    private long mDelayUs;
    private long baseLineUs = 0;

    private Queue<PipedMediaByteBufferSource> mSources = new LinkedList<>();
    private PipedMediaByteBufferSource mSource;
    private ByteBuffer mSourceBuffer;
    private ShortBuffer mSourceShortBuffer;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private MediaFormat mOutputFormat;

    private boolean mIsDone = false;

    public PipedAudioConcatenator(long delayUs) {
        mDelayUs = delayUs;
    }

    @Override
    protected short getSampleForTime(long time, int channel) {
        if(mSource == null && time > baseLineUs + mDelayUs) {
            //If sources are all gone, this component is done.
            if(mSources.isEmpty()) {
                mIsDone = true;
                return 0;
            }
            else {
                mSource = mSources.remove();
                fetchSourceBuffer();
            }
        }

        if(mSource == null) {
            return 0;
        }
        else {
            while(mSourceShortBuffer != null && !mSourceShortBuffer.hasRemaining()) {
                releaseSourceBuffer();
                fetchSourceBuffer();
            }
            if(mSourceShortBuffer != null) {
                return mSourceShortBuffer.get();
            }
            else {
                mSource = null;
                baseLineUs = time;
                return 0;
            }
        }
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        mSources.add(src);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mSources.isEmpty()) {
            throw new SourceUnacceptableException("No sources provided!");
        }

        for(PipedMediaByteBufferSource source : mSources) {
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

    private void releaseSourceBuffer() {
        mSource.releaseBuffer(mSourceBuffer);
        mSourceBuffer = null;
        mSourceShortBuffer = null;
    }

    private void fetchSourceBuffer() {
        //If our source has no more output, leave the buffers as null (assumed from releaseInputBuffer).
        if(mSource.isDone()) {
            return;
        }

        //Pull in new buffer.
        mSourceBuffer = mSource.getBuffer(mInfo);
        mSourceShortBuffer = MediaHelper.getShortBuffer(mSourceBuffer);
    }
}

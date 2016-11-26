package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This media pipeline component concatenates raw audio streams with specified delay in between streams.
 *
 * This component also optionally ensures that each audio stream matches an expected duration.
 */
public class PipedAudioConcatenator extends PipedAudioShortManipulator {

    private PipedMediaByteBufferSource mFirstSource;
    private Queue<String> mSourceAudioPaths = new LinkedList<>();

    private long mDelayUs;
    private long mDelayStartUs = 0;
    private long mSourceStartUs = 0;

    private Queue<Long> mSourceExpectedDurations = new LinkedList<>();
    private PipedMediaByteBufferSource mSource;
    private long mSourceExpectedDuration;
    private ByteBuffer mSourceBuffer;
    private ShortBuffer mSourceShortBuffer;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private MediaFormat mOutputFormat;

    private boolean mIsDone = false;

    public PipedAudioConcatenator(long delayUs, int sampleRate, int channelCount) {
        mDelayUs = delayUs;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
    }

    public PipedAudioConcatenator(long delayUs) {
        mDelayUs = delayUs;
    }

    @Override
    protected short getSampleForTime(long time, int channel) {
        if(mSource == null && time > mDelayStartUs + mDelayUs) {
            //If sources are all gone, this component is done.
            if(mSourceAudioPaths.isEmpty()) {
                mIsDone = true;
                return 0;
            }
            else {
                mSourceStartUs = mSourceStartUs + mSourceExpectedDuration + mDelayUs;

                mSource = getNextSource();
                mSourceExpectedDuration = mSourceExpectedDurations.remove();
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

            boolean isWithinExpectedTime = mSourceExpectedDuration == 0
                    || time <= mSourceStartUs + mSourceExpectedDuration;

            if(mSourceShortBuffer != null && isWithinExpectedTime) {
                return mSourceShortBuffer.get();
            }
            else {
                try {
                    mSource.close();
                } catch (IOException e) {
                    //TODO
                    e.printStackTrace();
                }
                mSource = null;
                if(mSourceExpectedDuration == 0) {
                    mDelayStartUs = time;
                }
                else {
                    mDelayStartUs = mSourceStartUs + mSourceExpectedDuration;
                }
                return 0;
            }
        }
    }

    private PipedMediaByteBufferSource getNextSource() {
        PipedMediaByteBufferSource nextSource = null;
        if(mFirstSource != null) {
            nextSource = mFirstSource;
            mFirstSource = null;
        }
        else if(!mSourceAudioPaths.isEmpty()) {
            String nextSourcePath = mSourceAudioPaths.remove();
            nextSource = new PipedAudioDecoderMaverick(nextSourcePath, mSampleRate, mChannelCount, 1);
            try {
                nextSource.setup();
                checkSourceValidity(nextSource);
            } catch (IOException | SourceUnacceptableException e) {
                e.printStackTrace();
                throw new RuntimeException("Source setup failed!", e);
            }

        }
        return nextSource;
    }

    public void addSource(String sourcePath) throws SourceUnacceptableException {
        addSource(sourcePath, 0);
    }

    /**
     * <p>Add a source with an associated expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n + 1 delays.</p>
     *
     * @param sourcePath source audio path
     * @param duration expected duration of the source audio stream
     */
    public void addSource(String sourcePath, long duration) throws SourceUnacceptableException {
        mSourceAudioPaths.add(sourcePath);
        mSourceExpectedDurations.add(duration);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mSourceAudioPaths.isEmpty()) {
            throw new SourceUnacceptableException("No sources provided!");
        }

        String nextSourcePath = mSourceAudioPaths.remove();
        PipedAudioDecoderMaverick firstSource = new PipedAudioDecoderMaverick(nextSourcePath);

        if(mSampleRate > 0) {
            firstSource.setSampleRate(mSampleRate);
        }
        if(mChannelCount > 0) {
            firstSource.setChannelCount(mChannelCount);
        }

        firstSource.setup();

        MediaFormat format = firstSource.getOutputFormat();

        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mFirstSource = firstSource;

        checkSourceValidity(mFirstSource);

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
    }

    private void checkSourceValidity(PipedMediaByteBufferSource source) throws SourceUnacceptableException {
        MediaFormat format = source.getOutputFormat();

        if (source.getMediaType() != MediaHelper.MediaType.AUDIO) {
            throw new SourceUnacceptableException("Source must be audio!");
        }

        if(!format.getString(MediaFormat.KEY_MIME).equals(MediaHelper.MIMETYPE_RAW_AUDIO)) {
            throw new SourceUnacceptableException("Source audio must be a raw audio stream!");
        }

        if (mChannelCount != format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
            throw new SourceUnacceptableException("Source audio channel counts don't match!");
        }

        if (mSampleRate != format.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw new SourceUnacceptableException("Source audio sample rates don't match!");
        }
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

    @Override
    public void close() throws IOException {
        //TODO
        if(mSource != null) {
            mSource.close();
        }
    }
}

package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>This media pipeline component concatenates raw audio streams with specified transition time
 * in between streams. Note that this transition time is halved for the beginning and end of the stream.</p>
 * <p>This component also optionally ensures that each audio stream matches an expected duration.</p>
 */
public class PipedAudioConcatenator extends PipedAudioShortManipulator {

    private PipedMediaByteBufferSource mFirstSource;
    private Queue<String> mSourceAudioPaths = new LinkedList<>();

    private final long mTransitionUs; //duration of the audio transition
    private long mTransitionStart = 0;
    private long mSourceStart = 0;

    private Queue<Long> mSourceExpectedDurations = new LinkedList<>();
    private PipedMediaByteBufferSource mSource; //current source
    private long mSourceExpectedDuration; //current source expected duration
    private ByteBuffer mSourceBuffer; //currently held ByteBuffer of current source
    private ShortBuffer mSourceShortBuffer; //ShortBuffer for mSourceBuffer

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private MediaFormat mOutputFormat;

    private boolean mIsDone = false;

    /**
     * Create concatenator with specified transition time, using the first audio source's format.
     * @param transitionUs length of audio transitions (dead space between audio sources) in microseconds.
     */
    public PipedAudioConcatenator(long transitionUs) {
        this(transitionUs, 0, 0);
    }

    /**
     * Create concatenator with specified transition time, resampling the audio stream.
     * @param transitionUs length of audio transitions (dead space between audio sources) in microseconds.
     * @param sampleRate desired sample rate.
     * @param channelCount desired channel count.
     */
    public PipedAudioConcatenator(long transitionUs, int sampleRate, int channelCount) {
        mTransitionUs = transitionUs;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
    }

    @Override
    protected short getSampleForTime(long time, int channel) {
        long nextTransitionUs = mTransitionUs;
        //For pre-first-source and last source, make the transition half as long.
        if(mFirstSource != null || mSourceAudioPaths.isEmpty()) {
            nextTransitionUs /= 2;
        }

        boolean inBetweenSources = mSource == null;
        if(inBetweenSources && time > mTransitionStart + nextTransitionUs) {
            //If sources are all gone, this component is done.
            if(mSourceAudioPaths.isEmpty()) {
                mIsDone = true;
                return 0;
            }
            else {
                mSourceStart = mSourceStart + mSourceExpectedDuration + nextTransitionUs;

                mSource = getNextSource();
                mSourceExpectedDuration = mSourceExpectedDurations.remove();
                fetchSourceBuffer();

                inBetweenSources = mSource == null; //only true if source was invalid
            }
        }

        if(inBetweenSources) {
            return 0;
        }
        else {
            while(mSourceShortBuffer != null && !mSourceShortBuffer.hasRemaining()) {
                releaseSourceBuffer();
                fetchSourceBuffer();
            }

            boolean isWithinExpectedTime = mSourceExpectedDuration == 0
                    || time <= mSourceStart + mSourceExpectedDuration;

            if(mSourceShortBuffer != null && isWithinExpectedTime) {
                //In the normal case, return the short from the buffer.
                return mSourceShortBuffer.get();
            }
            else {
                //Clear out the current source.
                mSource.close();
                mSource = null;

                //If no particular duration was expected, start transition now. Otherwise use precise time.
                if(mSourceExpectedDuration == 0) {
                    mTransitionStart = time;
                }
                else {
                    mTransitionStart = mSourceStart + mSourceExpectedDuration;
                }

                //Return first 0 of transition period.
                return 0;
            }
        }
    }

    private PipedMediaByteBufferSource getNextSource() {
        PipedMediaByteBufferSource nextSource = null;

        //Since the first source was already setup in setup(), it is held in a special place.
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
                //If we encounter an error, just let this source be passed over.
                new Exception("Source setup failed!", e).printStackTrace();
                nextSource = null;
            }

        }

        return nextSource;
    }

    /**
     * <p>Add a source without an expected duration. The audio stream will be used in its entirety.</p>
     *
     * @param sourcePath source audio path.
     */
    public void addSource(String sourcePath) throws SourceUnacceptableException {
        addSource(sourcePath, 0);
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
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
        PipedAudioDecoderMaverick firstSource;

        //If sample rate and channel count were specified, apply them to the first source.
        if(mSampleRate > 0) {
            firstSource = new PipedAudioDecoderMaverick(nextSourcePath, mSampleRate, mChannelCount);
        }
        else {
            firstSource = new PipedAudioDecoderMaverick(nextSourcePath);
        }

        firstSource.setup();

        MediaFormat format = firstSource.getOutputFormat();

        //In any event, the first source's sample rate and channel count become the standard.
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
    public void close() {
        if(mSource != null) {
            mSource.close();
        }
    }
}

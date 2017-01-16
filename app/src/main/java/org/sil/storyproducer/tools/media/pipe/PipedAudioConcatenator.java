package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

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
    private static final String TAG = "PipedAudioConcatenator";

    private enum ConcatState {
        DATA,
        DONE,
        //INVALID_SOURCE,
        TRANSITION,
        ;
    }

    private ConcatState mCurrentState = ConcatState.TRANSITION;

    private PipedMediaByteBufferSource mFirstSource;
    private Queue<String> mSourceAudioPaths = new LinkedList<>();

    private final long mTransitionUs; //duration of the audio transition
    private long mTransitionStart = 0;
    private long mSourceStart = 0;

    private Queue<Long> mSourceExpectedDurations = new LinkedList<>();
    private PipedMediaByteBufferSource mSource; //current source
    private long mSourceExpectedDuration; //current source expected duration

    private final short[] mSourceBufferA = new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2];
    private boolean mHasBuffer = false;
    private int mPos;
    private int mSize;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private MediaFormat mOutputFormat;

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
    protected short getSampleForChannel(int channel) {
        if(mCurrentState == ConcatState.DATA) {
            //In the normal case, return the short from the buffer.
            return mSourceBufferA[mPos++];
        }
        else {
            return 0;
        }
    }

    @Override
    protected boolean loadSamplesForTime(long time) {
        boolean isDone = false;

        if(mCurrentState == ConcatState.TRANSITION) {
            long transitionUs = mTransitionUs;
            //For pre-first-source and last source, make the transition half as long.
            if (mFirstSource != null || mSourceAudioPaths.isEmpty()) {
                transitionUs /= 2;
            }

            boolean transitionComplete = time > mTransitionStart + transitionUs;
            if (transitionComplete) {
                if(MediaHelper.VERBOSE) {
                    Log.d(TAG, "loadSamplesForTime transition complete!");
                }

                //Clear out the current source.
                if(mSource != null) {
                    mSource.close();
                    mSource = null;
                }

                //Assume DATA state.
                mCurrentState = ConcatState.DATA;

                //Get a (valid) source or get to DONE state.
                while (mSource == null && !isDone) {
                    //If sources are all gone, this component is done.
                    if (mSourceAudioPaths.isEmpty()) {
                        isDone = true;
                        mCurrentState = ConcatState.DONE;
                    } else {
                        mSourceStart = mSourceStart + mSourceExpectedDuration + transitionUs;

                        mSource = getNextSource();
                        mSourceExpectedDuration = mSourceExpectedDurations.remove();
                    }
                }

                if (mCurrentState == ConcatState.DATA) {
                    fetchSourceBuffer();
                }
            }
        }

        if(mCurrentState == ConcatState.DATA) {
            while(mHasBuffer && mPos >= mSize) {
                releaseSourceBuffer();
                fetchSourceBuffer();
            }

            boolean isWithinExpectedTime = mSourceExpectedDuration == 0
                    || time <= mSourceStart + mSourceExpectedDuration;

            if(mHasBuffer && isWithinExpectedTime) {
                //Do nothing. (Stay in DATA state.)
            }
            else {
                if(MediaHelper.VERBOSE) {
                    Log.d(TAG, "loadSamplesForTime starting transition...");
                }

                mCurrentState = ConcatState.TRANSITION;

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
            }
        }

        return !isDone;
    }

    private PipedMediaByteBufferSource getNextSource() {
        if(MediaHelper.VERBOSE) {
            Log.d(TAG, "getNextSource starting...");
        }

        PipedMediaByteBufferSource nextSource = null;

        //Since the first source was already setup in setup(), it is held in a special place.
        if(mFirstSource != null) {
            if(MediaHelper.VERBOSE) {
                Log.d(TAG, "getNextSource first source");
            }

            nextSource = mFirstSource;
            mFirstSource = null;
        }
        else if(!mSourceAudioPaths.isEmpty()) {
            if(MediaHelper.VERBOSE) {
                Log.d(TAG, "getNextSource normal source");
            }

            String nextSourcePath = mSourceAudioPaths.remove();
            nextSource = new PipedAudioDecoderMaverick(nextSourcePath, mSampleRate, mChannelCount, 1);
            try {
                nextSource.setup();
                checkSourceValidity(nextSource);
            } catch (IOException | SourceUnacceptableException e) {
                //If we encounter an error, just let this source be passed over.
                new RuntimeException("Source setup failed!", e).printStackTrace();
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

        start();
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

    private void releaseSourceBuffer() {
        mHasBuffer = false;
    }

    private void fetchSourceBuffer() {
        //If our source has no more output, leave the buffers as null (assumed from releaseInputBuffer).
        if(mSource.isDone()) {
            return;
        }

        //Pull in new buffer.
        ByteBuffer buffer = mSource.getBuffer(mInfo);
        ShortBuffer sBuffer = MediaHelper.getShortBuffer(buffer);

        mPos = 0;
        mSize = sBuffer.remaining();

        sBuffer.get(mSourceBufferA, mPos, mSize);

        mSource.releaseBuffer(buffer);

        mHasBuffer = true;
    }

    @Override
    public void close() {
        super.close();
        if(mSource != null) {
            mSource.close();
        }
    }
}

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
    @Override
    protected String getComponentName() {
        return TAG;
    }

    private enum ConcatState {
        DATA,
        DONE,
        BEFORE_SOURCE,
        TRANSITION,
        ;
    }

    private ConcatState mCurrentState = ConcatState.TRANSITION; //start in transition

    private final Queue<String> mSourceAudioPaths = new LinkedList<>();
    private final Queue<Long> mSourceExpectedDurations = new LinkedList<>();

    private PipedMediaByteBufferSource mFirstSource;
    private PipedMediaByteBufferSource mSource; //current source
    private long mSourceExpectedDuration; //current source expected duration (us)

    private final long mTransitionUs; //duration of the audio transition
    private long mTransitionStart = 0; //timestamp (us) of current transition start
    private long mSourceStart = 0; //timestamp (us) of current source start (i.e. after prior transition)

    private final short[] mSourceBufferA = new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2]; //short = 2 bytes
    private boolean mHasMoreBuffers = false;

    private int mPos;
    private int mSize;

    private final MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

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
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
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
    public void addSource(String sourcePath, long duration) {
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

        validateSource(mFirstSource);

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        start();
    }

    @Override
    protected short getSampleForChannel(int channel) {
        if(mCurrentState == ConcatState.DATA) {
            return mSourceBufferA[mPos + channel];
        }
        else {
            return 0;
        }
    }

    @Override
    protected boolean loadSamplesForTime(long time) throws SourceClosedException {
        boolean isDone = false;

        if(mCurrentState == ConcatState.TRANSITION) {
            long transitionUs = mTransitionUs;
            //For pre-first-source and last source, make the transition half as long.
            if (mFirstSource != null || mSourceAudioPaths.isEmpty()) {
                transitionUs /= 2;
            }

            boolean transitionComplete = time > mTransitionStart + transitionUs;
            if (transitionComplete) {
                if(MediaHelper.VERBOSE) Log.v(TAG, "loadSamplesForTime transition complete!");

                //Clear out the current source.
                if(mSource != null) {
                    mSource.close();
                    mSource = null;
                }

                //Reset these stats so fetchSourceBuffer loop will trigger.
                mPos = 0;
                mSize = 0;
                mHasMoreBuffers = true;

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

                if(!isDone && mSourceStart > time) {
                    mCurrentState = ConcatState.BEFORE_SOURCE;
                }
            }
        }

        if(mCurrentState == ConcatState.BEFORE_SOURCE && time >= mSourceStart) {
            mCurrentState = ConcatState.DATA;
        }

        if(mCurrentState == ConcatState.DATA) {
            mPos += mChannelCount;

            while (mHasMoreBuffers && mPos >= mSize) {
                fetchSourceBuffer();
            }
            boolean isWithinExpectedTime = mSourceExpectedDuration == 0
                    || time <= mSourceStart + mSourceExpectedDuration;

            if(!mHasMoreBuffers || !isWithinExpectedTime) {
                if(MediaHelper.VERBOSE) Log.v(TAG, "loadSamplesForTime starting transition...");

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

    private void fetchSourceBuffer() throws SourceClosedException {
        mHasMoreBuffers = false;

        if(mSource.isDone()) {
            return;
        }

        //buffer of bytes
        ByteBuffer buffer = mSource.getBuffer(mInfo);
        //buffer of shorts (16-bit samples)
        ShortBuffer sBuffer = MediaHelper.getShortBuffer(buffer);

        mPos = 0;
        mSize = sBuffer.remaining();
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mSourceBufferA, mPos, mSize);

        //Release buffer since data was copied.
        mSource.releaseBuffer(buffer);

        mHasMoreBuffers = true;
    }

    private PipedMediaByteBufferSource getNextSource() {
        if(MediaHelper.VERBOSE) Log.v(TAG, "getNextSource starting...");

        PipedMediaByteBufferSource nextSource = null;

        //Since the first source was already setup in setup(), it is held in a special place.
        if(mFirstSource != null) {
            if(MediaHelper.VERBOSE) Log.v(TAG, "getNextSource first source");

            nextSource = mFirstSource;
            mFirstSource = null;
        }
        else if(!mSourceAudioPaths.isEmpty()) {
            if(MediaHelper.VERBOSE) Log.v(TAG, "getNextSource normal source");

            String nextSourcePath = mSourceAudioPaths.remove();
            if(nextSourcePath == null) {
                return null;
            }

            nextSource = new PipedAudioDecoderMaverick(nextSourcePath, mSampleRate, mChannelCount, 1);
            try {
                nextSource.setup();
                validateSource(nextSource);
            } catch (IOException | SourceUnacceptableException e) {
                //If we encounter an error, just let this source be passed over.
                Log.e(TAG, "Source setup failed!", e);
                nextSource = null;
            }
        }

        return nextSource;
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

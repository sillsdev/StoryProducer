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
public class PipedAudioConcatenator extends PipedAudioShortManipulator implements PipedMediaByteBufferDest {
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

    private final Queue<PipedMediaByteBufferSource> mSources = new LinkedList<>();
    private final Queue<Long> mSourceExpectedDurations = new LinkedList<>();

    private long mFadeOutUs = 0;

    private PipedMediaByteBufferSource mSource; //current source
    private long mSourceExpectedDuration; //current source expected duration (us)

    private final long mTransitionUs; //duration of the audio transition

    private long mTransitionStart = 0; //timestamp (us) of current transition start
    private long mSourceStart = 0; //timestamp (us) of current source start (i.e. after prior transition)

    private final short[] mSourceBufferA = new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2]; //short = 2 bytes
    private boolean mHasMoreBuffers = false;

    private int mPos;
    private int mSize;
    private float mVolumeModifier;

    private final MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    private MediaFormat mOutputFormat;

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

    /**
     * Set a duration for each audio segment to fade out before the transition period after it (or the end).
     * @param fadeOutUs microseconds to fade out each audio segment.
     */
    public void setFadeOut(long fadeOutUs) {
        mFadeOutUs = fadeOutUs;
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    /**
     * <p>Add a source without an expected duration. The audio stream will be used in its entirety.</p>
     *
     * @param source source audio path.
     */
    @Override
    public void addSource(PipedMediaByteBufferSource source) throws SourceUnacceptableException {
        addSource(source, 0);
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * @param source source audio path.
     * @param duration expected duration of the source audio stream.
     */
    public void addSource(PipedMediaByteBufferSource source, long duration) throws SourceUnacceptableException {
        mSources.add(source);
        mSourceExpectedDurations.add(duration);
    }

    /**
     * <p>Add a source without an expected duration. The audio stream will be used in its entirety.</p>
     *
     * @param sourcePath source audio path.
     */
    public void addSourcePath(String sourcePath) throws SourceUnacceptableException {
        addSourcePath(sourcePath, 0);
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * <p>This function differs from {@link #addLoopingSourcePath(String, long)} by padding the source audio
     * with silence until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.</p>
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    public void addSourcePath(String sourcePath, long duration) throws SourceUnacceptableException {
        if(sourcePath != null) {
            addSource(new PipedAudioDecoderMaverick(sourcePath), duration);
        }
        else {
            addSource(null, duration);
        }
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * <p>This function differs from {@link #addSourcePath(String, long)} by looping the source audio
     * until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.</p>
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    public void addLoopingSourcePath(String sourcePath, long duration) throws SourceUnacceptableException {
        if(sourcePath != null) {
            long sourceDuration = MediaHelper.getAudioDuration(sourcePath);
            if(sourceDuration < duration) {
                //Only add a looper if necessary
                addSource(new PipedAudioLooper(sourcePath, duration), duration);
            }
            else {
                addSourcePath(sourcePath, duration);
            }
        }
        else {
            addSource(null, duration);
        }
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mComponentState != State.UNINITIALIZED) {
            return;
        }

        if(mSources.isEmpty()) {
            throw new SourceUnacceptableException("No sources provided!");
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        mComponentState = State.SETUP;

        start();
    }

    @Override
    protected short getSampleForChannel(int channel) {
        if(mCurrentState == ConcatState.DATA) {
            return (short) (mVolumeModifier * mSourceBufferA[mPos + channel]);
        }
        else {
            return 0;
        }
    }

    @Override
    protected boolean loadSamplesForTime(long time) throws SourceClosedException {
        boolean isDone = false;

        //Reset volume modifier.
        mVolumeModifier = 1;

        if(mCurrentState == ConcatState.TRANSITION) {
            long transitionUs = mTransitionUs;
            //For pre-first-source and last source, make the transition half as long.
            if (time <= mTransitionUs / 2 || mSources.isEmpty()) {
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
                    if (mSources.isEmpty()) {
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

            long sourceEnd = mSourceStart + mSourceExpectedDuration;
            long sourceRemainingDuration = sourceEnd - time;

            if(sourceRemainingDuration < mFadeOutUs) {
                mVolumeModifier = sourceRemainingDuration / (float) mFadeOutUs;
            }

            while (mHasMoreBuffers && mPos >= mSize) {
                fetchSourceBuffer();
            }
            boolean isWithinExpectedTime = mSourceExpectedDuration == 0
                    || time <= sourceEnd;

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
                    mTransitionStart = sourceEnd;
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

        if(!mSources.isEmpty()) {
            if(MediaHelper.VERBOSE) Log.v(TAG, "getNextSource source found");

            nextSource = mSources.remove();
            if(nextSource == null) {
                return null;
            }

            try {
                nextSource.setup();

                nextSource = PipedAudioResampler.correctSampling(nextSource, mSampleRate, mChannelCount);
                nextSource.setup();

                validateSource(nextSource);
            } catch (IOException | SourceUnacceptableException e) {
                //If we encounter an error, just let this source be passed over.
                Log.e(TAG, "Silencing failed source setup....", e);
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

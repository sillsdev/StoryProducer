package org.sil.storyproducer.tools.media.pipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.LinkedList
import java.util.Queue

/**
 *
 * This media pipeline component concatenates raw audio streams with specified transition time
 * in between streams. Note that this transition time is halved for the beginning and end of the stream.
 *
 * This component also optionally ensures that each audio stream matches an expected duration.
 */
class PipedAudioConcatenator
/**
 * Create concatenator with specified transition time, resampling the audio stream.
 * @param transitionUs length of audio transitions (dead space between audio sources) in microseconds.
 * @param sampleRate desired sample rate.
 * @param channelCount desired channel count.
 */
(private val context: Context, private val mTransitionUs: Long //duration of the audio transition
 , sampleRate: Int, channelCount: Int) : PipedAudioShortManipulator(), PipedMediaByteBufferDest {

    private var mCurrentState = ConcatState.TRANSITION //start in transition

    private val mSources = LinkedList<PipedMediaByteBufferSource>()
    private val mSourceExpectedDurations = LinkedList<Long>()

    private var mFadeOutUs: Long = 0

    private var mSource: PipedMediaByteBufferSource? = null //current source
    private var mSourceExpectedDuration: Long = 0 //current source expected duration (us)

    private var mTransitionStart: Long = 0 //timestamp (us) of current transition start
    private var mSourceStart: Long = 0 //timestamp (us) of current source start (i.e. after prior transition)

    private val mSourceBufferA = ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2) //short = 2 bytes
    private var mHasMoreBuffers = false

    private var mPos: Int = 0
    private var mSize: Int = 0
    private var mVolumeModifier: Float = 0.toFloat()

    private val mInfo = MediaCodec.BufferInfo()

    private var mOutputFormat: MediaFormat? = null

    private//If we encounter an error, just let this source be passed over.
    val nextSource: PipedMediaByteBufferSource?
        get() {
            if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource starting")

            var nextSource: PipedMediaByteBufferSource? = null

            if (!mSources.isEmpty()) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource source found")

                nextSource = mSources.remove()
                if (nextSource == null) {
                    return null
                }

                try {
                    nextSource.setup()

                    nextSource = PipedAudioResampler.correctSampling(nextSource, mSampleRate, mChannelCount)
                    nextSource!!.setup()

                    validateSource(nextSource)
                } catch (e: IOException) {
                    Log.e(TAG, "Silencing failed source setup.", e)
                    nextSource = null
                } catch (e: SourceUnacceptableException) {
                    Log.e(TAG, "Silencing failed source setup.", e)
                    nextSource = null
                }

            }

            return nextSource
        }

    override fun getComponentName(): String {
        return TAG
    }

    private enum class ConcatState {
        DATA,
        DONE,
        BEFORE_SOURCE,
        TRANSITION
    }

    init {
        mSampleRate = sampleRate
        mChannelCount = channelCount
    }

    /**
     * Set a duration for each audio segment to fade out before the transition period after it (or the end).
     * @param fadeOutUs microseconds to fade out each audio segment.
     */
    fun setFadeOut(fadeOutUs: Long) {
        mFadeOutUs = fadeOutUs
    }

    override fun getOutputFormat(): MediaFormat? {
        return mOutputFormat
    }

    /**
     *
     * Add a source without an expected duration. The audio stream will be used in its entirety.
     *
     * @param source source audio path.
     */
    @Throws(SourceUnacceptableException::class)
    override fun addSource(source: PipedMediaByteBufferSource) {
        addSource(source, 0)
    }

    /**
     *
     * Add a source with an expected duration. The expected duration is guaranteed.
     *
     *
     * In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.
     *
     * @param source source audio path.
     * @param duration expected duration of the source audio stream.
     */
    @Throws(SourceUnacceptableException::class)
    fun addSource(source: PipedMediaByteBufferSource?, duration: Long) {
        if(source!=null) mSources.add(source)
        mSourceExpectedDurations.add(duration)
    }

    /**
     *
     * Add a source with an expected duration. The expected duration is guaranteed.
     *
     *
     * In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.
     *
     *
     * This function differs from [.addLoopingSourcePath] by padding the source audio
     * with silence until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    @Throws(SourceUnacceptableException::class)
    @JvmOverloads
    fun addSourcePath(sourcePath: String?, duration: Long = 0) {
        if (sourcePath != null) {
            addSource(PipedAudioDecoderMaverick(context,sourcePath), duration)
        } else {
            addSource(null, duration)
        }
    }

    /**
     *
     * Add a source with an expected duration. The expected duration is guaranteed.
     *
     *
     * In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.
     *
     *
     * This function differs from [.addSourcePath] by looping the source audio
     * until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    @Throws(SourceUnacceptableException::class)
    fun addLoopingSourcePath(sourcePath: String?, duration: Long) {
        if (sourcePath != null) {
            //FIXME
            //long sourceDuration = MediaHelper.getAudioDuration(sourcePath);
            val sourceDuration: Long = 0
            if (sourceDuration < duration) {
                //Only add a looper if necessary
                addSource(PipedAudioLooper(context,sourcePath, duration), duration)
            } else {
                addSourcePath(sourcePath, duration)
            }
        } else {
            addSource(null, duration)
        }
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        if (mSources.isEmpty()) {
            throw SourceUnacceptableException("No sources provided!")
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    override fun getSampleForChannel(channel: Int): Short {
        return if (mCurrentState == ConcatState.DATA) {
            (mVolumeModifier * mSourceBufferA[mPos + channel]).toShort()
        } else {
            0
        }
    }

    @Throws(SourceClosedException::class)
    override fun loadSamplesForTime(time: Long): Boolean {
        var isDone = false

        //Reset volume modifier.
        mVolumeModifier = 1f

        if (mCurrentState == ConcatState.TRANSITION) {
            var transitionUs = mTransitionUs
            //For pre-first-source and last source, make the transition half as long.
            if (time <= mTransitionUs / 2 || mSources.isEmpty()) {
                transitionUs /= 2
            }

            val transitionComplete = time > mTransitionStart + transitionUs
            if (transitionComplete) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "loadSamplesForTime transition complete!")

                //Clear out the current source.
                if (mSource != null) {
                    mSource!!.close()
                    mSource = null
                }

                //Reset these stats so fetchSourceBuffer loop will trigger.
                mPos = 0
                mSize = 0
                mHasMoreBuffers = true

                //Assume DATA state.
                mCurrentState = ConcatState.DATA

                //Get a (valid) source or get to DONE state.
                while (mSource == null && !isDone) {
                    //If sources are all gone, this component is done.
                    if (mSources.isEmpty()) {
                        isDone = true
                        mCurrentState = ConcatState.DONE
                    } else {
                        mSourceStart = mSourceStart + mSourceExpectedDuration + transitionUs

                        mSource = nextSource
                        mSourceExpectedDuration = mSourceExpectedDurations.remove()
                    }
                }

                if (!isDone && mSourceStart > time) {
                    mCurrentState = ConcatState.BEFORE_SOURCE
                }
            }
        }

        if (mCurrentState == ConcatState.BEFORE_SOURCE && time >= mSourceStart) {
            mCurrentState = ConcatState.DATA
        }

        if (mCurrentState == ConcatState.DATA) {
            mPos += mChannelCount

            val sourceEnd = mSourceStart + mSourceExpectedDuration
            val sourceRemainingDuration = sourceEnd - time

            if (sourceRemainingDuration < mFadeOutUs) {
                mVolumeModifier = sourceRemainingDuration / mFadeOutUs.toFloat()
            }

            while (mHasMoreBuffers && mPos >= mSize) {
                fetchSourceBuffer()
            }
            val isWithinExpectedTime = mSourceExpectedDuration == 0L || time <= sourceEnd

            if (!mHasMoreBuffers || !isWithinExpectedTime) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "loadSamplesForTime starting transition")

                mCurrentState = ConcatState.TRANSITION

                //Clear out the current source.
                mSource!!.close()
                mSource = null

                //If no particular duration was expected, start transition now. Otherwise use precise time.
                if (mSourceExpectedDuration == 0L) {
                    mTransitionStart = time
                } else {
                    mTransitionStart = sourceEnd
                }
            }
        }

        return !isDone
    }

    @Throws(SourceClosedException::class)
    private fun fetchSourceBuffer() {
        mHasMoreBuffers = false

        if (mSource!!.isDone) {
            return
        }

        //buffer of bytes
        val buffer = mSource!!.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        mPos = 0
        mSize = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mSourceBufferA, mPos, mSize)

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(buffer)

        mHasMoreBuffers = true
    }

    override fun close() {
        super.close()
        if (mSource != null) {
            mSource!!.close()
            mSource = null
        }
    }

    companion object {
        private val TAG = "PipedAudioConcatenator"
    }
}
/**
 *
 * Add a source without an expected duration. The audio stream will be used in its entirety.
 *
 * @param sourcePath source audio path.
 */

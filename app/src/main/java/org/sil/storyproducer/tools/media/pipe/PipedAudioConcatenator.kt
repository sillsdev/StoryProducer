package org.sil.storyproducer.tools.media.pipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import org.sil.storyproducer.tools.file.getStoryUri

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

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

    override val componentName: String = TAG
    private var mCurrentState = ConcatState.TRANSITION //start in transition

    private val catSources = LinkedList<PipedMediaByteBufferSource>()
    private val catExpectedDurations = LinkedList<Long>()

    private var mFadeOutUs: Long = 0
    private val fadeOutSamples: Int get() {return (mFadeOutUs * mSampleRate / 1000000L).toInt()}


    private var mSourceExpectedDuration: Long = 0 //current source expected duration (us)

    private var mTransitionStart: Long = 0 //timestamp (us) of current transition start
    private var mSourceStart: Long = 0 //timestamp (us) of current source start (i.e. after prior transition)

    private var mVolumeModifier: Float = 0.toFloat()

    private val mInfo = MediaCodec.BufferInfo()

    private var mOutputFormat: MediaFormat? = null

    private//If we encounter an error, just let this source be passed over.
    val nextSource: PipedMediaByteBufferSource?
        get() {
            if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource starting")

            var nextSource: PipedMediaByteBufferSource? = null

            if (!catSources.isEmpty()) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource source found")

                nextSource = catSources.remove()
                if (nextSource == null) {
                    return null
                }

                try {
                    nextSource.setup()

                    nextSource = PipedAudioResampler.correctSampling(nextSource, mSampleRate, mChannelCount)
                    nextSource.setup()

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

    fun numOfSources(): Int {return catSources.size}

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
        if(source!=null) catSources.add(source)
        catExpectedDurations.add(duration)
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
            val sourceDuration: Long = MediaHelper.getAudioDuration(context,getStoryUri(sourcePath))
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

        if (catSources.isEmpty()) {
            throw SourceUnacceptableException("No sources provided!")
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    private fun zeroSourceBuffer(timeUntil: Long){
        srcPos = 0
        srcEnd = min(srcBuffer.size,((timeUntil - mSeekTime) * mSampleRate / 1000000L).toInt())
        for(index in 0 .. srcEnd){
            srcBuffer[index] = 0
        }
        srcHasBuffer = true
    }

    @Throws(SourceClosedException::class)
    override fun loadSamples(): Boolean {
        var isDone = false

        if (mCurrentState == ConcatState.TRANSITION) {
            var transitionUs = mTransitionUs
            //For pre-first-source and last source, make the transition half as long.
            if (mSeekTime <= mTransitionUs / 2 || catSources.isEmpty()) {
                transitionUs /= 2
            }


            val transitionComplete = mSeekTime >= mTransitionStart + transitionUs
            if (!transitionComplete){
                //fill the buffer with blank data of a size until the transition is complete or
                //the buffer is not large enough.
                zeroSourceBuffer(mTransitionStart + transitionUs)
                return true
            } else {
                //we are ready for some new data.  Load it up!
                if (MediaHelper.VERBOSE) Log.v(TAG, "loadSamples transition complete!")

                //Clear out the current source.
                if (mSource != null) {
                    mSource!!.close()
                    mSource = null
                }

                //Reset these stats so fetchSourceBuffer loop will trigger.
                srcPos = 0
                srcEnd = 0
                srcHasBuffer = true

                //Assume DATA state.
                mCurrentState = ConcatState.DATA

                //Get a (valid) source or get to DONE state.
                while (mSource == null && !isDone) {
                    //If sources are all gone, this component is done.
                    if (catSources.isEmpty()) {
                        isDone = true
                        mCurrentState = ConcatState.DONE
                    } else {
                        mSourceStart = mSourceStart + mSourceExpectedDuration + transitionUs

                        mSource = nextSource
                        mSourceExpectedDuration = catExpectedDurations.remove()
                    }
                }

                if (!isDone && mSourceStart > mSeekTime) {
                    mCurrentState = ConcatState.BEFORE_SOURCE
                }
            }
        }

        if(mSeekTime < mSourceStart){
            //Before Start
            zeroSourceBuffer(mSourceStart)
            return true
        }

        if (mCurrentState == ConcatState.BEFORE_SOURCE) {
            mCurrentState = ConcatState.DATA
        }

        if (mCurrentState == ConcatState.DATA) {
            if (srcHasBuffer && srcPos >= srcEnd) {
                fetchSourceBuffer()
            }

            //Only need to modify fadeout samples.
            val sourceEnd = mSourceStart + mSourceExpectedDuration
            val fadeOutStartTime = sourceEnd - mFadeOutUs
            val fadeOutEndPos = max(srcPos,(sourceEnd * mSampleRate / 1000000L).toInt())
            val fadeOutPos = max(srcPos,srcPos + ((fadeOutStartTime - mSeekTime) * mSampleRate / 1000000L).toInt())
            val fadeOutMult : Float = (1.0/fadeOutSamples).toFloat()

            for (index in fadeOutPos .. srcEnd){
                srcBuffer[index] = (srcBuffer[index] * (fadeOutEndPos - index)*fadeOutMult).toShort()
            }

            val isWithinExpectedTime = mSourceExpectedDuration == 0L || mSeekTime <= sourceEnd
            if (!srcHasBuffer || !isWithinExpectedTime) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "loadSamples starting transition")

                mCurrentState = ConcatState.TRANSITION

                //Clear out the current source.
                mSource!!.close()
                mSource = null

                //If no particular duration was expected, start transition now. Otherwise use precise time.
                if (mSourceExpectedDuration == 0L) {
                    mTransitionStart = mSeekTime
                } else {
                    mTransitionStart = sourceEnd
                }
            }
        }

        return !isDone
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

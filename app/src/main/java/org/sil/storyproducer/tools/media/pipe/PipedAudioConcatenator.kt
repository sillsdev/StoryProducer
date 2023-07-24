package org.sil.storyproducer.tools.media.pipe

import android.content.Context
import android.media.MediaFormat
import android.util.Log
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.MediaHelper
import java.io.IOException
import java.util.*
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
 * @param mTransitionUs length of audio transitions (dead space between audio sources) in microseconds.
 * @param sampleRate desired sample rate.
 * @param channelCount desired channel count.
 */
(private val context: Context, private val mTransitionUs: Long //duration of the audio transition
 , sampleRate: Int, channelCount: Int) : PipedAudioShortManipulator(), PipedMediaByteBufferDest {

    override val componentName: String = TAG
    private var mCurrentState = ConcatState.TRANSITION //start in transition

    private val catSources = LinkedList<PipedMediaByteBufferSource?>()
    private val catExpectedDurations = LinkedList<Long>()
    private val catVolume = LinkedList<Float>()

    //default to 20ms - get most of the finger press noise.
    private var mFadeInUs: Long = 50000
    private var mFadeOutUs: Long = 50000
    private val fadeInSamples: Int get() {return (mFadeInUs * mSampleRate / 1000000.0).toInt()}
    private val fadeOutSamples: Int get() {return (mFadeOutUs * mSampleRate / 1000000.0).toInt()}


    private var mSourceExpectedDuration: Long = 0 //current source expected duration (us)
    private var mSourceVolume: Float = 1.0f //current source volume

    private var mTransitionStart: Long = 0 //timestamp (us) of current transition start
    private var mSourceStart: Long = 0 //timestamp (us) of current source start (i.e. after prior transition)

    private var mOutputFormat: MediaFormat? = null

    private//If we encounter an error, just let this source be passed over.
    val nextSource: PipedMediaByteBufferSource?
        get() {
            if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource starting")

            var ns: PipedMediaByteBufferSource? = null

            if (!catSources.isEmpty()) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "getNextSource source found")

                ns = catSources.remove()
                if (ns == null) {
                    return null
                }

                try {
                    ns.setup()

                    ns = PipedAudioResampler.correctSampling(ns, mSampleRate, mChannelCount)
                    ns.setup()

                    validateSource(ns)
                } catch (e: IOException) {
                    Log.e(TAG, "Silencing failed source setup.", e)
                    ns = null
                } catch (e: SourceUnacceptableException) {
                    Log.e(TAG, "Silencing failed source setup.", e)
                    ns = null
                }

            }

            return ns
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

    fun anyNonNull(): Boolean {
        for (s in catSources)
            if (s != null) return true
        return false
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
    fun addSource(source: PipedMediaByteBufferSource?, duration: Long, volume: Float = 1.0f) {
        //even if it is null, add it.  If it is null, then we just add blank data.
        catSources.add(source)
        catExpectedDurations.add(duration)
        catVolume.add(volume)
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
    fun addSourcePath(sourcePath: String?, duration: Long = 0, volume: Float = 1.0f) {
        if (sourcePath != null) {
            addSource(PipedAudioDecoderMaverick(context,sourcePath), duration, volume)
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
    fun addLoopingSourcePath(sourcePath: String?, duration: Long, volume: Float = 1.0f) {
        if (sourcePath != null) {
            val sourceDuration: Long = MediaHelper.getAudioDuration(context,getStoryUri(sourcePath)!!)
            if (sourceDuration < duration) {
                //Only add a looper if necessary
                addSource(PipedAudioLooper(context,sourcePath, duration), duration, volume)
            } else {
                addSourcePath(sourcePath, duration, volume)
            }
        } else {
            addSource(null, duration, volume)
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
        srcEnd = min(srcBuffer.size,((timeUntil - mSeekTime) * mSampleRate / 1000000.0).toInt()+1)
        if(srcEnd < 1) srcEnd = 1
        srcBuffer.fill(0,0,srcEnd-1)
        srcHasBuffer = true
    }

    @Throws(SourceClosedException::class)
    override fun loadSamples(): Boolean {
        var isDone = false

        if (mCurrentState == ConcatState.TRANSITION) {
            var transitionUs = mTransitionUs
            //For pre-first-source and last source, make the transition half as long.
            if (mSeekTime <= mTransitionUs / 2 + 10000 || catSources.isEmpty()) {
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
                    mSource?.close()
                    mSource = null
                }

                //Reset these stats so fetchSourceBuffer loop will trigger.
                srcPos = 0
                srcEnd = 0
                srcHasBuffer = true

                //Assume DATA state.
                mCurrentState = ConcatState.DATA

                //Get a (valid) source or get to DONE state.
                //If sources are all gone, this component is done.
                if (catSources.isEmpty()) {
                    isDone = true
                    mCurrentState = ConcatState.DONE
                } else {
                    mSourceStart += mSourceExpectedDuration + transitionUs

                    mSource = nextSource
                    mSourceExpectedDuration = catExpectedDurations.remove()
                    mSourceVolume = catVolume.remove()
                    //If no particular duration was expected, start transition now. Otherwise use precise time.
                    if (mSourceExpectedDuration == 0L) {
                        mTransitionStart = mSeekTime
                    } else {
                        mTransitionStart = mSourceStart + mSourceExpectedDuration
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

            if(mSource == null){
                //This is a blank data source (silent, no data).
                zeroSourceBuffer(mSourceStart + mSourceExpectedDuration)
                if(mSeekTime > mSourceStart + mSourceExpectedDuration) {
                    mCurrentState = ConcatState.TRANSITION
                }
                return true
            }
            val sourceEnd = mSourceStart + mSourceExpectedDuration


            if (srcHasBuffer && srcPos >= srcEnd) {
                fetchSourceBuffer()
            }


            //Do a controlled fade out/fade in.
            val fadeInEndTime = mSourceStart + mFadeInUs
            val fadeInSamplesAtPos = ((mSeekTime - mSourceStart) *  mSampleRate / 1000000.0).toInt()
            val fadeInEnd = min(srcEnd, srcPos + ((fadeInEndTime - mSeekTime) * mSampleRate / 1000000.0).toInt())
            val fadeInMult: Float = (1.0 / fadeInSamples).toFloat()
            for (index in srcPos until fadeInEnd) {
                srcBuffer[index] = (srcBuffer[index] * (fadeInSamplesAtPos + index) * fadeInMult).toInt().toShort()
            }

            val fadeOutStartTime = sourceEnd - mFadeOutUs
            val fadeOutSamplesToEnd = (sourceEnd *  mSampleRate / 1000000.0 - mAbsoluteSampleIndex).toInt()
            val fadeOutPos = max(srcPos, srcPos + ((fadeOutStartTime - mSeekTime) * mSampleRate / 1000000.0).toInt())
            val fadeOutMult: Float = (1.0 / fadeOutSamples).toFloat()
            for (index in fadeOutPos until srcEnd) {
                srcBuffer[index] = (srcBuffer[index] * (fadeOutSamplesToEnd - index) * fadeOutMult).toInt().toShort()
            }


            val isWithinExpectedTime = mSourceExpectedDuration == 0L || mSeekTime <= sourceEnd
            if (!srcHasBuffer || !isWithinExpectedTime) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "loadSamples starting transition")

                mCurrentState = ConcatState.TRANSITION

                //Clear out the current source.
                mSource?.close()
                mSource = null
            }
        }

        return !isDone
    }

    @Throws(SourceClosedException::class)
    override fun fetchSourceBuffer() {
        if (mSource!!.isDone) {
            srcHasBuffer = false
            return
        }

        //buffer of bytes
        val buffer = mSource!!.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        srcPos = 0
        srcEnd = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(srcBuffer, srcPos, srcEnd)

        if(mSourceVolume != 1.0f)
            srcBuffer.sliceArray(srcPos..srcEnd).forEachIndexed {
                index, sh -> srcBuffer[index] = (sh*mSourceVolume).toInt().toShort() }

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(buffer)

        srcHasBuffer = true
    }


    companion object {
        private val TAG = "PipedAudioConcatenator"
    }
}

package org.sil.storyproducer.tools.media.pipe

import android.media.MediaFormat
import android.util.Log
import org.sil.storyproducer.tools.media.MediaHelper
import java.io.IOException
import kotlin.math.floor
import kotlin.math.max

/**
 *
 * This media pipeline component resamples (converts sample rate of) raw audio using linear interpolation.
 *
 * This component also optionally changes the channel count and/or volume of the raw audio stream.
 */
class PipedAudioResampler
/**
 * Create resampler changing channel count from the source channel count to the specified channel count.
 * @param sampleRate sample rate of the new, resampled audio stream.
 * @param channelCount number of channels in the new, resampled audio stream.
 */
@JvmOverloads constructor(sampleRate: Int, channelCount: Int = 0) : PipedAudioShortManipulator(), PipedMediaByteBufferDest {
    override val componentName: String
        get() = TAG

    private var mVolumeModifier = 1f

    private var mOutputFormat: MediaFormat? = null

    private var orgFormat: MediaFormat? = null
    protected val orgBuffer = ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2) //short = 2 bytes
    protected var orgPos: Int = 0
    protected var orgEnd: Int = 0
    private var orgSampleRate: Int = 0
    private var orgCumBufferEnd: Int = 0
    private var orgChannelCount: Int = 0
    private val orgBufferEndTime: Long get() {
        if(orgSampleRate == 0) return 0
        return (orgCumBufferEnd * 1000000.0 / orgSampleRate / orgChannelCount).toLong()
    }
    private var orgBufferStartTime: Long = 0

    init {
        mSampleRate = sampleRate
        mChannelCount = channelCount
    }

    override fun getOutputFormat(): MediaFormat? {
        return mOutputFormat
    }

    /**
     * Modify all samples by multiplying applying a constant (multiplication).
     * @param volumeModifier constant to multiply all samples by.
     */
    fun setVolumeModifier(volumeModifier: Float) {
        mVolumeModifier = volumeModifier
    }

    @Throws(SourceUnacceptableException::class)
    override fun addSource(src: PipedMediaByteBufferSource) {
        if (mSource != null) {
            throw SourceUnacceptableException("Audio source already added!")
        }
        mSource = src
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        if (mSource == null) {
            throw SourceUnacceptableException("Source cannot be null!")
        }

        mSource!!.setup()

        validateSource(mSource!!, 0, 0)

        orgFormat = mSource!!.outputFormat
        orgSampleRate = orgFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        orgChannelCount = orgFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        if (mChannelCount == 0) {
            mChannelCount = orgChannelCount
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    @Throws(SourceClosedException::class)
    override fun loadSamples(): Boolean {
        //grab the last sample for interpolation before the first sample of the new data
        val last = ShortArray(orgChannelCount)
        var ch = 0
        if(orgEnd > 0) { // don't populate if it is the first run through, where orgEnd == 0.
            for (index in orgEnd - orgChannelCount until orgEnd) {
                last[ch++] = orgBuffer[index]
            }
        }
        //grab the new buffer into org buffer
        //prepend the "last" samples.
        fetchSourceBufferWithPrepend(last)
        //There is no new data.  Return.
        if (!srcHasBuffer) {
            mSource!!.close()
            mSource = null
            return false
        }
        //Or, there is data.
        //Find out how many interpolated samples we can actually make based on the available time.
        //add one to capture the last element.
        srcPos = 0
        srcEnd = max(0,floor(((orgBufferEndTime - mSeekTime) * mSampleRate / 1000000.0 + 1).toFloat()).toInt() * mChannelCount)

        //convert all the samples
        val relStartTime = mSeekTime - orgBufferStartTime
        val srMult = (1000000.0 / mSampleRate).toFloat()
        val ssrMult = (orgSampleRate / 1000000.0).toFloat()
        if (mChannelCount == 2 && orgChannelCount == 2) {
            for (i in 0 until srcEnd) {
                val cChannel = i % 2
                val fSample = (relStartTime + (i / 2) * srMult) * ssrMult
                val si = floor(fSample).toInt() * 2 + cChannel  // first index
                val sw = fSample - floor(fSample) //weight of second term
                srcBuffer[i] = (mVolumeModifier * (orgBuffer[si] * (1 - sw) + orgBuffer[si + 2] * sw)).toInt().toShort()
            }
        } else if (mChannelCount == 1 && orgChannelCount == 2) {
            for (i in 0 until srcEnd) {
                val fSample = (relStartTime + i * srMult) * ssrMult
                val si = floor(fSample).toInt() * 2  // first index
                val sw = fSample - floor(fSample) //weight of second term
                srcBuffer[i] = (mVolumeModifier *
                        ((orgBuffer[si] + orgBuffer[si + 1]) * (1 - sw) +
                                (orgBuffer[si + 2] + orgBuffer[si + 3]) * sw) * 0.5).toInt().toShort()
            }
        } else if (mChannelCount == 2 && orgChannelCount == 1) {
            for (i in 0 until srcEnd step 2) {
                val fSample = (relStartTime + (i / 2) * srMult) * ssrMult
                val si = floor(fSample).toInt() // first index
                val sw = fSample - si //weight of second term
                srcBuffer[i] = (mVolumeModifier * (orgBuffer[si] * (1 - sw) + orgBuffer[si + 1] * sw)).toInt().toShort()
                srcBuffer[i + 1] = srcBuffer[i]
            }
        } else {//1,1
            for (i in 0 until srcEnd) {
                val fSample = (relStartTime + i * srMult) * ssrMult
                val si = floor(fSample).toInt() // first index
                val sw = fSample - si //weight of second term
                srcBuffer[i] = (mVolumeModifier * (orgBuffer[si] * (1 - sw) + orgBuffer[si + 1] * sw)).toInt().toShort()
            }
        }
        return true
    }

    @Throws(SourceClosedException::class)
    fun fetchSourceBufferWithPrepend(prependSamples : ShortArray) {
        if (mSource!!.isDone) {
            srcHasBuffer = false
            return
        }

        //buffer of bytes
        val buffer = mSource!!.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        if (MediaHelper.VERBOSE) {
            Log.v(TAG, "Received " + (if (buffer.isDirect) "direct" else "non-direct")
                    + " buffer of size " + mInfo.size
                    + " with" + (if (buffer.hasArray()) "" else "out") + " array")
        }

        for (i in 0 until prependSamples.size)
            orgBuffer[i] = prependSamples[i]

        orgBufferStartTime = orgBufferEndTime
        orgPos = prependSamples.size
        orgEnd = sBuffer.remaining() + prependSamples.size
        orgCumBufferEnd += sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(orgBuffer, orgPos, sBuffer.remaining())

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(buffer)


        srcHasBuffer = true
    }

    companion object {
        private val TAG = "PipedAudioResampler"

        /**
         *
         * Gets a [PipedMediaByteBufferSource] with the correct sampling.
         *
         * This method works by checking the output format of an already setup source
         * against the desired sampling parameters. If the source already matches, it is merely
         * returned. Otherwise, a resampler is inserted into the pipeline.
         */
        @Throws(IOException::class, SourceUnacceptableException::class)
        fun correctSampling(src: PipedMediaByteBufferSource, sampleRate: Int, channelCount: Int): PipedMediaByteBufferSource {
            val format = src.outputFormat

            val isSamplingCorrect = (sampleRate == 0 || format.getInteger(MediaFormat.KEY_SAMPLE_RATE) == sampleRate) && (channelCount == 0 || format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == channelCount)

            if (!isSamplingCorrect) {
                val resampler = PipedAudioResampler(sampleRate, channelCount)
                resampler.addSource(src)
                return resampler
            } else {
                return src
            }
        }
    }
}

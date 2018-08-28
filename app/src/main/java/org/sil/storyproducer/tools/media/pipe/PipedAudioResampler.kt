package org.sil.storyproducer.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer

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

    private var mSource: PipedMediaByteBufferSource? = null
    private var mSourceFormat: MediaFormat? = null
    private var mOutputFormat: MediaFormat? = null

    private var mSourceSampleRate: Int = 0
    private var mSourceUsPerSample: Float = 0.toFloat()
    private var mSourceChannelCount: Int = 0

    //variables for our sliding window of source data
    private var mLeftSamples: ShortArray? = null
    private var mRightSamples: ShortArray? = null
    private var mLeftSeekTime: Long = 0
    private var mRightSeekTime: Long = 0
    //N.B. Starting at -1 ensures starting with right and left from source.
    private var mAbsoluteRightSampleIndex = -1

    private val mSourceBufferA = ShortArray(SOURCE_BUFFER_CAPACITY / 2)
    private var mHasBuffer = false

    private var mSeekTime: Long = 0

    private var mSourcePos: Int = 0

    private val mInfo = MediaCodec.BufferInfo()

    private var mSourceSize: Int = 0

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

        mSourceFormat = mSource!!.outputFormat
        mSourceSampleRate = mSourceFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        mSourceUsPerSample = 1000000f / mSourceSampleRate //1000000 us/s
        mSourceChannelCount = mSourceFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        if (mChannelCount == 0) {
            mChannelCount = mSourceChannelCount
        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        //Initialize sample data to 0.
        mLeftSamples = ShortArray(mSourceChannelCount)
        mRightSamples = ShortArray(mSourceChannelCount)
        for (i in 0 until mSourceChannelCount) {
            mLeftSamples!![i] = 0
            mRightSamples!![i] = 0
        }

        //Get the first input buffer.
        try {
            fetchSourceBuffer()
        } catch (e: SourceClosedException) {
            //This case should not happen.
            throw SourceUnacceptableException("First fetchSourceBuffer failed! Strange", e)
        }

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    /**
     *
     * Get a sample for a given time and channel from the source media pipeline component using linear interpolation.
     *
     *
     * Note: Sequential calls to this function must provide strictly increasing times.
     * @param channel which channel to get sample for current time
     * @return sample for current time and channel
     */
    override fun getSampleForChannel(channel: Int): Short {
        val left: Short
        val right: Short

        if (mChannelCount == mSourceChannelCount) {
            left = mLeftSamples!![channel]
            right = mRightSamples!![channel]
        } else if (mChannelCount == 1/* && mSourceChannelCount == 2*/) {
            left = (mLeftSamples!![0] / 2 + mLeftSamples!![1] / 2).toShort()
            right = (mRightSamples!![0] / 2 + mRightSamples!![1] / 2).toShort()
        } else { //mChannelCount == 2 && mSourceChannelCount == 1
            left = mLeftSamples!![0]
            right = mRightSamples!![0]
        }

        if (mSeekTime == mLeftSeekTime) {
            return left
        } else {
            //Perform linear interpolation.
            val rightWeight = (mSeekTime - mLeftSeekTime) / mSourceUsPerSample
            val leftWeight = 1 - rightWeight

            val interpolatedSample = ((leftWeight * left).toShort() + (rightWeight * right).toShort()).toShort()

            return (mVolumeModifier * interpolatedSample).toShort()
        }
    }

    @Throws(SourceClosedException::class)
    override fun loadSamplesForTime(time: Long): Boolean {
        var moreInput = true

        mSeekTime = time

        //Move window forward until left and right appropriately surround the given time.
        //N.B. Left is inclusive; right is exclusive.
        while (mSeekTime >= mRightSeekTime) {
            moreInput = advanceWindow()
        }

        return moreInput
    }

    /**
     * Slide the window forward by one sample.
     */
    @Throws(SourceClosedException::class)
    private fun advanceWindow(): Boolean {
        var isDone = false

        //Set left's values to be right's current values.
        val temp = mLeftSamples
        mLeftSamples = mRightSamples
        mRightSamples = temp

        mLeftSeekTime = mRightSeekTime

        //Update right's time.
        mAbsoluteRightSampleIndex++
        mRightSeekTime = getTimeFromIndex(mSourceSampleRate.toLong(), mAbsoluteRightSampleIndex)

        while (mHasBuffer && mSourcePos >= mSourceSize) {
            fetchSourceBuffer()
        }
        //If we hit the end of input, use 0 as the last right sample value.
        if (!mHasBuffer) {
            isDone = true

            mSource!!.close()
            mSource = null

            for (i in 0 until mSourceChannelCount) {
                mRightSamples!![i] = 0
            }
        } else {
            //Get right's values from the input buffer.
            for (i in 0 until mSourceChannelCount) {
                try {
                    mRightSamples!![i] = mSourceBufferA[mSourcePos++]
                } catch (e: ArrayIndexOutOfBoundsException) {
                    Log.e(TAG, "Tried to read beyond buffer", e)
                }

            }
        }

        return !isDone
    }

    @Throws(SourceClosedException::class)
    private fun fetchSourceBuffer() {
        mHasBuffer = false

        if (mSource!!.isDone) {
            return
        }

        //buffer of bytes
        val tempSourceBuffer = mSource!!.getBuffer(mInfo)

        if (MediaHelper.VERBOSE) {
            Log.v(TAG, "Received " + (if (tempSourceBuffer.isDirect) "direct" else "non-direct")
                    + " buffer of size " + mInfo.size
                    + " with" + (if (tempSourceBuffer.hasArray()) "" else "out") + " array")
        }

        //buffer of shorts (16-bit samples)
        val tempShortBuffer = MediaHelper.getShortBuffer(tempSourceBuffer)

        mSourcePos = 0
        mSourceSize = tempShortBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        tempShortBuffer.get(mSourceBufferA, mSourcePos, mSourceSize)

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(tempSourceBuffer)

        mHasBuffer = true
    }

    override fun close() {
        super.close()
        if (mSource != null) {
            mSource!!.close()
            mSource = null
        }
    }

    companion object {
        private val TAG = "PipedAudioResampler"

        private val SOURCE_BUFFER_CAPACITY = MediaHelper.MAX_INPUT_BUFFER_SIZE

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
/**
 * Create resampler, maintaining channel count from source audio stream.
 * @param sampleRate sample rate of the new, resampled audio stream.
 */

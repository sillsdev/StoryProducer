package org.sil.storyproducer.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.ArrayList

/**
 *
 * This media pipeline component mixes raw audio streams together.
 *
 * This component also optionally changes the volume of the raw audio stream.
 */
class PipedAudioMixer : PipedAudioShortManipulator(), PipedMediaByteBufferDest {
    override val componentName: String
        get() = TAG

    private var mOutputFormat: MediaFormat? = null

    private val mSources = ArrayList<PipedMediaByteBufferSource>()
    private val mSourceVolumeModifiers = ArrayList<Float>()

    private val mSourceBufferAs = ArrayList<ShortArray>()
    private val mSourcePos = ArrayList<Int>()
    private val mSourceSizes = ArrayList<Int>()

    private var mCurrentSample: ShortArray? = null

    private val mInfo = MediaCodec.BufferInfo()

    override fun getOutputFormat(): MediaFormat? {
        return mOutputFormat
    }

    @Throws(SourceUnacceptableException::class)
    override fun addSource(src: PipedMediaByteBufferSource) {
        addSource(src, 1f)
    }

    /**
     * Specify a predecessor of this component in the pipeline with a specified volume scaling factor.
     * @param src the preceding component of the pipeline.
     * @param volumeModifier volume scaling factor.
     * @throws SourceUnacceptableException if source is null.
     */
    @Throws(SourceUnacceptableException::class)
    fun addSource(src: PipedMediaByteBufferSource?, volumeModifier: Float) {
        if (src == null) {
            throw SourceUnacceptableException("Source cannot be null!")
        }

        mSources.add(src)
        mSourceVolumeModifiers.add(volumeModifier)
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        if (mSources.isEmpty()) {
            throw SourceUnacceptableException("No sources specified!")
        }

        for (i in mSources.indices) {
            val source = mSources[i]
            source.setup()
            validateSource(source, mChannelCount, mSampleRate)

            val format = source.outputFormat
            if (mChannelCount == 0) {
                mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (mSampleRate == 0) {
                mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }

            mSourceBufferAs.add(ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2))
            mSourcePos.add(0)
            mSourceSizes.add(0)
            try {
                fetchSourceBuffer(i)
            } catch (e: SourceClosedException) {
                //This case should not happen.
                throw SourceUnacceptableException("First fetchSourceBuffer failed! Strange", e)
            }

        }

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        mCurrentSample = ShortArray(mChannelCount)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    override fun getSampleForChannel(channel: Int): Short {
        return mCurrentSample!![channel]
    }

    @Throws(SourceClosedException::class)
    override fun loadSamplesForTime(time: Long): Boolean {
        for (i in 0 until mChannelCount) {
            mCurrentSample!![i] = 0
        }

        //Loop through all sources and add samples.
        var iSource = 0
        while (iSource < mSources.size) {
            var size = mSourceSizes[iSource]
            var pos = mSourcePos[iSource]
            var buffer: ShortArray? = mSourceBufferAs[iSource]
            val volumeModifier = mSourceVolumeModifiers[iSource]
            while (buffer != null && pos >= size) {
                fetchSourceBuffer(iSource)

                size = mSourceSizes[iSource]
                pos = mSourcePos[iSource]
                buffer = mSourceBufferAs[iSource]
            }
            if (buffer != null) {
                for (iChannel: Int in 0 until mChannelCount) {
                    mCurrentSample!![iChannel] = (mCurrentSample!![iChannel] + buffer[pos++] * volumeModifier).toShort()
                }
                mSourcePos[iSource] = pos
            } else {
                //Remove depleted sources from the lists.
                mSources.removeAt(iSource)
                mSourceBufferAs.removeAt(iSource)
                mSourcePos.removeAt(iSource)
                mSourceSizes.removeAt(iSource)

                //Decrement iSource so that former source iSource + 1 is not skipped.
                iSource--
            }
            iSource++
        }

        //If sources are all gone, this component is done.
        return !mSources.isEmpty()
    }

    @Throws(SourceClosedException::class)
    private fun fetchSourceBuffer(sourceIndex: Int) {
        val source = mSources[sourceIndex]
        if (source.isDone) {
            source.close()
            mSourceBufferAs.set(sourceIndex, ShortArray(0))
            return
        }

        //buffer of bytes
        val buffer = source.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        val pos = 0
        val size = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mSourceBufferAs[sourceIndex], pos, size)
        mSourcePos[sourceIndex] = pos
        mSourceSizes[sourceIndex] = size

        //Release buffer since data was copied.
        source.releaseBuffer(buffer)
    }

    override fun close() {
        super.close()
        while (!mSources.isEmpty()) {
            val source = mSources.removeAt(0)
            source.close()
        }
    }

    companion object {
        private val TAG = "PipedAudioMixer"
    }
}
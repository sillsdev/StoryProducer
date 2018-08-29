package org.sil.storyproducer.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.util.ArrayList
import kotlin.math.min

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

    private val mixSources = ArrayList<PipedMediaByteBufferSource>()
    private val mixVolumeModifiers = ArrayList<Float>()
    private val mixBuffers = ArrayList<ShortArray?>()
    private val mixPoss = ArrayList<Int>()
    private val mixEnds = ArrayList<Int>()

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

        mixSources.add(src)
        mixVolumeModifiers.add(volumeModifier)
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        if (mixSources.isEmpty()) {
            throw SourceUnacceptableException("No sources specified!")
        }

        for (i in mixSources.indices) {
            val source = mixSources[i]
            source.setup()
            validateSource(source, mChannelCount, mSampleRate)

            val format = source.outputFormat
            if (mChannelCount == 0) {
                mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (mSampleRate == 0) {
                mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }

            mixBuffers.add(ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2))
            mixPoss.add(0)
            mixEnds.add(0)
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

    @Throws(SourceClosedException::class)
    override fun loadSamples(): Boolean {
        for (i in 0 until mChannelCount) {
            mCurrentSample!![i] = 0
        }

        //Loop through all sources and fetch buffers if we need to
        var iSource = 0
        var allLength = 1000000
        while (iSource < mixSources.size) {
            if (mixPoss[iSource] >= mixEnds[iSource] && mixBuffers[iSource] != null) {
                fetchSourceBuffer(iSource)
            }
            //The source is depleted!
            if (mixBuffers[iSource] == null) {
                //Remove depleted sources from the lists.
                mixSources.removeAt(iSource)
                mixBuffers.removeAt(iSource)
                mixPoss.removeAt(iSource)
                mixEnds.removeAt(iSource)
                continue
            }
            allLength = min(allLength,mixEnds[iSource] - mixPoss[iSource])
            iSource++
        }

        if(mixSources.isEmpty()) return false

        //prep srcBuffer with the mixing of all channels available.

        //do the first channel
        //grab the buffer
        var pos = mixPoss[0]
        var buffer: ShortArray = mixBuffers[0]!!
        var volumeModifier = mixVolumeModifiers[0]

        //setup the data as a source.  Copy the buffer
        for (index in 0 until allLength){
            srcBuffer[index] = (buffer[pos++] * volumeModifier).toShort()
        }
        mixPoss[0] = pos

        //do all other channels
        iSource = 1
        while (iSource < mixSources.size) {
            //grab the buffer
            pos = mixPoss[iSource]
            buffer = mixBuffers[iSource]!!
            volumeModifier = mixVolumeModifiers[iSource]

            //setup the data as a source.  Copy the buffer
            for (index in 0 until allLength){
                srcBuffer[index] = (srcBuffer[index] + buffer[pos++] * volumeModifier).toShort()
            }
            mixPoss[iSource] = pos
            iSource++
        }

        srcPos = 0
        srcEnd = allLength

        return true
    }

    @Throws(SourceClosedException::class)
    fun fetchSourceBuffer(sourceIndex: Int) {
        val source = mixSources[sourceIndex]
        if (source.isDone) {
            source.close()
            mixBuffers[sourceIndex] = null
            return
        }

        //buffer of bytes
        val buffer = source.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        if (MediaHelper.VERBOSE) {
            Log.v(TAG, "Received " + (if (buffer.isDirect) "direct" else "non-direct")
                    + " buffer of size " + mInfo.size
                    + " with" + (if (buffer.hasArray()) "" else "out") + " array")
        }

        val pos = 0
        val size = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mixBuffers[sourceIndex], pos, size)
        mixPoss[sourceIndex] = pos
        mixEnds[sourceIndex] = size

        //Release buffer since data was copied.
        source.releaseBuffer(buffer)
    }

    override fun close() {
        super.close()
        while (!mixSources.isEmpty()) {
            val source = mixSources.removeAt(0)
            source.close()
        }
    }

    companion object {
        private val TAG = "PipedAudioMixer"
    }
}
package org.tyndalebt.spadv.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.tyndalebt.spadv.tools.media.MediaHelper

import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.math.min

/**
 *
 * This abstract media pipeline component provides a base for audio components
 * which care about touching every output short.
 *
 *
 * The most important methods for a class overriding this class are [.loadSamples]
 * and [.getSampleForChannel]. [.loadSamples] will be called exactly
 * once, in order, for each time step according to [.mSampleRate]. After each of these calls,
 * [.getSampleForChannel] will be called exactly once, in order, for each channel
 * according to [.mChannelCount].
 *
 *
 * As a note on implementation, we are generally trying to use arrays when manipulating the shorts
 * rather than using buffers directly. We hypothesize that doing so gives us significant performance
 * gains on some physical devices.
 */
abstract class PipedAudioShortManipulator : PipedMediaByteBufferSource {

    protected abstract val componentName: String

    private var mThread: Thread? = null

    //Any caller of isDone needs to be immediately aware of changes to the mIsDone variable,
    //even in another thread.
    @Volatile
    private var mIsDone = false
    private var mNonvolatileIsDone = false

    //Although this is cross-thread, it isn't important for the input thread to immediately stop;
    //so no volatile keyword.
    protected var mComponentState: PipedMediaSource.State = PipedMediaSource.State.UNINITIALIZED
    private val mBufferQueue = ByteBufferQueue(BUFFER_COUNT)
    private val mShortBuffer = ShortArray(MAX_BUFFER_CAPACITY / 2) //short = 2 bytes

    protected var mSampleRate: Int = 0
    protected var mChannelCount: Int = 0
    protected var mAbsoluteSampleIndex = 0
    protected val mSeekTime: Long get() {
        if(mSampleRate == 0) return 0
        return (mAbsoluteSampleIndex * 1000000.0 / mSampleRate).toLong()
    }

    protected var mSource: PipedMediaByteBufferSource? = null
    protected val mInfo = MediaCodec.BufferInfo()
    protected var srcBuffer = ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2) //short = 2 bytes
    protected var srcPos: Int = 0
    protected var srcEnd: Int = 0
    protected val srcSamplesAvailable: Int get() {return srcEnd - srcPos}
    protected var srcHasBuffer = false

    override fun getMediaType(): MediaHelper.MediaType {
        return MediaHelper.MediaType.AUDIO
    }

    override fun isDone(): Boolean {
        return mIsDone && mBufferQueue.isEmpty || mComponentState == PipedMediaSource.State.CLOSED
    }

    override fun fillBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val myBuffer = mBufferQueue.getFilledBuffer(info)
        buffer.clear()
        buffer.put(myBuffer)
        mBufferQueue.releaseUsedBuffer(myBuffer)
    }

    override fun getBuffer(info: MediaCodec.BufferInfo): ByteBuffer? {
        return mBufferQueue.getFilledBuffer(info)
    }

    @Throws(InvalidBufferException::class)
    override fun releaseBuffer(buffer: ByteBuffer) {
        mBufferQueue.releaseUsedBuffer(buffer)
    }

    @Throws(SourceUnacceptableException::class)
    protected fun start() {
        if (mSampleRate == 0) {
            throw SourceUnacceptableException("$componentName: Sample rate not specified!")
        }
        if (mChannelCount == 0) {
            throw SourceUnacceptableException("$componentName: Channel count not specified!")
        }

        mThread = Thread(Runnable {
            try {
                spinInput()
            } catch (e: Exception) {
                Log.w(TAG, "spinInput stopped prematurely", e)
            }
        })
        mComponentState = PipedMediaSource.State.RUNNING
        mThread!!.start()
    }

    @Throws(SourceClosedException::class)
    private fun spinInput() {

        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput starting")

        while (mComponentState != PipedMediaSource.State.CLOSED && !mIsDone) {
            var durationNs: Long = 0
            val info = MediaCodec.BufferInfo()
            if (MediaHelper.DEBUG) {
                durationNs = -System.nanoTime()
            }
            //Prepare outBuffer
            val outBuffer = mBufferQueue.getEmptyBuffer(MediaHelper.TIMEOUT_USEC)
            if (outBuffer == null) {
                if (MediaHelper.VERBOSE)
                    Log.d(TAG, "$componentName.spinInput: empty buffer unavailable")
                continue
            }

            outBuffer.clear()
            info.set(0, 0, mSeekTime, 0)
            val outShortBuffer = MediaHelper.getShortBuffer(outBuffer)
            val osbLength = outShortBuffer.remaining()
            var osbPos = 0

            outShortBuffer.get(mShortBuffer, osbPos, osbLength)
            outShortBuffer.clear()

            if (srcSamplesAvailable <= 0) mNonvolatileIsDone = !loadSamples()

            while ((osbPos < osbLength) && !mNonvolatileIsDone) {
                //interleave channels
                //N.B. Always put all samples (of different channels) of the same time in the same buffer.
                val copyLength = min(osbLength - osbPos, srcSamplesAvailable)
                val transBuffer = ShortBuffer.wrap(srcBuffer,srcPos,copyLength)
                transBuffer.get(mShortBuffer,osbPos,copyLength)
                osbPos += copyLength
                srcPos += copyLength

                //Keep track of the current presentation time in the output audio stream.
                mAbsoluteSampleIndex += copyLength

                if (srcSamplesAvailable <= 0) mNonvolatileIsDone = !loadSamples()
            }

            info.size = osbPos * 2 //short = 2 bytes

            outShortBuffer.put(mShortBuffer, 0, osbLength)

            //just to be sure
            outBuffer.position(info.offset)
            outBuffer.limit(info.offset + info.size)

            if (mNonvolatileIsDone) {
                //Sync the volatile version of the isDone variable.
                mIsDone = true

                info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
            }

            mBufferQueue.sendFilledBuffer(outBuffer, info)

            if (MediaHelper.DEBUG) {
                durationNs += System.nanoTime()
                val sec = durationNs / 1E9
                Log.d(TAG, componentName + ".spinInput: return output buffer after "
                        + MediaHelper.getDecimal(sec) + " seconds: size " + info.size
                        + " for time " + info.presentationTimeUs)
            }
        }
        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput complete!")
    }

    /**
     *
     * Instruct the callee to prepare to provide samples for a given mSeekTime.
     * Only this abstract base class should call this function.
     *
     *
     * Note: Sequential calls to this function will provide strictly increasing times.
     * @return true if the component has more source input to process and false if [.spinInput] should finish
     */
    @Throws(SourceClosedException::class)
    protected abstract fun loadSamples(): Boolean

    /**
     * Validate the source as raw audio against specified channel count and sample rate.
     * @param source to be validated
     * @param channelCount required source channel count (or 0 for any channel count)
     * @param sampleRate required source sample rate (or 0 for any sample rate)
     * @throws SourceUnacceptableException if source is not raw audio or doesn't match specs
     */
    @Throws(SourceUnacceptableException::class)
    @JvmOverloads
    protected fun validateSource(source: PipedMediaByteBufferSource, channelCount: Int = mChannelCount, sampleRate: Int = mSampleRate) {
        val format = source.outputFormat

        if (source.mediaType != MediaHelper.MediaType.AUDIO) {
            throw SourceUnacceptableException("Source must be audio!")
        }

        if (format.getString(MediaFormat.KEY_MIME) != MediaHelper.MIMETYPE_RAW_AUDIO) {
            throw SourceUnacceptableException("Source audio must be a raw audio stream!")
        }

        val sourceChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (sourceChannelCount != 1 && sourceChannelCount != 2) {
            throw SourceUnacceptableException("Source audio is neither mono nor stereo!")
        } else if (channelCount != 0 && channelCount != sourceChannelCount) {
            throw SourceUnacceptableException("Source audio channel counts don't match!")
        }

        if (sampleRate != 0 && sampleRate != format.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw SourceUnacceptableException("Source audio sample rates don't match!")
        }
    }

    @Throws(SourceClosedException::class)
    open fun fetchSourceBuffer() {
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

        srcPos = 0
        srcEnd = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(srcBuffer, srcPos, srcEnd)

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(buffer)

        srcHasBuffer = true
    }

    override fun close() {
        //Force the spinInput thread to shutdown.
        mComponentState = PipedMediaSource.State.CLOSED
        if (mSource != null) {
            mSource!!.close()
            mSource = null
        }
        if (mThread != null) {
            try {
                mThread!!.join()
            } catch (e: InterruptedException) {
                Log.w(TAG, "$componentName: Failed to stop input thread!", e)
            }

            mThread = null
        }
    }

    companion object {
        private val TAG = "PipedAudioShortMan"

        val BUFFER_COUNT = 8

        private val MAX_BUFFER_CAPACITY = MediaHelper.MAX_INPUT_BUFFER_SIZE
    }
}

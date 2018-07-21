package org.sil.storyproducer.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.MediaHelper

import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 *
 * This abstract media pipeline component provides a base for audio components
 * which care about touching every output short.
 *
 *
 * The most important methods for a class overriding this class are [.loadSamplesForTime]
 * and [.getSampleForChannel]. [.loadSamplesForTime] will be called exactly
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
    private var mSeekTime: Long = 0

    private var mAbsoluteSampleIndex = 0

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
            } catch (e: SourceClosedException) {
                Log.w(TAG, "spinInput stopped prematurely", e)
            }
        })
        mComponentState = PipedMediaSource.State.RUNNING
        mThread!!.start()
    }

    @Throws(SourceClosedException::class)
    private fun spinInput() {
        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput starting")

        mNonvolatileIsDone = !loadSamplesForTime(mSeekTime)

        while (mComponentState != PipedMediaSource.State.CLOSED && !mIsDone) {
            var durationNs: Long = 0
            if (MediaHelper.DEBUG) {
                durationNs = -System.nanoTime()
            }
            val outBuffer = mBufferQueue.getEmptyBuffer(MediaHelper.TIMEOUT_USEC)

            if (outBuffer == null) {
                if (MediaHelper.VERBOSE)
                    Log.d(TAG, "$componentName.spinInput: empty buffer unavailable")
                continue
            }

            val info = MediaCodec.BufferInfo()

            //reset output buffer
            outBuffer.clear()

            //reset output buffer info
            info.set(0, 0, mSeekTime, 0)

            //prepare a ShortBuffer view of the output buffer
            val outShortBuffer = MediaHelper.getShortBuffer(outBuffer)

            val length = outShortBuffer.remaining()
            var pos = 0

            outShortBuffer.get(mShortBuffer, pos, length)
            outShortBuffer.clear()

            var iSample: Int
            val sampleRateMultiplier = 1000000L / mSampleRate
            iSample = 0
            while (iSample < length) {
                //interleave channels
                //N.B. Always put all samples (of different channels) of the same time in the same buffer.
                for (i in 0 until mChannelCount) {
                    mShortBuffer[pos++] = getSampleForChannel(i)
                }

                //Keep track of the current presentation time in the output audio stream.
                mAbsoluteSampleIndex++
                //get time from index
                mSeekTime = mAbsoluteSampleIndex * sampleRateMultiplier

                //Give warning about new time
                mNonvolatileIsDone = !loadSamplesForTime(mSeekTime)

                //Break out only in exception case of exhausting source.
                if (mNonvolatileIsDone) {
                    //Since iSample is used outside the loop, count this last iteration.
                    iSample += mChannelCount
                    break
                }
                iSample += mChannelCount
            }

            info.size = iSample * 2 //short = 2 bytes

            outShortBuffer.put(mShortBuffer, 0, length)

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
     * Get a sample for given a channel from the source media pipeline component using linear interpolation.
     *
     *
     * Note: No two calls to this function will elicit information for the same state.
     * @param channel
     * @return
     */
    protected abstract fun getSampleForChannel(channel: Int): Short

    /**
     *
     * Instruct the callee to prepare to provide samples for a given time.
     * Only this abstract base class should call this function.
     *
     *
     * Note: Sequential calls to this function will provide strictly increasing times.
     * @param time
     * @return true if the component has more source input to process and false if [.spinInput] should finish
     */
    @Throws(SourceClosedException::class)
    protected abstract fun loadSamplesForTime(time: Long): Boolean

    /**
     *
     * Get the sample time from the sample index given a sample rate.
     *
     *
     * Note: This method provides more accurate timestamps than simply keeping track
     * of the current timestamp and deltas.
     * @param sampleRate
     * @param index
     * @return timestamp associated with index (in microseconds)
     */
    protected fun getTimeFromIndex(sampleRate: Long, index: Int): Long {
        return index * 1000000L / sampleRate
    }

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

    override fun close() {
        //Force the spinInput thread to shutdown.
        mComponentState = PipedMediaSource.State.CLOSED
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

        private val BUFFER_COUNT = 4

        private val MAX_BUFFER_CAPACITY = MediaHelper.MAX_INPUT_BUFFER_SIZE
    }
}
/**
 * Validate the source as raw audio against channel count and sample rate of this component.
 * @param source to be validated
 * @throws SourceUnacceptableException if source is not raw audio or doesn't match specs
 */

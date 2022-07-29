package org.tyndalebt.spadv.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import org.tyndalebt.spadv.tools.media.MediaHelper
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*

/**
 *
 * This abstract media pipeline component provides a base for components which encode or decode
 * media streams. This class primarily encapsulates a [MediaCodec].
 *
 * Note: This class is spawns a child thread which keeps churning input while other calling code
 * pulls output.
 */
abstract class PipedMediaCodec : PipedMediaByteBufferSource {

    protected abstract val componentName: String

    internal var mThread: Thread? = null

    @Volatile
    protected var mComponentState: PipedMediaSource.State = PipedMediaSource.State.UNINITIALIZED

    protected var mCodec: MediaCodec? = null
    protected var outputBufferId: Int = 0
    private var mOutputFormat: MediaFormat? = null

    private val mBuffersBeforeFormat = LinkedList<MediaBuffer>()

    @Volatile
    private var mIsDone = false
    private var mPresentationTimeUsLast: Long = 0

    private val mInfo = MediaCodec.BufferInfo()

    override fun getOutputFormat(): MediaFormat {
        if (mOutputFormat == null) {
            try {
                pullBuffer(mInfo, true)
            } catch (e: SourceClosedException) {
                throw RuntimeException("format retrieval interrupted", e)
            }

            if (mOutputFormat == null) {
                throw RuntimeException("format was not retrieved from loop")
            }
        }
        return mOutputFormat!!
    }

    override fun isDone(): Boolean {
        return mIsDone
    }

    @Throws(SourceClosedException::class)
    override fun fillBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val outputBuffer = pullBuffer(info, false)
        buffer.clear()
        buffer.put(outputBuffer!!)
        releaseBuffer(outputBuffer)
    }

    @Throws(SourceClosedException::class)
    override fun getBuffer(info: MediaCodec.BufferInfo): ByteBuffer? {
        return pullBuffer(info, false)
    }

    @Throws(InvalidBufferException::class, SourceClosedException::class)
    override fun releaseBuffer(buffer: ByteBuffer?) {
        if (mComponentState == PipedMediaSource.State.CLOSED) {
            throw SourceClosedException()
        }
        mCodec!!.releaseOutputBuffer(outputBufferId,false)
        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.release buffer $outputBufferId")
        return
    }

    protected fun start() {
        mCodec!!.start()

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
    private fun pullBuffer(info: MediaCodec.BufferInfo, getFormat: Boolean): ByteBuffer? {
        if (mIsDone) {
            throw RuntimeException("pullBuffer called after depleted")
        }

        //If actually trying to get a buffer and we cached the buffer, return buffer from cache.
        if (!getFormat && !mBuffersBeforeFormat.isEmpty()) {
            val tempBuffer = mBuffersBeforeFormat.remove()
            MediaHelper.copyBufferInfo(tempBuffer.info, info)
            return tempBuffer.buffer
        }

        while (!mIsDone) {
            if (mComponentState == PipedMediaSource.State.CLOSED) {
                throw SourceClosedException()
            }
            try {
                outputBufferId = mCodec!!.dequeueOutputBuffer(
                        info, MediaHelper.TIMEOUT_USEC)
            } catch (e : Exception) {
                mIsDone = true
                outputBufferId = MediaCodec.INFO_TRY_AGAIN_LATER
            }
            if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.v(TAG, "$componentName.pullBuffer: output format changed")
                if (mOutputFormat != null) {
                    throw RuntimeException("changed output format again?")
                }
                mOutputFormat = mCodec!!.outputFormat
                if (getFormat) {
                    return null
                }
            }
            if (outputBufferId >= 0) {
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // TODO: Perhaps these buffers should not be ignored in the future.
                    // This indicated that the buffer marked as such contains codec initialization / codec specific data instead of media data.
                    // This should actually never occur...
                    // Simply ignore codec config buffers.
                    mCodec!!.releaseOutputBuffer(outputBufferId, false)
                } else {
                    val buffer = mCodec!!.getOutputBuffer(outputBufferId)!!

                    if (((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0) && (info.size != 0)) {
                        correctTime(info)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                    }

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.pullBuffer: EOS")
                        mIsDone = true
                    }

                    //If trying to get the format, save the buffer for later and don't return it.
                    if (getFormat) {
                        val tempInfo = MediaCodec.BufferInfo()
                        MediaHelper.copyBufferInfo(info, tempInfo)
                        mBuffersBeforeFormat.add(MediaBuffer(buffer, tempInfo))
                    } else {
                        return buffer
                    }
                }
            }
        }

        return null
    }

    /**
     * Correct the presentation time of the current buffer.
     * This function is primarily intended to be overridden by [PipedVideoSurfaceEncoder] to
     * allow video frames to be displayed at the proper time.
     * @param info to be updated
     */
    protected open fun correctTime(info: MediaCodec.BufferInfo) {
        if (mPresentationTimeUsLast > info.presentationTimeUs) {
            throw RuntimeException("buffer presentation time out of order!")
        }
        mPresentationTimeUsLast = info.presentationTimeUs
    }

    /**
     *
     * Gather input from source, feeding it into mCodec, until source is depleted.
     *
     * Note: This method **must return after [.mComponentState] becomes CLOSED**.
     */
    @Throws(SourceClosedException::class)
    protected abstract fun spinInput()

    override fun close() {
        //Shutdown child thread
        mComponentState = PipedMediaSource.State.CLOSED

        //Wait for two times the length of the timeout in the pullBuffer loop to ensure the codec
        //stops being used.
        try {
            Thread.sleep((MediaHelper.TIMEOUT_USEC * 2 / 1E6).toLong() + 1)
        } catch (e: InterruptedException) {
            Log.w(TAG, "sleep interrupted", e)
        }

        if (mThread != null) {
            try {
                mThread!!.join()
            } catch (e: InterruptedException) {
                Log.w(TAG, "$componentName: Failed to close input thread!", e)
            }

            mThread = null
        }

        //Shutdown MediaCodec
        if (mCodec != null) {
            try {
                mCodec!!.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "$componentName: Failed to stop MediaCodec!", e)
            } finally {
                mCodec!!.release()
            }
            mCodec = null
        }
    }

    companion object {
        private val TAG = "PipedMediaCodec"
    }
}

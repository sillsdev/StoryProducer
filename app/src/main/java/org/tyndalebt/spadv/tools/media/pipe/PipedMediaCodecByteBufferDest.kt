package org.tyndalebt.spadv.tools.media.pipe

import android.media.MediaCodec
import android.util.Log

import org.tyndalebt.spadv.tools.media.MediaHelper

import java.nio.ByteBuffer

/**
 *
 * This abstract media pipeline component provides a base for most encoders/decoders which work with
 * [ByteBuffer] input.
 */
abstract class PipedMediaCodecByteBufferDest : PipedMediaCodec(), PipedMediaByteBufferDest {

    protected var mSource: PipedMediaByteBufferSource? = null
    private val mInfo = MediaCodec.BufferInfo()

    @Throws(SourceUnacceptableException::class)
    override fun addSource(src: PipedMediaByteBufferSource) {
        if (mSource != null) {
            throw SourceUnacceptableException("One source already supplied!")
        }
        mSource = src
    }

    @Throws(SourceClosedException::class)
    override fun spinInput() {
        if (mSource == null) {
            throw RuntimeException("No source specified for encoder!")
        }

        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput starting")

        while (mComponentState != PipedMediaSource.State.CLOSED && !mSource!!.isDone) {
            val pollCode = mCodec!!.dequeueInputBuffer(MediaHelper.TIMEOUT_USEC)
            if (pollCode == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput: no input buffer")
                //Do nothing.
            } else {
                if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput: returned input buffer: $pollCode")

                var durationNs: Long = 0
                if (MediaHelper.DEBUG) {
                    durationNs = -System.nanoTime()
                }

                val inputBuffer = mCodec!!.getInputBuffer(pollCode)
                mSource!!.fillBuffer(inputBuffer, mInfo)
                mCodec!!.queueInputBuffer(pollCode, 0, mInfo.size, mInfo.presentationTimeUs, mInfo.flags)

                if (MediaHelper.DEBUG) {
                    durationNs += System.nanoTime()
                    val sec = durationNs / 1E9
                    Log.d(TAG, componentName + ".spinInput: fill/queue input buffer after "
                            + MediaHelper.getDecimal(sec) + " seconds: " + pollCode
                            + " of size " + mInfo.size + " for time " + mInfo.presentationTimeUs)
                }
            }

        }

        if (MediaHelper.VERBOSE) Log.v(TAG, "$componentName.spinInput complete!")

        mSource!!.close()
    }

    companion object {
        private val TAG = "PipedMediaCodecBBDest"
    }
}

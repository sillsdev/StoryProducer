package org.tyndalebt.spadv.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log

import org.tyndalebt.spadv.tools.media.MediaHelper

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 *
 * This media pipeline component multiplexes encoded audio and video streams into an output file.
 * This class primarily encapsulates a [MediaMuxer].
 *
 * Components commonly used in conjunction with this class are [PipedMediaCodec]
 * (particularly its subclasses [PipedMediaEncoder] and [PipedVideoSurfaceEncoder])
 * and [PipedMediaExtractor].
 */
class PipedMediaMuxer
/**
 * Create a muxer.
 * @param mPath the output media file.
 * @param mFormat the format of the output media file
 * (from [android.media.MediaMuxer.OutputFormat]).
 */
(private val mPath: String, private val mFormat: Int) : Closeable, PipedMediaByteBufferDest {

    private var mMuxer: MediaMuxer? = null

    @Volatile
    protected var mComponentState: PipedMediaSource.State = PipedMediaSource.State.UNINITIALIZED

    private var mAudioSource: PipedMediaByteBufferSource? = null
    private var mAudioTrackIndex = -1
    private var mAudioOutputFormat: MediaFormat? = null
    private val mAudioBitrate = -1
    private var mAudioThread: StreamThread? = null

    private var mVideoSource: PipedMediaByteBufferSource? = null
    private var mVideoTrackIndex = -1
    private var mVideoOutputFormat: MediaFormat? = null
    private val mVideoBitrate = -1
    private var mVideoThread: StreamThread? = null

    @Volatile
    private var mAbnormallyEnded = false

    /**
     * Get approximate current progress of the audio track (i.e. the latest timestamp in microseconds).
     * @return approximate microseconds of completed audio
     */
    val audioProgress: Long
        get() = getAudioProgress(true)

    /**
     * Get approximate current progress of the video track (i.e. the latest timestamp in microseconds).
     * @return approximate microseconds of completed video
     */
    val videoProgress: Long
        get() = getVideoProgress(true)

    @Throws(SourceUnacceptableException::class)
    override fun addSource(src: PipedMediaByteBufferSource) {
        if (src.mediaType == MediaHelper.MediaType.AUDIO) {
            if (mAudioSource == null) {
                mAudioSource = src
            } else {
                throw SourceUnacceptableException("audio source already provided")
            }
        } else if (src.mediaType == MediaHelper.MediaType.VIDEO) {
            if (mVideoSource == null) {
                mVideoSource = src
            } else {
                throw SourceUnacceptableException("video source already provided")
            }
        }
    }

    private fun getAudioProgress(allowDeflect: Boolean): Long {
        return if (mAudioThread != null) {
            mAudioThread!!.progress
        } else if (allowDeflect) {
            //If there is no audio channel, use the video progress as audio progress.
            getVideoProgress(false)
        } else {
            0
        }
    }

    private fun getVideoProgress(allowDeflect: Boolean): Long {
        return if (mVideoThread != null) {
            mVideoThread!!.progress
        } else if (allowDeflect) {
            //If there is no video channel, use the audio progress as video progress.
            getAudioProgress(false)
        } else {
            0
        }
    }

    /**
     * Set the muxer in motion.
     * @return whether the muxer finished its job.
     * @throws IOException
     * @throws SourceUnacceptableException
     */
    @Throws(IOException::class, SourceUnacceptableException::class)
    fun crunch(): Boolean {
        start()

        synchronized(audioLock) {
            if (mAudioSource != null) {
                mAudioThread = StreamThread(mMuxer!!, mAudioSource!!, mAudioTrackIndex, mAudioBitrate)
                mAudioThread!!.start()
            }
        }

        synchronized(videoLock) {
            if (mVideoSource != null) {
                mVideoThread = StreamThread(mMuxer!!, mVideoSource!!, mVideoTrackIndex, mVideoBitrate)
                mVideoThread!!.start()
            }
        }

        if (mAudioThread != null) {
            try {
                mAudioThread!!.join()
            } catch (e: InterruptedException) {
                Log.w(TAG, "Audio thread did not end!", e)
            }

        }

        if (mVideoThread != null) {
            try {
                mVideoThread!!.join()
            } catch (e: InterruptedException) {
                Log.w(TAG, "Video thread did not end!", e)
            }

        }

        close()

        return !mAbnormallyEnded
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    private fun start() {
        val output = File(mPath)
        //Ensure file exists to avoid bugs on some devices.
        if (!output.exists()) {
            output.createNewFile()
        }
        synchronized(muxerLock) {
            mMuxer = MediaMuxer(mPath, mFormat)
            mComponentState = PipedMediaSource.State.RUNNING

            if (mAudioSource != null) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "setting up audio track.")
                mAudioSource!!.setup()

                mAudioOutputFormat = mAudioSource!!.outputFormat
                //TODO: fudge bitrate since it isn't available
                //            mAudioBitrate = mAudioOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                if (MediaHelper.VERBOSE) Log.v(TAG, "adding audio track.")
                mAudioTrackIndex = mMuxer!!.addTrack(mAudioOutputFormat!!)
            }
            if (mVideoSource != null) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "setting up video track.")
                mVideoSource!!.setup()

                mVideoOutputFormat = mVideoSource!!.outputFormat
                //TODO: fudge bitrate since it isn't available
                //            mVideoBitrate = mVideoOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                if (MediaHelper.VERBOSE) Log.v(TAG, "adding video track.")
                mVideoTrackIndex = mMuxer!!.addTrack(mVideoOutputFormat!!)
            }
            if (MediaHelper.VERBOSE) Log.v(TAG, "starting")
            mMuxer!!.start()
        }
    }

    private inner class StreamThread(private val mMuxer: MediaMuxer, private val mSource: PipedMediaByteBufferSource, private val mTrackIndex: Int, private val mBitrate: Int) : Thread() {
        var progress: Long = 0
            private set

        override fun run() {
            var buffer: ByteBuffer
            val info = MediaCodec.BufferInfo()
            try {
                while (!mSource.isDone && mComponentState != PipedMediaSource.State.CLOSED) {
                    buffer = mSource.getBuffer(info)
                    if (MediaHelper.VERBOSE)
                        Log.v(TAG, "[track " + mTrackIndex + "] writing output buffer of size "
                                + info.size + " for time " + info.presentationTimeUs)

                    //Update progress if progress increased. (There may be edge cases where
                    //presentation time is 0, and that is an undesirable progress indicator.)
                    //In other words, never allow regression, only progression.
                    if (info.presentationTimeUs > progress) {
                        //TODO: determine presentation time for end of this buffer if possible
                        progress = info.presentationTimeUs// + (info.size * 1000000L / 8 / mBitrate);
                    }

                    synchronized(mMuxer) {
                        mMuxer.writeSampleData(mTrackIndex, buffer, info)
                    }
                    mSource.releaseBuffer(buffer)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Source closed forcibly", e)
                mAbnormallyEnded = true
            }

        }
    }

    override fun close() {
        synchronized(muxerLock) {
            //Close sources.
            mComponentState = PipedMediaSource.State.CLOSED

            //Close self.
            if (mMuxer != null) {
                try {
                    mMuxer!!.stop()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Failed to stop MediaMuxer!", e)
                } finally {
                    try {
                        mMuxer!!.release()
                    } catch (e: IllegalStateException) {
                        //It isn't documented that MediaMuxer.release throws an IllegalStateException
                        //sometimes, but it has been seen experimentally.
                        Log.w(TAG, "Failed to release MediaMuxer", e)
                    }

                }
                mMuxer = null
            }

            synchronized(audioLock) {
                if (mAudioSource != null) {
                    mAudioSource!!.close()
                    mAudioSource = null
                }
            }
            synchronized(videoLock) {
                if (mVideoSource != null) {
                    mVideoSource!!.close()
                    mVideoSource = null
                }
            }

        }
    }

    companion object {
        private val TAG = "PipedMediaMuxer"

        private val audioLock = Any()
        private val videoLock = Any()
        private val muxerLock = Any()
    }
}

package org.tyndalebt.spadv.tools.media.pipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import org.tyndalebt.spadv.tools.file.getStoryFileDescriptor

import org.tyndalebt.spadv.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer

/**
 *
 * This media pipeline component extracts a media file from a file and outputs an encoded media
 * stream. This class primarily encapsulates a [MediaExtractor].
 */
class PipedMediaExtractor
/**
 * Create extractor from specified file.
 * @param mPath path of the media file.
 * @param mType (audio/video) track to select from file.
 */
(private val context: Context, private val mPath: String, private val mType: MediaHelper.MediaType) : PipedMediaByteBufferSource {

    private var mComponentState: PipedMediaSource.State = PipedMediaSource.State.UNINITIALIZED

    private var mExtractor: MediaExtractor? = null

    private var mFormat: MediaFormat? = null

    private var mIsDone = false

    private val mBufferPool = ByteBufferPool()

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        mExtractor = MediaExtractor()
        mExtractor!!.setDataSource(getStoryFileDescriptor(context, mPath,"","r")!!)

        var foundTrack = false

        for (i in 0 until mExtractor!!.trackCount) {
            mFormat = mExtractor!!.getTrackFormat(i)
            if (!foundTrack && MediaHelper.getTypeFromFormat(mFormat!!) == mType) {
                mExtractor!!.selectTrack(i)
                foundTrack = true
            }
        }

        if (!foundTrack) {
            throw IOException("File does not contain track of type " + mType.name)
        }

        mComponentState = PipedMediaSource.State.SETUP
    }

    override fun getMediaType(): MediaHelper.MediaType {
        return mType
    }

    override fun getOutputFormat(): MediaFormat? {
        return mFormat
    }

    override fun isDone(): Boolean {
        return mIsDone
    }

    override fun fillBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        pullBuffer(buffer, info)
    }

    override fun getBuffer(info: MediaCodec.BufferInfo): ByteBuffer {
        val buffer = mBufferPool.get()
        pullBuffer(buffer, info)
        return buffer
    }

    @Throws(InvalidBufferException::class)
    override fun releaseBuffer(buffer: ByteBuffer) {
        mBufferPool.release(buffer)
    }

    private fun pullBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (mIsDone) {
            throw RuntimeException("pullBuffer called after depleted")
        }

        buffer.clear()

        info.offset = 0
        info.size = mExtractor!!.readSampleData(buffer, 0)
        info.presentationTimeUs = mExtractor!!.sampleTime
        val actualFlags = mExtractor!!.sampleFlags
        info.flags = 0
        //TODO: Do we need this SDK check?
        if (Build.VERSION.SDK_INT >= 21 && actualFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
            info.flags = info.flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (actualFlags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            info.flags = info.flags or MediaCodec.BUFFER_FLAG_END_OF_STREAM
        }
        //TODO: Why aren't these listed in documentation but in annotations?
        //            if((actualFlags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
        //                info.flags |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;
        //            }
        //            if((actualFlags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
        //                info.flags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        //            }
        if (MediaHelper.VERBOSE)
            Log.v(TAG, "pullBuffer: return buffer of size "
                    + info.size + " for time " + info.presentationTimeUs)

        if (info.size >= 0) {
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)
            mExtractor!!.advance()
        } else {
            if (MediaHelper.VERBOSE) Log.v(TAG, "pullBuffer: EOS")
            info.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            mIsDone = true
        }
    }

    override fun close() {
        if (mExtractor != null) {
            mExtractor!!.release()
            mExtractor = null
        }
    }

    companion object {
        private val TAG = "PipedMediaExtractor"
    }
}

package org.sil.storyproducer.tools.media.pipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 *
 * This media pipeline component loops a single audio file for a specified amount of time.
 */
class PipedAudioLooper
/**
 * Create looper from an audio file with specified duration, resampling the audio stream.
 * @param path path of the audio file.
 * @param durationUs desired duration in microseconds.
 * @param sampleRate desired sample rate.
 * @param channelCount desired channel count.
 * @param volumeModifier volume scaling factor.
 */
@JvmOverloads constructor(private val context: Context, private val mPath: String, private val mDurationUs: Long, sampleRate: Int = 0, channelCount: Int = 0, private val mVolumeModifier: Float = 1f) : PipedAudioShortManipulator() {

    private var mSource: PipedMediaByteBufferSource? = null

    private var mOutputFormat: MediaFormat? = null

    private val mSourceBufferA = ShortArray(MediaHelper.MAX_INPUT_BUFFER_SIZE / 2)

    private var mPos: Int = 0
    private var mSize: Int = 0
    private var mHasBuffer = false

    private val mInfo = MediaCodec.BufferInfo()
    override val componentName: String = TAG

    init {
        mSampleRate = sampleRate
        mChannelCount = channelCount
    }

    override fun getOutputFormat(): MediaFormat? {
        return mOutputFormat
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        mSource = PipedAudioDecoderMaverick(context, mPath, mSampleRate, mChannelCount, mVolumeModifier)
        mSource!!.setup()

        validateSource(mSource!!)

        try {
            fetchSourceBuffer()
        } catch (e: SourceClosedException) {
            //This case should not happen.
            throw SourceUnacceptableException("First fetchSourceBuffer failed! Strange", e)
        }

        val sourceOutputFormat = mSource!!.outputFormat
        mSampleRate = sourceOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        mChannelCount = sourceOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO)
        mOutputFormat!!.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate)
        mOutputFormat!!.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    override fun getSampleForChannel(channel: Int): Short {
        if (mHasBuffer) {
            try {
                return mSourceBufferA[mPos + channel]
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "Tried to read beyond buffer", e)
            }

        }

        //only necessary for exception case
        return 0
    }

    @Throws(SourceClosedException::class)
    override fun loadSamplesForTime(time: Long): Boolean {
        //Component is done if duration is exceeded.
        if (time >= mDurationUs) {
            mSource!!.close()
            mSource = null
            return false
        }

        mPos += mChannelCount

        while (mHasBuffer && mPos >= mSize) {
            releaseSourceBuffer()
            fetchSourceBuffer()
        }
        if (!mHasBuffer) {
            mSource!!.close()
            mSource = PipedAudioDecoderMaverick(context, mPath, mSampleRate, mChannelCount, mVolumeModifier)

            try {
                mSource!!.setup()
                fetchSourceBuffer()
            } catch (e: IOException) {
                Log.e(TAG, "Source setup failed!", e)
                mSource!!.close()
                mSource = null
                return false
            } catch (e: SourceUnacceptableException) {
                Log.e(TAG, "Source setup failed!", e)
                mSource!!.close()
                mSource = null
                return false
            }

        }

        return true
    }

    @Throws(SourceClosedException::class)
    private fun fetchSourceBuffer() {
        if (mSource!!.isDone) {
            return
        }

        //buffer of bytes
        val buffer = mSource!!.getBuffer(mInfo)
        //buffer of shorts (16-bit samples)
        val sBuffer = MediaHelper.getShortBuffer(buffer)

        mPos = 0
        mSize = sBuffer.remaining()
        //Copy ShortBuffer to array of shorts in hopes of speedup.
        sBuffer.get(mSourceBufferA, mPos, mSize)

        //Release buffer since data was copied.
        mSource!!.releaseBuffer(buffer)

        mHasBuffer = true
    }

    private fun releaseSourceBuffer() {
        mHasBuffer = false
    }

    override fun close() {
        super.close()
        if (mSource != null) {
            mSource!!.close()
            mSource = null
        }
    }

    companion object {
        private val TAG = "PipedAudioLooper"
    }
}
/**
 * Create looper from an audio file with specified duration, using the file's format.
 * @param path path of the audio file.
 * @param durationUs desired duration in microseconds.
 */
/**
 * Create looper from an audio file with specified duration, resampling the audio stream.
 * @param path path of the audio file.
 * @param durationUs desired duration in microseconds.
 * @param sampleRate desired sample rate.
 * @param channelCount desired channel count.
 */

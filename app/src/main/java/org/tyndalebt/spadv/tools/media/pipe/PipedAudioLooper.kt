package org.tyndalebt.spadv.tools.media.pipe

import android.content.Context
import android.media.MediaFormat
import android.util.Log
import org.tyndalebt.spadv.tools.media.MediaHelper
import java.io.IOException

/**
 *
 * This media pipeline component loops a single audio file for a specified amount of time.
 */
class PipedAudioLooper
/**
 * Create looper from an audio file with specified duration, resampling the audio stream.
 * @param mPath path of the audio file.
 * @param mDurationUs desired duration in microseconds.
 * @param sampleRate desired sample rate.
 * @param channelCount desired channel count.
 * @param mVolumeModifier volume scaling factor.
 */
@JvmOverloads constructor(private val context: Context, private val mPath: String, private val mDurationUs: Long, sampleRate: Int = 0, channelCount: Int = 0, private val mVolumeModifier: Float = 1f) : PipedAudioShortManipulator() {

    private var mOutputFormat: MediaFormat? = null

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

    @Throws(SourceClosedException::class)
    override fun loadSamples(): Boolean {
        //Component is done if duration is exceeded.
        if (mSeekTime >= mDurationUs) {
            mSource!!.close()
            mSource = null
            return false
        }

        if (srcHasBuffer && srcPos >= srcEnd) {
            fetchSourceBuffer()
        }
        if (!srcHasBuffer) {
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

    companion object {
        private val TAG = "PipedAudioLooper"
    }
}
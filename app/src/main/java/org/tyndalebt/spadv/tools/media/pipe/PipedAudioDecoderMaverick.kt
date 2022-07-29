package org.tyndalebt.spadv.tools.media.pipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat

import org.tyndalebt.spadv.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer

/**
 *
 * This media pipeline component is a thin wrapper for the commonly used triumvirate of
 * [PipedMediaExtractor], [PipedMediaDecoder], and [PipedAudioResampler].
 */
class PipedAudioDecoderMaverick
/**
 * Create maverick from a file, resampling the audio stream.
 * @param mPath path of the audio file.
 * @param mSampleRate desired sample rate.
 * @param mChannelCount desired channel count.
 * @param mVolumeModifier volume scaling factor.
 */
@JvmOverloads constructor(private val context: Context, private val mPath: String, private val mSampleRate: Int = 0, private val mChannelCount: Int = 0, private val mVolumeModifier: Float = 1f) : PipedMediaByteBufferSource {

    private var mComponentState: PipedMediaSource.State = PipedMediaSource.State.UNINITIALIZED

    private var mSource: PipedMediaByteBufferSource? = null

    override fun getMediaType(): MediaHelper.MediaType {
        return MediaHelper.MediaType.AUDIO
    }

    override fun getOutputFormat(): MediaFormat {
        return mSource!!.outputFormat
    }

    override fun isDone(): Boolean {
        return mSource!!.isDone
    }

    @Throws(SourceClosedException::class)
    override fun fillBuffer(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        mSource!!.fillBuffer(buffer, info)
    }

    @Throws(SourceClosedException::class)
    override fun getBuffer(info: MediaCodec.BufferInfo): ByteBuffer {
        return mSource!!.getBuffer(info)
    }

    @Throws(InvalidBufferException::class, SourceClosedException::class)
    override fun releaseBuffer(buffer: ByteBuffer) {
        mSource!!.releaseBuffer(buffer)
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        val extractor = PipedMediaExtractor(context, mPath, MediaHelper.MediaType.AUDIO)

        val decoder = PipedMediaDecoder()
        decoder.addSource(extractor)
        decoder.setup()

        if (Math.abs(mVolumeModifier - 1) < 0.001) {
            mSource = PipedAudioResampler.correctSampling(decoder, mSampleRate, mChannelCount)
        } else {
            val resampler = PipedAudioResampler(mSampleRate, mChannelCount)
            resampler.setVolumeModifier(mVolumeModifier)
            resampler.addSource(decoder)
            mSource = resampler
        }
        mSource!!.setup()

        mComponentState = PipedMediaSource.State.SETUP
    }

    override fun close() {
        if (mSource != null) {
            mSource!!.close()
            mSource = null
        }
    }

    companion object {
        private val TAG = "PipedAudioMaverick"
    }
}
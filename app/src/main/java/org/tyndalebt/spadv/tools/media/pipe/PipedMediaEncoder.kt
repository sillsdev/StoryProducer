package org.tyndalebt.spadv.tools.media.pipe

import android.media.MediaCodec
import android.media.MediaFormat

import org.tyndalebt.spadv.tools.media.MediaHelper
import org.tyndalebt.spadv.tools.selectCodec

import java.io.IOException

/**
 *
 * This media pipeline component provides a simple encoder encapsulating a [MediaCodec].
 * Therefore, it takes a raw media stream and outputs an encoded media stream.
 *
 * Common source for this component include [PipedMediaDecoder]
 * or any child class of [PipedAudioShortManipulator].
 */
class PipedMediaEncoder(private val mConfigureFormat: MediaFormat) : PipedMediaCodecByteBufferDest() {
    override val componentName: String
        get() = TAG
    private var mSourceFormat: MediaFormat? = null

    override fun getMediaType(): MediaHelper.MediaType {
        return MediaHelper.getTypeFromFormat(mConfigureFormat)
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        mSource!!.setup()
        mSourceFormat = mSource!!.outputFormat

        //audio keys
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_CHANNEL_COUNT)
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_SAMPLE_RATE)

        //video keys
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_WIDTH)
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_HEIGHT)
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_COLOR_FORMAT)
        MediaHelper.copyFormatIntKey(mSourceFormat!!, mConfigureFormat, MediaFormat.KEY_FRAME_RATE)
        //TODO: worry about KEY_CAPTURE_RATE being API 21+
        //TODO This may be why the video playback is not synced with newer phones.
        //MediaHelper.copyFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CAPTURE_RATE);

        //encoder input buffers are too small, by default, to handle some decoder output buffers
        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE)

        mCodec = MediaCodec.createByCodecName(selectCodec(mConfigureFormat.getString(MediaFormat.KEY_MIME)!!)!!.name)
        mCodec!!.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    companion object {
        private val TAG = "PipedMediaEncoder"
    }
}

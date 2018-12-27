package org.sil.storyproducer.tools.media.pipe

import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.view.Surface

import org.sil.storyproducer.tools.media.MediaHelper

import java.io.IOException
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.NoSuchElementException
import java.util.Queue

/**
 *
 * This media pipeline component provides a simple encoder encapsulating a [MediaCodec].
 * Unlike [PipedMediaEncoder], it uses an input [Surface] instead of ByteBuffers.
 * This component takes raw canvas frames of a video and outputs an encoded video stream.
 *
 * Sources for this component must implement [Source].
 */
class PipedVideoSurfaceEncoder : PipedMediaCodec() {
    override val componentName: String
        get() = TAG

    private var mSurface: Surface? = null

    private var mConfigureFormat: MediaFormat? = null
    private var mSource: Source? = null

    private val mPresentationTimeQueue = LinkedList<Long>()

    private var mCurrentPresentationTime: Long = 0

    override fun getMediaType(): MediaHelper.MediaType {
        return MediaHelper.MediaType.VIDEO
    }

    /**
     * Specify a canvas provider for this component in the pipeline.
     * @param src the preceding component (a canvas drawer) of the pipeline.
     * @throws SourceUnacceptableException
     */
    @Throws(SourceUnacceptableException::class)
    fun addSource(src: Source) {
        if (mSource != null) {
            throw SourceUnacceptableException("I already got a source")
        }
        mSource = src
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mComponentState != PipedMediaSource.State.UNINITIALIZED) {
            return
        }

        mSource!!.setup()
        mConfigureFormat = mSource!!.outputFormat

        mConfigureFormat!!.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat!!.getString(MediaFormat.KEY_MIME))
        mCodec!!.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        mSurface = mCodec!!.createInputSurface()

        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    override fun spinInput() {
        if (mSource == null) {
            throw RuntimeException("No source provided!")
        }

        while (mComponentState != PipedMediaSource.State.CLOSED && !mSource!!.isDone) {
            //Note: This method of getting a canvas to draw to may be invalid
            //per documentation of MediaCodec.getInputSurface().

            while(mPresentationTimeQueue.size > PipedAudioShortManipulator.BUFFER_COUNT-1){
                //Really, for async processing we would use MediaCodec.Callback(), but maybe we can
                //just count the number of buffers used through looking at the time queue.
                Thread.sleep(10)
            }
            val canv = if (Build.VERSION.SDK_INT >= 23) {
                mSurface!!.lockHardwareCanvas()
            } else {
                mSurface!!.lockCanvas(null)
            }
            mCurrentPresentationTime = mSource!!.fillCanvas(canv)
            synchronized(mPresentationTimeQueue) {
                mPresentationTimeQueue.add(mCurrentPresentationTime)
            }
            mSurface!!.unlockCanvasAndPost(canv)
        }

        if (mComponentState != PipedMediaSource.State.CLOSED) {
            mCodec!!.signalEndOfInputStream()
        }

        mSource!!.close()
    }

    override fun correctTime(info: MediaCodec.BufferInfo) {
        try {
            synchronized(mPresentationTimeQueue) {
                info.presentationTimeUs = mPresentationTimeQueue.pop()
            }
        } catch (e: NoSuchElementException) {
            throw RuntimeException("Tried to correct time for extra frame", e)
        }
    }

    /**
     * Describes a component of the media pipeline which draws frames to a provided canvas when called.
     */
    interface Source : PipedMediaSource {
        /**
         * Request that this component draw a frame to the canvas.
         * @param canv the canvas to be drawn upon.
         * @return the presentation time (in microseconds) of the drawn frame.
         */
        fun fillCanvas(canv: Canvas): Long
    }

    companion object {
        private val TAG = "PipedVideoSurfaceEnc"
    }
}

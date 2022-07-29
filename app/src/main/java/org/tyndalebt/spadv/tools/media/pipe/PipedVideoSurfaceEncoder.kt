package org.tyndalebt.spadv.tools.media.pipe

import android.graphics.Canvas
import android.media.*
import android.view.Surface
import org.tyndalebt.spadv.tools.media.MediaHelper
import org.tyndalebt.spadv.tools.media.pipe.PipedVideoSurfaceEncoder.Source
import org.tyndalebt.spadv.tools.selectCodec
import java.io.IOException
import java.util.*


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

    private var mCanvas: Canvas? = null
    private var mSurface: Surface? = null

    private var mConfigureFormat: MediaFormat? = null
    private var mSource: Source? = null

    private val mPresentationTimeQueue = LinkedList<Long>()

    private val mStartPresentationTime: Long = System.nanoTime()/1000
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

        mCodec = MediaCodec.createByCodecName(selectCodec(mConfigureFormat!!.getString(MediaFormat.KEY_MIME)!!)!!.name)
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

            //For video creation, it should be able to create one slide from one image.
            //If there is something holding it up, keep going but give it time to process.
            //give it 100ms to process a frame.
            var waitTries = 0
            while(mPresentationTimeQueue.size > 3 && waitTries++ < 10){
                //Really, for async processing we would use MediaCodec.Callback(), but maybe we can
                //just count the number of buffers used through looking at the time queue.
                Thread.sleep(10)
            }
            // 4/6/2022 - DKH, Issue 639: Logcat reports this error: FrameEvents: updateAcquireFence: Did not find frame.
            // The following code was previously used to allow the use of hardware Codec video
            // encoding.
            // mCanvas = if (Build.VERSION.SDK_INT >= 23) {
            //    mSurface!!.lockHardwareCanvas()
            // } else {
            //    mSurface!!.lockCanvas(null)
            //}
            // With the advent of Android 11 and later versions, the Codec encoder began reporting
            // to the system error log the following error per frame of data:
            //      FrameEvents: updateAcquireFence: Did not find frame.
            // For small stories (6 slides), this resulted ~3000 error messages to the system log
            // Further analysis of the performance data showed that in most cases, the software
            // codec encoder performed better than the hardware encoder.
            // The following change uses the software codec to eliminate the error messages and
            // provides better performance in most cases.
            mCanvas = mSurface!!.lockCanvas(null)  // force use of software video codec encoder

            mCurrentPresentationTime = mSource!!.fillCanvas(mCanvas!!)

            synchronized(mPresentationTimeQueue) {
                mPresentationTimeQueue.add(mCurrentPresentationTime)
            }
            mSurface!!.unlockCanvasAndPost(mCanvas!!)
        }

        if (mComponentState != PipedMediaSource.State.CLOSED){
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

package org.sil.storyproducer.tools.media.pipe

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.*
import android.os.Build
import android.support.v4.math.MathUtils
import android.view.Surface
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceEncoder.Source
import org.sil.storyproducer.tools.selectCodec
import java.io.IOException
import java.util.*
import java.nio.ByteBuffer


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

    private var mBitmap: Bitmap? = null
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

        mCodec = MediaCodec.createByCodecName(selectCodec(mConfigureFormat!!.getString(MediaFormat.KEY_MIME))!!.name)
        mCodec!!.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        if(mConfigureFormat!!.getString(MediaFormat.KEY_MIME) == "video/avc"){
            //For H264, just use the surface.  It works fine.
            mSurface = mCodec!!.createInputSurface()
        }else{
            //For H263, funny things happen with timestamps.
            //Use the Image capabilitites to be able to specify a timestamp with the data.
            mBitmap = Bitmap.createBitmap(
                    mConfigureFormat!!.getInteger(MediaFormat.KEY_WIDTH),
                    mConfigureFormat!!.getInteger(MediaFormat.KEY_HEIGHT),
                    Bitmap.Config.ARGB_8888) //matches MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565

            mCanvas = Canvas(mBitmap)
        }


        mComponentState = PipedMediaSource.State.SETUP

        start()
    }

    override fun spinInput() {
        if (mSource == null) {
            throw RuntimeException("No source provided!")
        }

        while (mComponentState != PipedMediaSource.State.CLOSED && !mSource!!.isDone) {

            //Posting to the canvas should be done synchonously, but it is on different threads.
            //Synchonize with PipedMediaMuxerRun!
            //TODO remove the wait?
            while(unreleasedBuffer){
                //Really, for async processing we would use MediaCodec.Callback(), but maybe we can
                //just count the number of buffers used through looking at the time queue.
                Thread.sleep(10)
            }
            if(mSurface != null){
                mCanvas = if (Build.VERSION.SDK_INT >= 23) {
                    mSurface!!.lockHardwareCanvas()
                } else {
                    mSurface!!.lockCanvas(null)
                }
                mCurrentPresentationTime = mSource!!.fillCanvas(mCanvas!!)
                synchronized(mPresentationTimeQueue) {
                    mPresentationTimeQueue.add(mCurrentPresentationTime)
                }
                mSurface!!.unlockCanvasAndPost(mCanvas!!)
            }else{
                val buf_int = mCodec!!.dequeueInputBuffer(10000)
                if(buf_int >= 0){
                    val image = mCodec!!.getInputImage(buf_int)!!

                    mCurrentPresentationTime = mSource!!.fillCanvas(mCanvas!!)
                    synchronized(mPresentationTimeQueue) {
                        mPresentationTimeQueue.add(mCurrentPresentationTime)
                    }
                    RGBToYUV(mBitmap!!,image)

                    val size = image.planes[0].buffer.capacity() +
                            image.planes[1].buffer.capacity() +
                            image.planes[2].buffer.capacity()
                    mCodec!!.queueInputBuffer(buf_int,0,size,
                            mStartPresentationTime + mCurrentPresentationTime,0)
                }
            }
        }

        if (mComponentState != PipedMediaSource.State.CLOSED){
            if(mSurface != null) {
                mCodec!!.signalEndOfInputStream()
            }else{
                //Send empty buffer with "end of stream" flag.
                val buf_int = mCodec!!.dequeueInputBuffer(10000)
                mCodec!!.queueInputBuffer(buf_int,0,0,
                        mStartPresentationTime + mCurrentPresentationTime,MediaCodec.BUFFER_FLAG_END_OF_STREAM)

            }
        }

        mSource!!.close()
    }

    override fun correctTime(info: MediaCodec.BufferInfo) {
        try {
            if(mSurface != null) {
                synchronized(mPresentationTimeQueue) {
                    info.presentationTimeUs = mPresentationTimeQueue.pop()
                }
            }else{
                info.presentationTimeUs -= mStartPresentationTime
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

        //Bitmap is ARGB_8888
        //Image is YUV 888 (3 planes)
        @kotlin.ExperimentalUnsignedTypes
        fun RGBToYUV(bitmapRGB: Bitmap, imageYUV: Image) {

            val h = bitmapRGB.height
            val w = bitmapRGB.width

            val y = imageYUV.planes[0].buffer
            val u = imageYUV.planes[1].buffer
            val v = imageYUV.planes[2].buffer

            var unew = DoubleArray(w/2)
            var vnew = DoubleArray(w/2)

            val bufferRGB = ByteBuffer.allocate(bitmapRGB.getByteCount())
            bitmapRGB.copyPixelsToBuffer(bufferRGB)

            bufferRGB.rewind()

            for (i in 0 until h * w) {
                val r:Int = bufferRGB.get().toUByte().toInt()
                val g:Int = bufferRGB.get().toUByte().toInt()
                val b:Int = bufferRGB.get().toUByte().toInt()
                val a:Int = bufferRGB.get().toUByte().toInt()

                y.put(MathUtils.clamp(0.299 * r + 0.587 * g + 0.114 * b,0.0,255.0).toByte())

                val ind = (i % w) / 2
                val stage = (i % 2) + ((i / w) % 2) * 2
                when (stage % 4) {
                    0 -> {
                        unew[ind] = -0.147 * r - 0.289 * g + 0.436 * b
                        vnew[ind] = 0.615 * r - 0.515 * g - 0.100 * b
                    }
                    1, 2 -> {
                        unew[ind] += -0.147 * r - 0.289 * g + 0.436 * b
                        vnew[ind] += 0.615 * r - 0.515 * g - 0.100 * b
                    }
                    3 -> {
                        val utemp = unew[ind] + -0.147 * r - 0.289 * g + 0.436 * b
                        val vtemp = vnew[ind] + 0.615 * r - 0.515 * g - 0.100 * b
                        u.put(MathUtils.clamp(128.0+utemp/4.0,0.0,255.0).toByte())
                        v.put(MathUtils.clamp(128.0+vtemp/4.0,0.0,255.0).toByte())
                    }
                }
            }
        }
    }
}

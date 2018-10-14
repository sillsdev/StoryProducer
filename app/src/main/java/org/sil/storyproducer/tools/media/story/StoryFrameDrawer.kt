package org.sil.storyproducer.tools.media.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaFormat
import android.text.Layout
import android.util.Log
import org.sil.storyproducer.tools.file.getStoryImage

import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceEncoder
import org.sil.storyproducer.tools.media.pipe.SourceUnacceptableException

import java.io.IOException

/**
 * This class knows how to draw the frames provided to it by [StoryMaker].
 */
internal class StoryFrameDrawer(private val context: Context, private val mVideoFormat: MediaFormat, private val mPages: Array<StoryPage>, private val mAudioTransitionUs: Long, slideCrossFadeUs: Long) : PipedVideoSurfaceEncoder.Source {
    private val mSlideCrossFadeUs: Long

    private val mFrameRate: Int

    private val mWidth: Int
    private val mHeight: Int
    private val mScreenRect: Rect

    private val mBitmapPaint: Paint

    private var mCurrentTextOverlay: TextOverlay? = null
    private var mNextTextOverlay: TextOverlay? = null

    private var mCurrentSlideIndex = -1 //starts at -1 to allow initial transition
    private var mCurrentSlideExDuration: Long = 0 //exclusive duration of current slide
    private var mCurrentSlideStart: Long = 0 //time (after transition) of audio start

    private var mCurrentSlideImgDuration: Long = 0 //total duration of current slide image (cached for performance)
    private var mNextSlideImgDuration: Long = 0 //total duration of next slide image (cached for performance)

    private var mCurrentFrame = 0

    private var mIsVideoDone = false

    private var bitmaps: MutableMap<String,Bitmap?> = mutableMapOf()

    init {

        var correctedSlideTransitionUs = slideCrossFadeUs

        //mSlideTransition must never exceed the length of slides in terms of audio.
        //Pre-process pages and clip the slide transition time to fit in all cases.
        for (page in mPages) {
            val totalPageUs = page.audioDuration + mAudioTransitionUs
            if (correctedSlideTransitionUs > totalPageUs) {
                correctedSlideTransitionUs = totalPageUs
                Log.d(TAG, "Corrected slide transition from $slideCrossFadeUs to $correctedSlideTransitionUs")
            }
        }

        mSlideCrossFadeUs = correctedSlideTransitionUs

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH)
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
        mScreenRect = Rect(0, 0, mWidth, mHeight)

        mBitmapPaint = Paint()
        mBitmapPaint.isAntiAlias = true
        mBitmapPaint.isFilterBitmap = true
        mBitmapPaint.isDither = true
    }

    override fun getMediaType(): MediaHelper.MediaType {
        return MediaHelper.MediaType.VIDEO
    }

    override fun getOutputFormat(): MediaFormat {
        return mVideoFormat
    }

    override fun isDone(): Boolean {
        return mIsVideoDone
    }

    @Throws(IOException::class, SourceUnacceptableException::class)
    override fun setup() {
        if (mPages.isNotEmpty()) {
            val nextPage = mPages[0]
            mNextSlideImgDuration = nextPage.getVisibleDuration(mAudioTransitionUs, mSlideCrossFadeUs)

            val nextText = nextPage.text
            mNextTextOverlay = TextOverlay(nextText)
            //Push text to bottom if there is a picture.
            mNextTextOverlay!!.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
        }
    }

    override fun fillCanvas(canv: Canvas): Long {

        //TODO somethere in here is where the video time is not working properly.
        val currentTimeUs = MediaHelper.getTimeFromIndex(mFrameRate.toLong(), mCurrentFrame)

        var nextSlideTransitionUs = mSlideCrossFadeUs
        //For pre-first "slide" and last slide, make the transition half as long.
        if (mCurrentSlideIndex == -1 || mCurrentSlideIndex == mPages.size - 1) {
            nextSlideTransitionUs /= 2
        }

        var nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideExDuration

        while (currentTimeUs > nextSlideTransitionStartUs + nextSlideTransitionUs) {
            mCurrentSlideIndex++

            if (mCurrentSlideIndex >= mPages.size) {
                mIsVideoDone = true
                break
            }

            val currentPage = mPages[mCurrentSlideIndex]

            mCurrentSlideStart += mCurrentSlideExDuration + nextSlideTransitionUs
            mCurrentSlideExDuration = currentPage.getExclusiveDuration(mAudioTransitionUs, mSlideCrossFadeUs)
            mCurrentSlideImgDuration = mNextSlideImgDuration
            nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideExDuration

            mCurrentTextOverlay = mNextTextOverlay

            if (mCurrentSlideIndex + 1 < mPages.size) {
                val nextPage = mPages[mCurrentSlideIndex + 1]
                mNextSlideImgDuration = nextPage.getVisibleDuration(mAudioTransitionUs, mSlideCrossFadeUs)

                val nextText = nextPage.text
                mNextTextOverlay = TextOverlay(nextText)
                //Push text to bottom if there is a picture.
                mNextTextOverlay!!.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
            }
        }

        val timeSinceCurrentSlideStartUs = currentTimeUs - mCurrentSlideStart
        val currentSlideOffsetUs = timeSinceCurrentSlideStartUs + mSlideCrossFadeUs

        drawFrame(canv, mCurrentSlideIndex, currentSlideOffsetUs, mCurrentSlideImgDuration,
                1f, mCurrentTextOverlay)

        if (currentTimeUs >= nextSlideTransitionStartUs) {
            val timeSinceTransitionStartUs = currentTimeUs - nextSlideTransitionStartUs
            val extraOffsetUs = mSlideCrossFadeUs - nextSlideTransitionUs //0 normally, transition/2 for edge cases
            val nextOffsetUs = timeSinceTransitionStartUs + extraOffsetUs
            var alpha = nextOffsetUs / nextSlideTransitionUs.toFloat()
            //Don't "fade in" at the beginning.
            if(mCurrentSlideIndex==-1) alpha = 1.0f
            drawFrame(canv, mCurrentSlideIndex + 1, nextOffsetUs, mNextSlideImgDuration,
                    alpha, mNextTextOverlay)
        }

        //clear image cache to save memory.
        if(mCurrentSlideIndex >= 1) {
            if (bitmaps.containsKey(mPages[mCurrentSlideIndex - 1].imRelPath)) {
                bitmaps.remove(mPages[mCurrentSlideIndex - 1].imRelPath)
            }
        }

        mCurrentFrame++

        return currentTimeUs
    }

    private fun drawFrame(canv: Canvas, pageIndex: Int, timeOffsetUs: Long, imgDurationUs: Long,
                          alpha: Float, overlay: TextOverlay?) {
        //In edge cases, draw a black frame with alpha value.
        if (pageIndex < 0 || pageIndex >= mPages.size) {
            canv.drawARGB((alpha * 255).toInt(), 0, 0, 0)
            return
        }

        val page = mPages[pageIndex]
        if(!bitmaps.containsKey(page.imRelPath)){
            bitmaps[page.imRelPath] = getStoryImage(context,page.imRelPath)
        }
        val bitmap = bitmaps[page.imRelPath]

        if (bitmap != null) {
            val kbfx = page.kenBurnsEffect

            val position = (timeOffsetUs / imgDurationUs.toDouble()).toFloat()
            val drawRect: Rect
            if (kbfx != null) {
                drawRect = kbfx.interpolate(position)
            } else {
                drawRect = Rect(0, 0, bitmap.width, bitmap.height)
            }

            if (MediaHelper.DEBUG) {
                Log.d(TAG, "drawing bitmap (" + bitmap.width + "x" + bitmap.height + ") from "
                        + drawRect + " to canvas " + mScreenRect)
            }

            mBitmapPaint.alpha = (alpha * 255).toInt()

            canv.drawBitmap(bitmap, drawRect, mScreenRect, mBitmapPaint)
        } else {
            //If there is no picture, draw black background for text overlay.
            canv.drawARGB((alpha * 255).toInt(), 0, 0, 0)
        }

        if (overlay != null) {
            overlay.setAlpha(alpha)
            overlay.draw(canv)
        }
    }

    override fun close() {
        bitmaps.clear()
    }

    companion object {
        private val TAG = "StoryFrameDrawer"
    }
}

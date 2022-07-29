package org.tyndalebt.spadv.tools.media.story

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaFormat
import android.util.Log
import org.tyndalebt.spadv.model.SlideType
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.service.SlideService
import org.tyndalebt.spadv.tools.BitmapScaler
import org.tyndalebt.spadv.tools.file.getDownsample
import org.tyndalebt.spadv.tools.media.MediaHelper
import org.tyndalebt.spadv.tools.media.pipe.PipedVideoSurfaceEncoder

/**
 * This class knows how to draw the frames provided to it by [StoryMaker].
 */
internal class StoryFrameDrawer(private val context: Context, private val mVideoFormat: MediaFormat, private val mPages: Array<StoryPage>, private val mAudioTransitionUs: Long, slideCrossFadeUs: Long) : PipedVideoSurfaceEncoder.Source {
    private val xTime: Long //transition (cross fade) time

    private val mFrameRate: Int

    private val mWidth: Int
    private val mHeight: Int

    private val mBitmapPaint: Paint

    private var slideIndex = -1 //starts at -1 to allow initial transition
    private var slideAudioStart: Long = 0
    private var slideAudioEnd: Long = 0
    private var nSlideAudioEnd: Long = 0
    private val slideVisStart: Long
        get() {return if(slideIndex<=0){slideAudioStart} else {slideAudioStart-xTime/2}}
    private val slideXStart: Long  //beginning of the next transition
        get() {return if(slideIndex>=mPages.size-1){slideAudioEnd} else {slideAudioEnd-xTime/2}}
    private val slideXEnd: Long  //end of the next transition
        get() {return if(slideIndex>=mPages.size-1){slideAudioEnd} else {slideAudioEnd+xTime/2}}
    private val nSlideXEnd: Long  //end of the next transition
        get() {return if(slideIndex>=mPages.size-2){nSlideAudioEnd} else {nSlideAudioEnd+xTime/2}}
    private val slideVisDur: Long // the visible duration of the slide
        get() {return slideXEnd - slideVisStart}
    private val nSlideVisDur: Long // the visible duration of the next slide
        get() {return nSlideXEnd - slideXStart}

    private var mCurrentFrame = 0

    private var mIsVideoDone = false

    private var bitmaps: MutableMap<String,Bitmap?> = mutableMapOf()
    private var downsamples: MutableMap<String,Int> = mutableMapOf()

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

        xTime = correctedSlideTransitionUs

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH)
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)

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

    override fun setup() {}

    override fun fillCanvas(canv: Canvas): Long {

        //[-|-page-1-|-| ]
        //           [ |-|-page-2-|-| ]
        //                        [ |-|-page-last-|-]
        // | | | (two bars) = transition time (xtime)
        // | | (one bar) = 1/2 xtime
        // --- (dash) sound playing from slide
        // Exclusive time + xtime/2 for first and last slide
        // "current page" is the page until it ends
        // "Next page" is growing in intensity for "xtime"
        // Visible time

        //Each time this is called, go forward 1/30 of a second.
        val cTime = MediaHelper.getTimeFromIndex(mFrameRate.toLong(), mCurrentFrame)

        if(cTime > slideXEnd){
            //go to the next slide
            slideIndex++

            if (slideIndex >= mPages.size) {
                mIsVideoDone = true
            } else {
                slideAudioStart = slideAudioEnd
                slideAudioEnd += mPages[slideIndex].getDuration(mAudioTransitionUs)

                if (slideIndex + 1 < mPages.size) {
                    nSlideAudioEnd = slideAudioEnd + mPages[slideIndex + 1].getDuration(mAudioTransitionUs)
                }
            }
        }

        drawFrame(canv, slideIndex, cTime - slideVisStart, slideVisDur,
                1f)

        if (cTime >= slideXStart) {
            var alpha = (cTime - slideXStart) / xTime.toFloat()
            if(cTime < xTime.toFloat()/2)
                alpha = 1.0f
            drawFrame(canv, slideIndex + 1, cTime - slideXStart, nSlideVisDur,
                    alpha)
        }

        //clear image cache to save memory.
        if(slideIndex >= 1 && slideIndex < mPages.size) {
            if (bitmaps.containsKey(mPages[slideIndex - 1].imRelPath) &&
                    mPages[slideIndex - 1].imRelPath != mPages[slideIndex].imRelPath) {
                bitmaps.remove(mPages[slideIndex - 1].imRelPath)
            }
        }

        mCurrentFrame++

        return cTime
    }

    private fun drawFrame(canv: Canvas, pageIndex: Int, timeOffsetUs: Long, imgDurationUs: Long,
                          alpha: Float) {
        //In edge cases, draw a black frame with alpha value.
        if (pageIndex < 0 || pageIndex >= mPages.size) {
            canv.drawARGB((alpha * 255).toInt(), 0, 0, 0)
            return
        }

        val page = mPages[pageIndex]
        if(!bitmaps.containsKey(page.imRelPath)){
            val ds = getDownsample(context,page.imRelPath,mWidth*2, mHeight*2)
            downsamples[page.imRelPath] = ds
            bitmaps[page.imRelPath] = SlideService(context).getImage(page.imRelPath, ds, true, Workspace.activeStory)
        }
        val bitmap = bitmaps[page.imRelPath]
        val downSample = downsamples[page.imRelPath]!!

        if (bitmap != null) {
            val position = (timeOffsetUs / imgDurationUs.toDouble()).toFloat()

            //If ken burns, then interpolate
            val drawRect = page.kenBurnsEffect?.
                    revInterpolate(position,mWidth,mHeight,bitmap.width,bitmap.height,downSample*1f) ?:
                //else, fit to crop the height and width to show everything.
                BitmapScaler.centerCropRectF(
                        bitmap.height, bitmap.width, mHeight, mWidth)

            mBitmapPaint.alpha = (alpha * 255).toInt()

            canv.drawBitmap(bitmap, null, drawRect, mBitmapPaint)
        } else {
            //If there is no picture, draw black background for text overlay.
            canv.drawARGB((alpha * 255).toInt(), 0, 0, 0)
        }

        page.textOverlay.let{
            // 2/22/2022 - DKH, Issue 456: Add grey rectangle to backdrop text "sub titles"
            // If this is a NUMBEREDPAGE type slide, draw a background behind the text so
            // that it can be clearly read in the video that we are creating
            // Other pages (eg: NONE, FRONTCOVER, LOCALSONG, LOCALCREDITS, COPYRIGHT, ENDPAGE),
            // do not need a background for the text
            if (page.sType == SlideType.NUMBEREDPAGE) it?.drawTextBG(true)
            it?.setAlpha(alpha)
            it?.draw(canv)
        }
    }

    override fun close() {
        bitmaps.clear()
    }

    companion object {
        private val TAG = "StoryFrameDrawer"
    }
}

package org.tyndalebt.spadv.tools.media.graphics

import android.graphics.Rect
import android.graphics.RectF
import org.tyndalebt.spadv.model.Slide

/**
 * This class encapsulates a single, un-timed Ken Burns effect for an image (Bitmap). In other words,
 * this class stores two crops of an image and provides help with intermediary crops.
 * Coordinates are stored as integer pixel values.
 */
class KenBurnsEffect
/**
 * Create Ken Burns effect with starting and ending rectangles whose values are relative to a given crop.
 * @param start starting crop of effect.
 * @param end ending crop of effect.
 * @param crop initial crop of image (start and end are relative to this).
 */
@JvmOverloads constructor(start: Rect, end: Rect, crop: Rect? = null) {

    private val mStart: Rect
    private val mEnd: Rect

    private val dLeft: Int
    private val dTop: Int
    private val dRight: Int
    private val dBottom: Int

    init {
        mStart = Rect(start)
        if (crop != null) {
            RectHelper.translate(mStart, crop.left, crop.top)
        }

        mEnd = Rect(end)
        if (crop != null) {
            RectHelper.translate(mEnd, crop.left, crop.top)
        }

        dLeft = mEnd.left - mStart.left
        dTop = mEnd.top - mStart.top
        dRight = mEnd.right - mStart.right
        dBottom = mEnd.bottom - mStart.bottom
    }

    /**
     * Obtain an intermediary crop from the Ken Burns effect.
     * @param position time-step between 0 and 1 (inclusive)
     * where 0 corresponds to the starting crop.
     * @return stretch of original image over screen size to make crop
     */
    fun revInterpolate(position: Float, scrWidth: Int, scrHeight: Int, imWidth: Int, imHeight: Int, downSample: Float): RectF {
        var pos = position
        //Clamp position to [0, 1]
        if (pos < 0) {
            pos = 0f
        } else if (pos > 1) {
            pos = 1f
        }

        //Start by calculating the "internal rectangle" that is stored in the Bloom file
        //This is where the screen is looking at the picture
        val irL = (mStart.left + pos * dLeft)/downSample
        val irT = (mStart.top + pos * dTop)/downSample
        val irR = (mStart.right + pos * dRight)/downSample
        val irB = (mStart.bottom + pos * dBottom)/downSample
        val irH = irB - irT
        val irW = irR - irL

        //now, lets invert it where the picture is stretched to be outside of the screen.
        val top = -irT/irH*scrHeight
        val bottom = ((imHeight-irB)/irH + 1)*scrHeight
        val left = -irL/irW*scrWidth
        val right = ((imWidth-irR)/irW + 1)*scrWidth

        return RectF(left, top, right, bottom)
    }


    companion object {
        fun fromSlide(slide: Slide) : KenBurnsEffect {
            val imageDimensions = Rect(0, 0, slide.width, slide.height)
            var start = slide.startMotion ?: imageDimensions
            start = RectHelper.clip(start,imageDimensions)
            var end = slide.endMotion ?: imageDimensions
            end = RectHelper.clip(end,imageDimensions)
            return KenBurnsEffect(start,end,slide.crop)
        }
    }
}

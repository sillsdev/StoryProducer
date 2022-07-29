package org.tyndalebt.spadv.tools.media.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

class TextOverlay(private val mText: String) {
    private var mFontSize = 18
    private var mAlpha = 1f
    private var mTextColor = Color.WHITE
    // 2/22/2022 - DKH, Issue 456: Add grey rectangle to backdrop text "sub titles"
    // 05/26/2022 - DKH Issue SPa3: Update subtitles for better readability.
    private var mTextBgColor = Color.BLUE  // BLUE - DKGRAY - Define the background color for rectangle behind text box
    // 05/26/2022 - DKH Issue SPa3: Update subtitles for better readability.
    // 192  = 75%
    // 204 = 80%
    // 212 = 83%
    // 230 = 90%
    // 255 = 100%
    private var mBgAlpha: Int = 192  // Background for Text transparency, 0-255, 255 is opaque
    private val mOutlineColor = Color.BLACK
    private var mPadding = 2
    private var mPaddingActual: Int = 0
    private var mHorizontalAlign: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    private var mVerticalAlign: Layout.Alignment = Layout.Alignment.ALIGN_CENTER

    private var mIsDirty = true

    private var mCanvasWidth: Int = 0
    private var mCanvasHeight: Int = 0

    private var mTextPaint: TextPaint? = null
    private var mTextOutlinePaint: TextPaint? = null
    private var mTextLayout: StaticLayout? = null
    private var mTextOutlineLayout: StaticLayout? = null
    private var mTextWidth: Int = 0
    private var mTextHeight: Int = 0

    private var mTranslateX: Float = 0.toFloat()
    private var mTranslateY: Float = 0.toFloat()

    // 2/22/2022 - DKH, Issue 456: Add rectangle to backdrop text "sub titles"
    private var mDrawTextBG: Boolean = false    // set default to false

    fun draw(canvas: Canvas) {
        if (mCanvasWidth != canvas.width || mCanvasHeight != canvas.height) {
            mCanvasWidth = canvas.width
            mCanvasHeight = canvas.height
            mIsDirty = true
        }

        if (mIsDirty) {
            setup()
        }
        canvas.save()
        canvas.translate(mTranslateX, mTranslateY)
        // 2/22/2022 - DKH, Issue 456: Add  rectangle to backdrop text "sub titles"
        // Determine if we need to draw a background text box.  mDrawTextBG is set to true during
        // video creation.
        if (mText.length > 0 && mDrawTextBG) {
            // Update a few values in the text box definition for drawing the rectangle
            val mAlpha = mTextLayout!!.paint.getAlpha() // grab the current alpha
            mTextLayout!!.paint.color = mTextBgColor    // set the rectangle background color
            mTextLayout!!.paint.setAlpha(mBgAlpha)      // set the amount of transparency

            // draw a rectangle behind the text
            canvas.drawRect(0.0F, 0.0F, mCanvasWidth.toFloat(), mCanvasHeight.toFloat(), mTextLayout!!.paint);
            // reset the updated values
            mTextLayout!!.paint.color = mTextColor      // reset back to the text color
            mTextLayout!!.paint.setAlpha(mAlpha)        // reset to original alpha
            
            mDrawTextBG = false  // reset to default of no background color
        }
        // 05/26/2022 - DKH Issue SPa3: Update subtitles for better readability. Delete outline
        // mTextOutlineLayout!!.draw(canvas)

        mTextLayout!!.draw(canvas)
        canvas.restore()
    }

    /**
     * Set the **relative** font size for the text.
     * @param fontSize
     */
    fun setFontSize(fontSize: Int) {
        mFontSize = fontSize
        mIsDirty = true
    }

    // 2/22/2022 - DKH, Issue 456: Add  rectangle to backdrop text "sub titles"
    // Accessor function to set the drawing of a background for the text
    // True means draw a background.
    fun drawTextBG (newVal : Boolean) {
        mDrawTextBG = newVal
    }

    fun setAlpha(alpha: Float) {
        mAlpha = alpha
        mIsDirty = true
        if (mTextPaint != null && mTextOutlinePaint != null) {
            mTextPaint!!.alpha = (mAlpha * 255).toInt()
            mTextOutlinePaint!!.alpha = (mAlpha * 255).toInt()
        }
    }

    fun setPadding(padding: Int) {
        mPadding = padding
        mIsDirty = true
    }

    fun setVerticalAlign(align: Layout.Alignment) {
        mVerticalAlign = align
        mIsDirty = true
    }

    fun setHorizontalAlign(align: Layout.Alignment) {
        mHorizontalAlign = align
        mIsDirty = true
    }

    private fun setup() {
        val fontSizeScale = mCanvasHeight / FONT_SIZE_SCALE_FACTOR.toFloat()

        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.color = mTextColor
        mTextPaint!!.alpha = (mAlpha * 255).toInt()
        mTextPaint!!.textSize = mFontSize * fontSizeScale
        // 05/26/2022 - DKH, Issue SPa3: For readability, change font to bold
        mTextPaint!!.setTypeface(Typeface.DEFAULT_BOLD)

        mTextOutlinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextOutlinePaint!!.color = mOutlineColor
        mTextOutlinePaint!!.alpha = (mAlpha * 255).toInt()
        mTextOutlinePaint!!.textSize = mTextPaint!!.textSize
        mTextOutlinePaint!!.style = Paint.Style.STROKE
        mTextOutlinePaint!!.strokeWidth = mFontSize.toFloat() * 0.1f * fontSizeScale
        mPaddingActual = (mPadding * fontSizeScale).toInt()

        //Set text width to canvas width minus padding.
        mTextWidth = mCanvasWidth - 2 * mPaddingActual

        //text
        //TODO switch to StaticLayout.Builder.obtain when switching to API23.
        //
        // 05/26/2022 - DKH Issue SPa3: Update subtitles for better readability. Add more spacing
        // between the lines (spacingmult).  Originally was 0.8f
        mTextLayout = StaticLayout(mText, mTextPaint, mTextWidth,
                mHorizontalAlign, 1.1f, 0.0f, false)
        //text outline
        mTextOutlineLayout = StaticLayout(mText, mTextOutlinePaint, mTextWidth,
                mHorizontalAlign, 1.1f, 0.0f, false)

        //Get height of multiline text.
        mTextHeight = mTextLayout!!.height


        when (mHorizontalAlign) {
            Layout.Alignment.ALIGN_OPPOSITE -> mTranslateX = (mCanvasWidth - mTextWidth - mPaddingActual).toFloat()
            Layout.Alignment.ALIGN_CENTER -> mTranslateX = ((mCanvasWidth - mTextWidth) / 2).toFloat()
            Layout.Alignment.ALIGN_NORMAL -> mTranslateX = mPaddingActual.toFloat()
            else -> mTranslateX = mPaddingActual.toFloat()
        }

        when (mVerticalAlign) {
            Layout.Alignment.ALIGN_OPPOSITE -> mTranslateY = (mCanvasHeight - mTextHeight - mPaddingActual).toFloat()
            Layout.Alignment.ALIGN_CENTER -> mTranslateY = ((mCanvasHeight - mTextHeight) / 2).toFloat()
            Layout.Alignment.ALIGN_NORMAL -> mTranslateY = mPaddingActual.toFloat()
            else -> mTranslateY = mPaddingActual.toFloat()
        }

        mIsDirty = false
    }

    companion object {
        private val FONT_SIZE_SCALE_FACTOR = 240
    }
}


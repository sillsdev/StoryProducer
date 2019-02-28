package org.sil.storyproducer.tools.media.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

class TextOverlay(private val mText: String) {
    private var mFontSize = 18
    private var mAlpha = 1f
    private var mTextColor = Color.WHITE
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
        mTextOutlineLayout!!.draw(canvas)
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

    private fun setup() {
        val fontSizeScale = mCanvasHeight / FONT_SIZE_SCALE_FACTOR.toFloat()

        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.color = mTextColor
        mTextPaint!!.alpha = (mAlpha * 255).toInt()
        mTextPaint!!.textSize = mFontSize * fontSizeScale

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
        mTextLayout = StaticLayout(mText, mTextPaint, mTextWidth,
                mHorizontalAlign, 1.0f, 0.0f, false)
        //text outline
        mTextOutlineLayout = StaticLayout(mText, mTextOutlinePaint, mTextWidth,
                mHorizontalAlign, 1.0f, 0.0f, false)

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


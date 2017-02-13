package org.sil.storyproducer.tools.media.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class TextOverlay {
    private static final int FONT_SIZE_SCALE_FACTOR = 240;

    private final String mText;
    private int mFontSize = 20;
    private int mTextColor = Color.WHITE;
    private int mOutlineColor = Color.BLACK;
    private int mPadding = 8;
    private int mPaddingActual;
    private Layout.Alignment mHorizontalAlign = Layout.Alignment.ALIGN_CENTER;
    private Layout.Alignment mVerticalAlign = Layout.Alignment.ALIGN_CENTER;

    private boolean mIsDirty = true;

    private int mCanvasWidth;
    private int mCanvasHeight;

    private TextPaint mTextPaint;
    private TextPaint mTextOutlinePaint;
    private StaticLayout mTextLayout;
    private StaticLayout mTextOutlineLayout;
    private int mTextWidth;
    private int mTextHeight;

    private float mTranslateX;
    private float mTranslateY;

    public TextOverlay(String text) {
        mText = text;
    }

    public void draw(Canvas canvas) {
        if(mCanvasWidth != canvas.getWidth() || mCanvasHeight != canvas.getHeight()) {
            mCanvasWidth = canvas.getWidth();
            mCanvasHeight = canvas.getHeight();
            mIsDirty = true;
        }

        if(mIsDirty) {
            setup();
        }

        canvas.save();
        canvas.translate(mTranslateX, mTranslateY);
        mTextOutlineLayout.draw(canvas);
        mTextLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Set the <b>relative</b> font size for the text.
     * @param fontSize
     */
    public void setFontSize(int fontSize) {
        mFontSize = fontSize;
        mIsDirty = true;
    }

    public void setTextColor(int color) {
        mTextColor = color;
        mIsDirty = true;
    }

    public void setOutlineColor(int color) {
        mOutlineColor = color;
        mIsDirty = true;
    }

    public void setPadding(int padding) {
        mPadding = padding;
        mIsDirty = true;
    }

    public void setHorizontalAlign(Layout.Alignment align) {
        mHorizontalAlign = align;
        mIsDirty = true;
    }

    public void setVerticalAlign(Layout.Alignment align) {
        mVerticalAlign = align;
        mIsDirty = true;
    }

    private void setup() {
        float fontSizeScale = mCanvasHeight / (float) FONT_SIZE_SCALE_FACTOR;

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mFontSize * fontSizeScale);

        mTextOutlinePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextOutlinePaint.setColor(mOutlineColor);
        mTextOutlinePaint.setTextSize(mTextPaint.getTextSize());
        mTextOutlinePaint.setStyle(Paint.Style.STROKE);
        mTextOutlinePaint.setStrokeWidth(1.5f * fontSizeScale);

        mPaddingActual = (int) (mPadding * fontSizeScale);

        //Set text width to canvas width minus padding.
        mTextWidth = mCanvasWidth - 2 * mPaddingActual;

        //text
        mTextLayout = new StaticLayout(mText, mTextPaint, mTextWidth,
                mHorizontalAlign, 1.0f, 0.0f, false);
        //text outline
        mTextOutlineLayout = new StaticLayout(mText, mTextOutlinePaint, mTextWidth,
                mHorizontalAlign, 1.0f, 0.0f, false);

        //Get height of multiline text.
        mTextHeight = mTextLayout.getHeight();


        switch(mHorizontalAlign) {
            case ALIGN_OPPOSITE:
                mTranslateX = mCanvasWidth - mTextWidth - mPaddingActual;
                break;
            case ALIGN_CENTER:
                mTranslateX = (mCanvasWidth - mTextWidth) / 2;
                break;
            case ALIGN_NORMAL:
            default:
                mTranslateX = mPaddingActual;
        }

        switch(mVerticalAlign) {
            case ALIGN_OPPOSITE:
                mTranslateY = mCanvasHeight - mTextHeight - mPaddingActual;
                break;
            case ALIGN_CENTER:
                mTranslateY = (mCanvasHeight - mTextHeight) / 2;
                break;
            case ALIGN_NORMAL:
            default:
                mTranslateY = mPaddingActual;
        }

        mIsDirty = false;
    }
}

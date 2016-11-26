package org.sil.storyproducer.media;

import android.graphics.Rect;

public class KenBurnsEffect {
    public enum Easing {
        LINEAR,
        ;
    }

    private Rect mStart;
    private Rect mEnd;

    private Easing mEasing;

    private int mStartX, mStartY, mStartWidth, mStartHeight;
    private int mEndX, mEndY, mEndWidth, mEndHeight;

    private int dx, dy, dWidth, dHeight;

    public KenBurnsEffect(Rect start, Rect end) {
        mStart = start;
        mEnd = end;

        mEasing = Easing.LINEAR;

        mStartX = mStart.left;
        mStartY = mStart.top;
        mStartWidth = mStart.width();
        mStartHeight = mStart.height();

        mEndX = mEnd.left;
        mEndY = mEnd.top;
        mEndWidth = mEnd.width();
        mEndHeight = mEnd.height();

        dx = mEndX - mStartX;
        dy = mEndY - mStartY;
        dWidth = mEndWidth - mStartWidth;
        dHeight = mEndHeight - mStartHeight;
    }

    public Rect interpolate(float position) {
        //Clamp position to [0, 1]
        if(position <= 0) {
            position = 0;
        }
        else if(position >= 1) {
            position = 1;
        }

        int left, top, right, bottom;

        switch(mEasing) {
            case LINEAR:
                //Fall through to default case.
            default: //default to linear
                left = (int) (mStartX + position * dx);
                top = (int) (mStartY + position * dy);
                right = (int) (left + mStartWidth + position * dWidth);
                bottom = (int) (top + mStartHeight + position * dHeight);
                break;
        }

        return new Rect(left, top, right, bottom);
    }
}

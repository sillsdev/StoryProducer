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

    private int dLeft, dTop, dRight, dBottom, dWidth, dHeight;

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

        dLeft = mEnd.left - mStart.left;
        dTop = mEnd.top - mStart.top;
        dRight = mEnd.right - mStart.right;
        dBottom = mEnd.bottom - mStart.bottom;
        dWidth = mEndWidth - mStartWidth;
        dHeight = mEndHeight - mStartHeight;
    }

    public Rect interpolate(float position) {
        //Clamp position to [0, 1]
        if(position < 0) {
            position = 0;
        }
        else if(position > 1) {
            position = 1;
        }

        int left, top, right, bottom;

        switch(mEasing) {
            case LINEAR:
                //Fall through to default case.
            default: //default to linear
                left = mStart.left + (int) (position * dLeft);
                top = mStart.top + (int) (position * dTop);
                right = mStart.right + (int) (position * dRight);
                bottom = mStart.bottom + (int) (position * dBottom);
//                right = left + mStartWidth + (int) (position * dWidth);
//                bottom = top + mStartHeight + (int) (position * dHeight);
                break;
        }

//        if(right > left) {
//            right--;
//        }
//        if(bottom > top) {
//            bottom--;
//        }

        return new Rect(left, top, right, bottom);
    }
}

package org.sil.storyproducer.media;

import android.graphics.Rect;

import org.sil.storyproducer.graphics.RectHelper;

public class KenBurnsEffect {
    public enum Easing {
        LINEAR,
        ;
    }

    private Rect mStart;
    private Rect mEnd;

    private Easing mEasing;

    private int dLeft, dTop, dRight, dBottom;

    public KenBurnsEffect(Rect start, Rect end) {
        this(start, end, null);
    }

    public KenBurnsEffect(Rect start, Rect end, Rect crop) {
        mStart = new Rect(start);
        if(crop != null) {
            RectHelper.translate(mStart, crop.left, crop.top);
        }

        mEnd = new Rect(end);
        if(crop != null) {
            RectHelper.translate(mEnd, crop.left, crop.top);
        }

        mEasing = Easing.LINEAR;

        dLeft = mEnd.left - mStart.left;
        dTop = mEnd.top - mStart.top;
        dRight = mEnd.right - mStart.right;
        dBottom = mEnd.bottom - mStart.bottom;
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
                break;
        }

        return new Rect(left, top, right, bottom);
    }
}

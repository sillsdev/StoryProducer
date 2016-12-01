package org.sil.storyproducer.media;

import android.graphics.Rect;

import org.sil.storyproducer.media.graphics.RectHelper;

/**
 * This class encapsulates a single, un-timed Ken Burns effect for an image (Bitmap). In other words,
 * this class stores two crops of an image and provides help with intermediary crops.
 * Coordinates are stored as integer pixel values.
 */
public class KenBurnsEffect {
    public enum Easing {
        LINEAR,
        ;
    }

    private Rect mStart;
    private Rect mEnd;

    private Easing mEasing;

    private int dLeft, dTop, dRight, dBottom;

    /**
     * Create Ken Burns effect with starting and ending rectangles.
     * @param start starting crop of effect.
     * @param end ending crop of effect.
     */
    public KenBurnsEffect(Rect start, Rect end) {
        this(start, end, null);
    }

    /**
     * Create Ken Burns effect with starting and ending rectangles whose values are relative to a given crop.
     * @param start starting crop of effect.
     * @param end ending crop of effect.
     * @param crop initial crop of image (start and end are relative to this).
     */
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

    /**
     * Obtain an intermediary crop from the Ken Burns effect.
     * @param position time-step between 0 and 1 (inclusive)
     *                 where 0 corresponds to the starting crop.
     * @return crop at time-step.
     */
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

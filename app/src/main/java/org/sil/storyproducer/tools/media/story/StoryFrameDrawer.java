package org.sil.storyproducer.tools.media.story;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFormat;
import android.text.Layout;
import android.util.Log;

import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;
import org.sil.storyproducer.tools.media.graphics.TextOverlay;
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.tools.media.pipe.SourceUnacceptableException;

import java.io.IOException;

/**
 * This class knows how to draw the frames provided to it by {@link StoryMaker}.
 */
class StoryFrameDrawer implements PipedVideoSurfaceEncoder.Source {
    private static final String TAG = "StoryFrameDrawer";

    private final MediaFormat mVideoFormat;
    private final StoryPage[] mPages;
    private final long mAudioTransitionUs;
    private final long mSlideTransitionUs;

    private final int mFrameRate;

    private final int mWidth;
    private final int mHeight;
    private final Rect mScreenRect;

    private final Paint mBitmapPaint;

    private TextOverlay mTextOverlay;

    private int mCurrentSlideIndex = -1; //starts at -1 to allow initial transition
    private long mCurrentSlideExDuration = 0; //exclusive duration of current slide
    private long mCurrentSlideStart = 0; //time (after transition) of audio start

    private long mCurrentSlideImgDuration = 0; //total duration of current slide image (cached for performance)
    private long mNextSlideImgDuration = 0; //total duration of next slide image (cached for performance)

    private int mCurrentFrame = 0;

    private boolean mIsVideoDone = false;

    StoryFrameDrawer(MediaFormat videoFormat, StoryPage[] pages, long audioTransitionUs, long slideTransitionUs) {
        mVideoFormat = videoFormat;
        mPages = pages;

        mAudioTransitionUs = audioTransitionUs;

        long correctedSlideTransitionUs = slideTransitionUs;

        //mSlideTransition must never exceed the length of slides in terms of audio.
        //Pre-process pages and clip the slide transition time to fit in all cases.
        for(StoryPage page : pages) {
            long totalPageUs = page.getDuration() + mAudioTransitionUs;
            if(correctedSlideTransitionUs > totalPageUs) {
                correctedSlideTransitionUs = totalPageUs;
                Log.d(TAG, "Corrected slide transition from " + slideTransitionUs + " to " + correctedSlideTransitionUs);
            }
        }

        mSlideTransitionUs = correctedSlideTransitionUs;

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mScreenRect = new Rect(0, 0, mWidth, mHeight);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.VIDEO;
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mVideoFormat;
    }

    @Override
    public boolean isDone() {
        return mIsVideoDone;
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        if(mPages.length > 0) {
            mNextSlideImgDuration = mPages[0].getDuration() + 2 * mSlideTransitionUs;
        }
    }

    @Override
    public long fillCanvas(Canvas canv) {
        long currentTimeUs = MediaHelper.getTimeFromIndex(mFrameRate, mCurrentFrame);

        long nextSlideTransitionUs = mSlideTransitionUs;
        //For pre-first "slide" and last slide, make the transition half as long.
        if(mCurrentSlideIndex == -1 || mCurrentSlideIndex == mPages.length - 1) {
            nextSlideTransitionUs /= 2;
        }

        long nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideExDuration;

        while(currentTimeUs > nextSlideTransitionStartUs + nextSlideTransitionUs) {
            mCurrentSlideIndex++;

            if(mCurrentSlideIndex >= mPages.length) {
                mIsVideoDone = true;
                break;
            }

            StoryPage currentPage = mPages[mCurrentSlideIndex];

            mCurrentSlideStart = mCurrentSlideStart + mCurrentSlideExDuration + nextSlideTransitionUs;
            mCurrentSlideExDuration = currentPage.getDuration() + mAudioTransitionUs - mSlideTransitionUs;
            mCurrentSlideImgDuration = mNextSlideImgDuration;
            nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideExDuration;

            if(mCurrentSlideIndex + 1 < mPages.length) {
                mNextSlideImgDuration = mPages[mCurrentSlideIndex + 1].getDuration() + 2 * mSlideTransitionUs;
            }

            String text = currentPage.getText();
            if(text == null) {
                mTextOverlay = null;
            }
            else {
                mTextOverlay = new TextOverlay(text);
                mTextOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE);
            }

        }

        long timeSinceCurrentSlideStartUs = currentTimeUs - mCurrentSlideStart;
        long currentSlideOffsetUs = timeSinceCurrentSlideStartUs + mSlideTransitionUs;

        drawFrame(canv, mCurrentSlideIndex, currentSlideOffsetUs, mCurrentSlideImgDuration, 1);
        if(mTextOverlay != null) {
            mTextOverlay.draw(canv);
        }


        if(currentTimeUs > nextSlideTransitionStartUs) {
            mTextOverlay = null;

            long timeSinceTransitionStartUs = currentTimeUs - nextSlideTransitionStartUs;
            long extraOffsetUs = mSlideTransitionUs - nextSlideTransitionUs; //0 normally, transition/2 for edge cases
            long nextOffsetUs = timeSinceTransitionStartUs + extraOffsetUs;
            drawFrame(canv, mCurrentSlideIndex + 1, nextOffsetUs, mNextSlideImgDuration, nextOffsetUs / (float) nextSlideTransitionUs);
        }

        mCurrentFrame++;

        return currentTimeUs;
    }

    private void drawFrame(Canvas canv, int pageIndex, long timeOffsetUs, long imgDurationUs, float alpha) {
        //In edge cases, draw a black frame with alpha value.
        if(pageIndex < 0 || pageIndex >= mPages.length) {
            canv.drawARGB((int) (alpha * 255), 0, 0, 0);
            return;
        }

        StoryPage page = mPages[pageIndex];
        Bitmap bitmap = page.getBitmap();
        KenBurnsEffect kbfx = page.getKenBurnsEffect();

        float position = (float) (timeOffsetUs / (double) imgDurationUs);
        Rect drawRect;
        if(kbfx != null) {
            drawRect = kbfx.interpolate(position);
        }
        else {
            drawRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        if (MediaHelper.DEBUG) {
            Log.d(TAG, "drawing bitmap (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ") from "
                    + drawRect + " to canvas " + mScreenRect);
        }

        mBitmapPaint.setAlpha((int) (alpha * 255));

        canv.drawBitmap(bitmap, drawRect, mScreenRect, mBitmapPaint);
    }

    @Override
    public void close() {
        //Do nothing.
    }
}

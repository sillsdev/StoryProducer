package org.sil.storyproducer.tools.media.story;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFormat;
import android.util.Log;

import org.sil.storyproducer.tools.media.KenBurnsEffect;
import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceSource;
import org.sil.storyproducer.tools.media.pipe.SourceUnacceptableException;

import java.io.IOException;

/**
 * This class knows how to draw the frames provided to it by {@link StoryMaker}.
 */
//TODO: use slide transition
class StoryFrameDrawer implements PipedVideoSurfaceSource {
    private static final String TAG = "StoryFrameDrawer";

    private MediaFormat mVideoFormat;
    private StoryPage[] mPages;
    private long mAudioTransitionUs;
    private long mSlideTransitionUs;

    private int mCurrentSlideIndex = -1; //starts at -1 to allow initial transition
    private long mCurrentSlideDuration = 0; //duration of audio
    private long mCurrentSlideStart = 0; //time (after transition) of audio start

    private int mFrameRate;

    private int mCurrentFrame = 0;

    private int mWidth;
    private int mHeight;
    private Rect mScreenRect;

    private boolean mIsVideoDone = false;

    StoryFrameDrawer(MediaFormat videoFormat, StoryPage[] pages, long audioTransitionUs, long slideTransitionUs) {
        mVideoFormat = videoFormat;
        mPages = pages;

        mAudioTransitionUs = audioTransitionUs;
        mSlideTransitionUs = slideTransitionUs;

        //mSlideTransition must never exceed the length of slides in terms of audio.
        //Pre-process pages and clip the slide transition time to fit in all cases.
        for(StoryPage page : pages) {
            long totalPageUs = page.getAudioDuration() + mAudioTransitionUs;
            if(mSlideTransitionUs > totalPageUs) {
                mSlideTransitionUs = totalPageUs;
            }
        }

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mScreenRect = new Rect(0, 0, mWidth, mHeight);
    }

    private void drawFrame(Canvas canv, int pageIndex, long timeOffsetUs, float alpha) {
        //In edge cases, draw a black frame with alpha value.
        if(pageIndex < 0 || pageIndex >= mPages.length) {
            canv.drawARGB((int) (alpha * 255), 0, 0, 0);
            return;
        }

        StoryPage page = mPages[pageIndex];
        Bitmap bitmap = page.getBitmap();
        long durationUs = page.getAudioDuration() + 2 * mSlideTransitionUs;
        KenBurnsEffect kbfx = page.getKenBurnsEffect();

        float position = (float) (timeOffsetUs / (double) durationUs);
        Rect drawRect = kbfx.interpolate(position);

        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "drawer: drawing rectangle (" + drawRect.left + ", " + drawRect.top + ", "
                    + drawRect.right + ", " + drawRect.bottom + ") of bitmap ("
                    + bitmap.getWidth() + ", " + bitmap.getHeight() + ")");
        }

        Paint p = new Paint(0);

        //TODO: Should we use these flags?
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        p.setAlpha((int) (alpha * 255));

        canv.drawBitmap(bitmap, drawRect, mScreenRect, p);
    }

    @Override
    public long fillCanvas(Canvas canv) {
        long currentTimeUs = MediaHelper.getTimeFromIndex(mFrameRate, mCurrentFrame);

        long nextSlideTransitionUs = mSlideTransitionUs;
        //For pre-first "slide" and last slide, make the transition half as long.
        if(mCurrentSlideIndex == -1 || mCurrentSlideIndex == mPages.length - 1) {
            nextSlideTransitionUs /= 2;
        }

        long nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideDuration;

        while(currentTimeUs > nextSlideTransitionStartUs + nextSlideTransitionUs) {
            mCurrentSlideIndex++;

            if(mCurrentSlideIndex >= mPages.length) {
                mIsVideoDone = true;
                break;
            }

            mCurrentSlideStart = mCurrentSlideStart + mCurrentSlideDuration + nextSlideTransitionUs;
            mCurrentSlideDuration = mPages[mCurrentSlideIndex].getAudioDuration() + mAudioTransitionUs - mSlideTransitionUs;

            nextSlideTransitionStartUs = mCurrentSlideStart + mCurrentSlideDuration;
        }

        long timeSinceCurrentSlideStartUs = currentTimeUs - mCurrentSlideStart;
        long currentSlideOffsetUs = timeSinceCurrentSlideStartUs + mSlideTransitionUs;

        drawFrame(canv, mCurrentSlideIndex, currentSlideOffsetUs, 1);

        if(currentTimeUs > nextSlideTransitionStartUs) {
            long timeSinceTransitionStartUs = currentTimeUs - nextSlideTransitionStartUs;
            long extraOffsetUs = mSlideTransitionUs - nextSlideTransitionUs; //0 normally, transition/2 for edge cases
            long nextOffsetUs = timeSinceTransitionStartUs + extraOffsetUs;
            drawFrame(canv, mCurrentSlideIndex + 1, nextOffsetUs, nextOffsetUs / (float) nextSlideTransitionUs);
        }

        mCurrentFrame++;

        return currentTimeUs;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.VIDEO;
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        //Do nothing.
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
    public void close() {
        //Do nothing.
    }
}

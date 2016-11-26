package org.sil.storyproducer.media.videostory;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFormat;

import org.sil.storyproducer.media.KenBurnsEffect;
import org.sil.storyproducer.media.MediaHelper;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceSource;
import org.sil.storyproducer.media.pipe.SourceUnacceptableException;

import java.io.IOException;

class VideoStoryDrawer implements PipedVideoSurfaceSource {
    private MediaFormat mVideoFormat;
    private StoryPage[] mPages;
    private long mDelayUs;

    private int mCurrentPageIndex = -1;
    private long mCurrentPageDuration = 0;
    private long mCurrentPageStart = 0;

    private int mFrameRate;

    private int mCurrentFrame = 0;

    private int mWidth;
    private int mHeight;
    private Rect mScreenRect;

    private boolean mIsVideoDone = false;

    VideoStoryDrawer(MediaFormat videoFormat, StoryPage[] pages, long delayUs) {
        mVideoFormat = videoFormat;
        mPages = pages;
        mDelayUs = delayUs;

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mScreenRect = new Rect(0, 0, mWidth, mHeight);
    }

    private void drawFrame(Canvas canv, int pageIndex, long timeOffset, float alpha) {
        if(pageIndex < 0 || pageIndex >= mPages.length) {
            canv.drawARGB((int) (alpha * 255), 0, 0, 0);
            return;
        }

        StoryPage page = mPages[pageIndex];
        Bitmap bitmap = page.getBitmap();
        long duration = page.getDuration() + 2 * mDelayUs;
        //TODO: use kbfx
        KenBurnsEffect kbfx = page.getKenBurnsEffect();

        Paint p = new Paint();
//        p.setAntiAlias(true);
//        p.setFilterBitmap(true);
//        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        p.setAlpha((int) (alpha * 255));

        float percent = (float) (timeOffset / (double) duration);
//        int x = (int) (percent * bitmap.getWidth());
//        int y = (int) (percent * bitmap.getHeight());
//        canv.drawBitmap(bitmap, new Rect(0, 0, x, y), mScreenRect, p);
        canv.drawBitmap(bitmap, kbfx.interpolate(percent), mScreenRect, p);
    }

    @Override
    public long fillCanvas(Canvas canv) {
        long currentTime = MediaHelper.getTimeFromIndex(mFrameRate, mCurrentFrame);

        while(currentTime > mCurrentPageStart + mCurrentPageDuration + mDelayUs) {
            mCurrentPageIndex++;

            if(mCurrentPageIndex >= mPages.length) {
                mIsVideoDone = true;
                break;
            }

            mCurrentPageStart = mCurrentPageStart + mCurrentPageDuration + mDelayUs;
            mCurrentPageDuration = mPages[mCurrentPageIndex].getDuration();
        }

        long currentOffset = currentTime - mCurrentPageStart + mDelayUs;

        drawFrame(canv, mCurrentPageIndex, currentOffset, 1);

        if(currentOffset > mCurrentPageDuration + mDelayUs) {
            long nextOffset = currentTime - mCurrentPageStart - mCurrentPageDuration;
            drawFrame(canv, mCurrentPageIndex + 1, nextOffset, nextOffset / (float) mDelayUs);
        }

        mCurrentFrame++;

        return currentTime;
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
    public void close() throws IOException {
        //Do nothing.
    }
}

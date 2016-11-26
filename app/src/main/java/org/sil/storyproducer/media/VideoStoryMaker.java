package org.sil.storyproducer.media;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFormat;

import org.sil.storyproducer.media.pipe.PipedAudioConcatenator;
import org.sil.storyproducer.media.pipe.PipedAudioDecoderMaverick;
import org.sil.storyproducer.media.pipe.PipedAudioMixer;
import org.sil.storyproducer.media.pipe.PipedAudioResampler;
import org.sil.storyproducer.media.pipe.PipedMediaDecoder;
import org.sil.storyproducer.media.pipe.PipedMediaEncoder;
import org.sil.storyproducer.media.pipe.PipedMediaExtractor;
import org.sil.storyproducer.media.pipe.PipedMediaMuxer;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceSource;
import org.sil.storyproducer.media.pipe.SourceUnacceptableException;

import java.io.File;
import java.io.IOException;

public class VideoStoryMaker implements PipedVideoSurfaceSource {
    private File mOutputFile;
    private int mOutputFormat;

    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private StoryPage[] mPages;
    private File mSoundTrack;
    private long mDelayUs;

    private int mCurrentPageIndex = -1;
    private long mCurrentPageDuration = 0;
    private long mCurrentPageStart = 0;

    private int mSampleRate;
    private int mChannelCount;

    private int mFrameRate;

    private int mCurrentFrame = 0;

    private int mWidth;
    private int mHeight;
    private Rect mScreenRect;

    private boolean mIsVideoDone = false;

    public VideoStoryMaker(File output, int outputFormat, MediaFormat videoFormat, MediaFormat audioFormat, StoryPage[] pages, File soundtrack, long delayUs) {
        mOutputFile = output;
        mOutputFormat = outputFormat;
        mVideoFormat = videoFormat;
        mAudioFormat = audioFormat;
        mPages = pages;
        mSoundTrack = soundtrack;
        mDelayUs = delayUs;

        mSampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mChannelCount = mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mFrameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

        mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        mScreenRect = new Rect(0, 0, mWidth, mHeight);
    }

    public void churn() {
        try {
            PipedAudioDecoderMaverick soundtrackMaverick = new PipedAudioDecoderMaverick(mSoundTrack.getPath(), mSampleRate, mChannelCount);
            PipedAudioConcatenator narrationConcatenator = new PipedAudioConcatenator(mDelayUs, mSampleRate, mChannelCount);
            PipedAudioMixer audioMixer = new PipedAudioMixer();
            PipedMediaEncoder audioEncoder = new PipedMediaEncoder(mAudioFormat);
            PipedVideoSurfaceEncoder videoEncoder = new PipedVideoSurfaceEncoder();
            PipedMediaMuxer muxer = new PipedMediaMuxer(mOutputFile.getPath(), mOutputFormat);

            muxer.addSource(audioEncoder);

            audioEncoder.addSource(audioMixer);
            audioMixer.addSource(soundtrackMaverick);
            audioMixer.addSource(narrationConcatenator);
            for (StoryPage page : mPages) {
                narrationConcatenator.addSource(page.getNarrationAudio().getPath());
            }

            muxer.addSource(videoEncoder);

            videoEncoder.addSource(this);

            muxer.crunch();
            System.out.println("muxer complete");
        }
        catch (IOException | SourceUnacceptableException | RuntimeException e) {
            e.printStackTrace();
        }
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
        p.setAlpha((int) (alpha * 255));

        float percent = (float) (timeOffset / (double) duration);
        int x = (int) (percent * bitmap.getWidth());
        int y = (int) (percent * bitmap.getHeight());
        canv.drawBitmap(bitmap, new Rect(0, 0, x, y), mScreenRect, p);
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
        //TODO
    }
}

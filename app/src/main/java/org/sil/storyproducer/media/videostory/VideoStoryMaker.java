package org.sil.storyproducer.media.videostory;

import android.media.MediaFormat;

import org.sil.storyproducer.media.pipe.PipedAudioConcatenator;
import org.sil.storyproducer.media.pipe.PipedAudioLooper;
import org.sil.storyproducer.media.pipe.PipedAudioMixer;
import org.sil.storyproducer.media.pipe.PipedMediaEncoder;
import org.sil.storyproducer.media.pipe.PipedMediaMuxer;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.media.pipe.SourceUnacceptableException;

import java.io.File;
import java.io.IOException;

public class VideoStoryMaker {
    private final File mOutputFile;
    private final int mOutputFormat;

    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private StoryPage[] mPages;
    private File mSoundTrack;

    private final long mAudioTransitionUs;
    private final long mSlideTransitionUs;

    private final int mSampleRate;
    private final int mChannelCount;

    private final long mDurationUs;

    public VideoStoryMaker(File output, int outputFormat, MediaFormat videoFormat, MediaFormat audioFormat,
                           StoryPage[] pages, File soundtrack, long audioTransitionUs, long slideTransitionUs) {
        mOutputFile = output;
        mOutputFormat = outputFormat;
        mVideoFormat = videoFormat;
        mAudioFormat = audioFormat;
        mPages = pages;
        mSoundTrack = soundtrack;

        mAudioTransitionUs = audioTransitionUs;
        mSlideTransitionUs = slideTransitionUs;

        mSampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mChannelCount = mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mDurationUs = getStoryDurationUs(mPages, mAudioTransitionUs);
    }

    public void churn() {
        try {
            PipedAudioLooper soundtrackMaverick = new PipedAudioLooper(mSoundTrack.getPath(), mDurationUs, mSampleRate, mChannelCount);
            PipedAudioConcatenator narrationConcatenator = new PipedAudioConcatenator(mAudioTransitionUs, mSampleRate, mChannelCount);
            PipedAudioMixer audioMixer = new PipedAudioMixer();
            PipedMediaEncoder audioEncoder = new PipedMediaEncoder(mAudioFormat);
            VideoStoryDrawer videoDrawer = new VideoStoryDrawer(mVideoFormat, mPages, mAudioTransitionUs, mSlideTransitionUs);
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

            videoEncoder.addSource(videoDrawer);

            muxer.crunch();
            System.out.println("muxer complete");
        }
        catch (IOException | SourceUnacceptableException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static long getStoryDurationUs(StoryPage[] pages, long audioTransitionUs) {
        long durationUs = pages.length * audioTransitionUs;

        for(StoryPage page : pages) {
            durationUs += page.getAudioDuration();
        }

        return durationUs;
    }
}

package org.sil.storyproducer.tools.media.story;

import android.media.MediaFormat;
import android.util.Log;

import org.sil.storyproducer.tools.media.pipe.PipedAudioConcatenator;
import org.sil.storyproducer.tools.media.pipe.PipedAudioLooper;
import org.sil.storyproducer.tools.media.pipe.PipedAudioMixer;
import org.sil.storyproducer.tools.media.pipe.PipedMediaEncoder;
import org.sil.storyproducer.tools.media.pipe.PipedMediaMuxer;
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.tools.media.pipe.SourceUnacceptableException;

import java.io.File;
import java.io.IOException;

/**
 * StoryMaker handles all the brunt work of constructing a media pipeline for a given set of StoryPages.
 */
public class StoryMaker {
    private static final String TAG = "StoryMaker";

    //TODO: revisit volume of soundtrack or add configuration
    private static final float SOUNDTRACK_VOLUME_MODIFIER = 0.8f;

    private final File mOutputFile;
    private final int mOutputFormat;

    private final MediaFormat mVideoFormat;
    private final MediaFormat mAudioFormat;
    private final StoryPage[] mPages;
    private final File mSoundTrack;

    private final long mAudioTransitionUs;
    private final long mSlideTransitionUs;

    private final int mSampleRate;
    private final int mChannelCount;

    private final long mDurationUs;

    private PipedMediaMuxer mMuxer;

    /**
     * Create StoryMaker.
     * @param output output video file.
     * @param outputFormat the format of the output media file
     *               (from {@link android.media.MediaMuxer.OutputFormat}).
     * @param videoFormat desired output video format.
     * @param audioFormat desired output audio format.
     * @param pages pages of this story.
     * @param soundtrack background music for this story.
     * @param audioTransitionUs transition duration, in microseconds, between narration segments.
     *                          Note: this helps drive length of video.
     * @param slideTransitionUs transition duration, in microseconds, of cross-fade between page images.
     */
    public StoryMaker(File output, int outputFormat, MediaFormat videoFormat, MediaFormat audioFormat,
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

        mDurationUs = getStoryDuration(mPages, mAudioTransitionUs);
    }

    /**
     * Set StoryMaker in motion. It is advisable to run this method from a separate thread.
     */
    public void churn() {
        PipedAudioLooper soundtrackLooper = new PipedAudioLooper(mSoundTrack.getPath(), mDurationUs, mSampleRate, mChannelCount);
        PipedAudioConcatenator narrationConcatenator = new PipedAudioConcatenator(mAudioTransitionUs, mSampleRate, mChannelCount);
        PipedAudioMixer audioMixer = new PipedAudioMixer();
        PipedMediaEncoder audioEncoder = new PipedMediaEncoder(mAudioFormat);
        StoryFrameDrawer videoDrawer = new StoryFrameDrawer(mVideoFormat, mPages, mAudioTransitionUs, mSlideTransitionUs);
        PipedVideoSurfaceEncoder videoEncoder = new PipedVideoSurfaceEncoder();
        mMuxer = new PipedMediaMuxer(mOutputFile.getPath(), mOutputFormat);

        try {
            mMuxer.addSource(audioEncoder);

            audioEncoder.addSource(audioMixer);
            audioMixer.addSource(soundtrackLooper, SOUNDTRACK_VOLUME_MODIFIER);
            audioMixer.addSource(narrationConcatenator);
            for (StoryPage page : mPages) {
                narrationConcatenator.addSource(page.getNarrationAudio().getPath());
            }

            mMuxer.addSource(videoEncoder);

            videoEncoder.addSource(videoDrawer);

            mMuxer.crunch();
            System.out.println("Video saved to " + mOutputFile);
        }
        catch (IOException | SourceUnacceptableException | RuntimeException e) {
            Log.d(TAG, "Error in story making", e);
        }
        finally {
            //Everything should be closed automatically, but close everything just in case.
            soundtrackLooper.close();
            narrationConcatenator.close();
            audioMixer.close();
            audioEncoder.close();
            videoDrawer.close();
            videoEncoder.close();
            mMuxer.close();
        }
    }

    public long getStoryDuration() {
        return mDurationUs;
    }

    /**
     * Get the expected duration, in microseconds, of the produced video.
     * This value should be accurate to a few milliseconds for arbitrarily long stories.
     * @param pages pages of this story.
     * @param audioTransitionUs transition duration, in microseconds, between narration segments.
     * @return expected duration of the produced video in microseconds.
     */
    public static long getStoryDuration(StoryPage[] pages, long audioTransitionUs) {
        long durationUs = pages.length * audioTransitionUs;

        for(StoryPage page : pages) {
            durationUs += page.getAudioDuration();
        }

        return durationUs;
    }

    public double getProgress() {
        if(mMuxer != null) {
            long audioProgress = mMuxer.getAudioProgress();
            long videoProgress = mMuxer.getVideoProgress();

            long minProgress = Math.min(audioProgress, videoProgress);

            return minProgress / (double) mDurationUs;
        }
        return 0;
    }

    public double getAudioProgress() {
        if(mMuxer != null) {
            long audioProgress = mMuxer.getAudioProgress();
            return audioProgress / (double) mDurationUs;
        }
        return 0;
    }

    public double getVideoProgress() {
        if(mMuxer != null) {
            long videoProgress = mMuxer.getVideoProgress();
            return videoProgress / (double) mDurationUs;
        }
        return 0;
    }
}

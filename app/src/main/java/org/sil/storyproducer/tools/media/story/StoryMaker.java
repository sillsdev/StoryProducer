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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * StoryMaker handles all the brunt work of constructing a media pipeline for a given set of StoryPages.
 */
public class StoryMaker implements Closeable {
    private static final String TAG = "StoryMaker";

    private float mSoundtrackVolumeModifier = 0.8f;

    private final File mOutputFile;
    private final int mOutputFormat;

    private final MediaFormat mVideoFormat;
    private final MediaFormat mAudioFormat;
    private final StoryPage[] mPages;
    private final File mSoundtrack;

    private final long mAudioTransitionUs;
    private final long mSlideTransitionUs;

    private final int mSampleRate;
    private final int mChannelCount;

    private final long mDurationUs;

    private PipedMediaMuxer mMuxer;
    private boolean mIsDone = false;

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
        mSoundtrack = soundtrack;

        mAudioTransitionUs = audioTransitionUs;
        mSlideTransitionUs = slideTransitionUs;

        mSampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mChannelCount = mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        mDurationUs = getStoryDuration(mPages, mAudioTransitionUs);
    }

    /**
     * Set the relative volume of the soundtrack to the narration for the video.
     * @param modifier between 0 (silent) and 1 (original volume)
     */
    public void setSoundtrackVolumeModifier(float modifier) {
        mSoundtrackVolumeModifier = modifier;
    }

    public boolean isDone() {
        return mIsDone;
    }

    /**
     * Set StoryMaker in motion. It is advisable to run this method from a separate thread.
     */
    public void churn() {
        if(mIsDone) {
            Log.e(TAG, "StoryMaker already finished!");
        }

        PipedAudioLooper soundtrackLooper = null;
        if(mSoundtrack != null) {
            soundtrackLooper = new PipedAudioLooper(mSoundtrack.getAbsolutePath(), mDurationUs, mSampleRate, mChannelCount);
        }
        PipedAudioConcatenator narrationConcatenator = new PipedAudioConcatenator(mAudioTransitionUs, mSampleRate, mChannelCount);
        PipedAudioMixer audioMixer = new PipedAudioMixer();
        PipedMediaEncoder audioEncoder = new PipedMediaEncoder(mAudioFormat);
        StoryFrameDrawer videoDrawer = new StoryFrameDrawer(mVideoFormat, mPages, mAudioTransitionUs, mSlideTransitionUs);
        PipedVideoSurfaceEncoder videoEncoder = new PipedVideoSurfaceEncoder();
        mMuxer = new PipedMediaMuxer(mOutputFile.getAbsolutePath(), mOutputFormat);

        try {
            mMuxer.addSource(audioEncoder);

            audioEncoder.addSource(audioMixer);
            if(soundtrackLooper != null) {
                audioMixer.addSource(soundtrackLooper, mSoundtrackVolumeModifier);
            }
            audioMixer.addSource(narrationConcatenator);
            for (StoryPage page : mPages) {
                File narration = page.getNarrationAudio();
                String path = narration != null ? narration.getAbsolutePath() : null;
                narrationConcatenator.addSource(path, page.getDuration());
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

        mIsDone = true;
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
            durationUs += page.getDuration();
        }

        return durationUs;
    }

    public double getProgress() {
        if(mIsDone) {
            return 1;
        }
        else if(mMuxer != null) {
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

    @Override
    public void close() {
        if(mMuxer != null) {
            mMuxer.close();
        }
        mIsDone = true;
    }
}

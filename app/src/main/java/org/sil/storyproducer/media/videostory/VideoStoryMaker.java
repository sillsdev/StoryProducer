package org.sil.storyproducer.media.videostory;

import android.media.MediaFormat;

import org.sil.storyproducer.media.pipe.PipedAudioConcatenator;
import org.sil.storyproducer.media.pipe.PipedAudioDecoderMaverick;
import org.sil.storyproducer.media.pipe.PipedAudioMixer;
import org.sil.storyproducer.media.pipe.PipedMediaEncoder;
import org.sil.storyproducer.media.pipe.PipedMediaMuxer;
import org.sil.storyproducer.media.pipe.PipedVideoSurfaceEncoder;
import org.sil.storyproducer.media.pipe.SourceUnacceptableException;

import java.io.File;
import java.io.IOException;

public class VideoStoryMaker {
    private File mOutputFile;
    private int mOutputFormat;

    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private StoryPage[] mPages;
    private File mSoundTrack;
    private long mDelayUs;

    private int mSampleRate;
    private int mChannelCount;

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
    }

    public void churn() {
        try {
            PipedAudioDecoderMaverick soundtrackMaverick = new PipedAudioDecoderMaverick(mSoundTrack.getPath(), mSampleRate, mChannelCount);
            PipedAudioConcatenator narrationConcatenator = new PipedAudioConcatenator(mDelayUs, mSampleRate, mChannelCount);
            PipedAudioMixer audioMixer = new PipedAudioMixer();
            PipedMediaEncoder audioEncoder = new PipedMediaEncoder(mAudioFormat);
            VideoStoryDrawer videoDrawer = new VideoStoryDrawer(mVideoFormat, mPages, mDelayUs);
            PipedVideoSurfaceEncoder videoEncoder = new PipedVideoSurfaceEncoder();
            PipedMediaMuxer muxer = new PipedMediaMuxer(mOutputFile.getPath(), mOutputFormat);

//            muxer.addSource(audioEncoder);

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
}

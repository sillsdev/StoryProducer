package org.sil.storyproducer.tools.media.story;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.tools.FileSystem;
import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffectHelper;

import java.io.File;

public class AutoStoryMaker extends Thread {
    private static final String TAG = "SampleStory";

    private final String mStory;

    // size of a frame, in pixels
    // first size isn't great quality, but runs faster
    private int mWidth = 320;
    private int mHeight = 240;
//    private int mWidth = 1280;
//    private int mHeight = 720;

    private static final long SLIDE_TRANSITION_US = 3000000;
    private static final long AUDIO_TRANSITION_US = 500000;

    private String mOutputExt = "mp4";
    private File mOutputDir;
    private File mOutputFile;

    // parameters for the video encoder
    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int VIDEO_FRAME_RATE = 30;               // 30fps
    private static final int VIDEO_IFRAME_INTERVAL = 1;           // 1 second between I-frames

    // using Kush Gauge for video bit rate
    private static final int MOTION_FACTOR = 4;                   // 1, 2, or 4
    private static final float KUSH_GAUGE_CONSTANT = 0.07f;
    // bits per second for video
    private int mVideoBitRate;

    // parameters for the audio encoder
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 48000;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_BIT_RATE = 64000;

    private final File mSoundtrack;

    private boolean mIncludeBackgroundMusic = true;
    private boolean mIncludePictures = true;
    private boolean mIncludeText = false;
    private boolean mIncludeKBFX = true;

    public AutoStoryMaker(String story) {
        mStory = story;

        setResolution(mWidth, mHeight);

        mOutputDir = FileSystem.getProjectDirectory("Fiery Furnace");
        mOutputFile = new File(mOutputDir, mWidth + "x" + mHeight + "." + mOutputExt);

        mSoundtrack = FileSystem.getSoundtrackAudio(mStory, 0);
    }

    public void setExtension(String extension) {
        mOutputExt = extension;
    }

    public void setFilePath(String filePath) {
        mOutputFile = new File(filePath);
    }

    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;

        int pixelRate = mWidth * mHeight * VIDEO_FRAME_RATE;
        mVideoBitRate = (int) (pixelRate * MOTION_FACTOR * KUSH_GAUGE_CONSTANT);
    }

    public void toggleBackgroundMusic(boolean includeBackgroundMusic) {
        mIncludeBackgroundMusic = includeBackgroundMusic;
    }

    public void togglePictures(boolean includePictures) {
        mIncludePictures = includePictures;
    }

    public void toggleText(boolean includeText) {
        mIncludeText = includeText;
    }

    public void toggleKenBurns(boolean includeKBFX) {
        mIncludeKBFX = includeKBFX;
    }

    private StoryMaker mStoryMaker;

    @Override
    public void run() {
        System.out.println("Starting to make story...");
        int outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

        MediaFormat audioFormat = generateAudioFormat();
        MediaFormat videoFormat = generateVideoFormat();
        StoryPage[] pages = generatePages();

        long duration = -System.currentTimeMillis();

        mStoryMaker = new StoryMaker(mOutputFile, outputFormat, videoFormat, audioFormat,
                pages, mSoundtrack, AUDIO_TRANSITION_US, SLIDE_TRANSITION_US);

        watchProgress();
        mStoryMaker.churn();

        duration += System.currentTimeMillis();

        Log.i(TAG, "Stopped making story after "
                + MediaHelper.getDecimal(duration / (double) 1000) + " seconds");
    }

    public double getProgress() {
        if(mStoryMaker == null) {
            return 0;
        }
        else {
            return mStoryMaker.getProgress();
        }
    }

    private MediaFormat generateAudioFormat() {
        MediaFormat audioFormat = MediaHelper.createFormat(AUDIO_MIME_TYPE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AUDIO_SAMPLE_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT);

        return audioFormat;
    }
    private MediaFormat generateVideoFormat() {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        return videoFormat;
    }

    private StoryPage[] generatePages() {
        //TODO: get actual slide count
        int slideCount = 2;
        //TODO: add copyright and hymn to count
        StoryPage[] pages = new StoryPage[slideCount];

        double widthToHeight = mWidth / (double) mHeight;

        for(int iSlide = 0; iSlide < slideCount; iSlide++) {
            File image = FileSystem.getImageFile(mStory, iSlide);
            File audio = FileSystem.getTranslationAudio(mStory, iSlide);
            //fallback to LWC narration
            //TODO: actually fallback
//            if(!audio.exists()) {
                audio = FileSystem.getNarrationAudio(mStory, iSlide);
//            }
            //TODO: get actual KBFX
            KenBurnsEffect kbfx = KenBurnsEffectHelper.getScroll(image.getPath(), widthToHeight, null);

            String text = null;
            if(mIncludeText) {
                //TODO: get actual text
                text = "The narrator reads the story...";
            }

            pages[iSlide] = new StoryPage(image, audio, kbfx, text);
        }

        //TODO: add copyright and hymn

        return pages;
    }

    private void watchProgress() {
        Thread watcher = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!mStoryMaker.isDone()) {
                    double progress = mStoryMaker.getProgress();
                    double audioProgress = mStoryMaker.getAudioProgress();
                    double videoProgress = mStoryMaker.getVideoProgress();
                    Log.i(TAG, "StoryMaker progress: " + MediaHelper.getDecimal(progress * 100) + "% "
                            + "(audio " + MediaHelper.getDecimal(audioProgress * 100) + "% "
                            + " and video " + MediaHelper.getDecimal(videoProgress * 100) + "%)");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        watcher.start();
    }
}

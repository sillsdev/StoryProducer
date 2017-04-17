package org.sil.storyproducer.tools.media.story;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.widget.Toast;

import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.KenBurnsSpec;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.file.VideoFiles;
import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;
import org.sil.storyproducer.tools.media.graphics.TextOverlay;
import org.sil.storyproducer.tools.media.graphics.TextOverlayHelper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoStoryMaker is a layer of abstraction above {@link StoryMaker} that handles all of the
 * parameters for StoryMaker according to some defaults, structure of projects/templates, and
 * minimal customization.
 */
public class AutoStoryMaker extends Thread implements Closeable {
    private static final String TAG = "AutoStoryMaker";

    private final String mStory;
    private String mTitle;

    // size of a frame, in pixels
    // first size isn't great quality, but runs faster
    private int mWidth = 320;
    private int mHeight = 240;
//    private int mWidth = 1280;
//    private int mHeight = 720;

    private static final int TITLE_FONT_SIZE = 20;

    private static final long SLIDE_CROSS_FADE_US = 3000000;
    private static final long AUDIO_TRANSITION_US = 500000;

    private static final long COPYRIGHT_SLIDE_US = 2000000;

    private String mOutputExt = ".mp4";
    private File mOutputFile;
    private File mTempFile;

    // parameters for the video encoder
    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int VIDEO_FRAME_RATE = 30;               // 30fps
    private static final int VIDEO_IFRAME_INTERVAL = 1;           // 1 second between I-frames

    // using Kush Gauge for video bit rate
    private static final int MOTION_FACTOR = 2;                   // 1, 2, or 4
    private static final float KUSH_GAUGE_CONSTANT = 0.07f;
    // bits per second for video
    private int mVideoBitRate;

    // parameters for the audio encoder
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; //MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 48000;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_BIT_RATE = 64000;

    private boolean mIncludeBackgroundMusic = true;
    private boolean mIncludePictures = true;
    private boolean mIncludeText = false;
    private boolean mIncludeKBFX = true;

    private boolean mLogProgress = false;

    private StoryMaker mStoryMaker;

    private Context mContext;

    public AutoStoryMaker(String story) {
        mStory = story;
        mTitle = mStory;

        setResolution(mWidth, mHeight);

        File outputDir = VideoFiles.getDefaultLocation(mStory);
        setOutputFile(new File(outputDir, mStory.replace(' ', '_') + "_"
                + mWidth + "x" + mHeight + mOutputExt));
    }

    /**
     * Set a context for the AutoStoryMaker if {@link android.widget.Toast} error messages are desired.
     * If no context is set, console logging will be used instead.
     *
     * @param context
     */
    public void setContext(Context context) {
        mContext = context;
    }

    public void setOutputFile(File output) {
        mOutputFile = output;

        Pattern extPattern = Pattern.compile("\\.\\w+$");
        Matcher extMatcher = extPattern.matcher(mOutputFile.getName());
        extMatcher.find();
        mOutputExt = extMatcher.group();

        mTempFile = new File(VideoFiles.getTempLocation(mStory), "partial_video" + mOutputExt);
    }

    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;

        int pixelRate = mWidth * mHeight * VIDEO_FRAME_RATE;
        mVideoBitRate = (int) (pixelRate * MOTION_FACTOR * KUSH_GAUGE_CONSTANT);
    }

    public void setTitle(String title) {
        mTitle = title;
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

    /**
     * Set whether progress is periodically logged in console. Default is false (no console logging).
     * @param logProgress whether progress should be logged in console.
     */
    public void toggleLogProgress(boolean logProgress) {
        mLogProgress = logProgress;
    }

    public boolean isDone() {
        if(mStoryMaker == null) {
            return false;
        }
        else {
            return mStoryMaker.isDone();
        }
    }

    @Override
    public void start() {
        int outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

        MediaFormat audioFormat = generateAudioFormat();
        MediaFormat videoFormat = generateVideoFormat();
        StoryPage[] pages = generatePages();

        //If pages weren't generated, exit.
        if(pages == null) {
            return;
        }

        mStoryMaker = new StoryMaker(mTempFile, outputFormat, videoFormat, audioFormat,
                pages, AUDIO_TRANSITION_US, SLIDE_CROSS_FADE_US);

        watchProgress();

        super.start();
    }

    @Override
    public void run() {
        long duration = -System.currentTimeMillis();

        Log.i(TAG, "Starting video creation...");
        boolean success = mStoryMaker.churn();

        if(success) {
            Log.v(TAG, "Moving completed video to " + mOutputFile.getAbsolutePath());
            mTempFile.renameTo(mOutputFile);
        }
        else {
            Log.w(TAG, "Deleting incomplete temporary video...");
            mTempFile.delete();
        }

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
        //If no video component, use null format.
        if(!mIncludePictures && !mIncludeText) {
            return null;
        }

        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        return videoFormat;
    }

    private StoryPage[] generatePages() {
        int slideCount = FileSystem.getContentSlideAmount(mStory);
        //TODO: add hymn to count
        StoryPage[] pages = new StoryPage[slideCount + 1];

        double widthToHeight = mWidth / (double) mHeight;

        //Create title slide with overlayed text.
        //Try to get special background (user provided).
        File titleBack = ImageFiles.getFile(mStory, ImageFiles.TITLE_BACKGROUND);
        if(!titleBack.exists()) {
            //Fallback to standard title slide.
            titleBack = ImageFiles.getFile(mStory, 0);
        }

        //Default title with no text.
        File title = titleBack;

        File tempTitle = ImageFiles.getFile(mStory, ImageFiles.TITLE_TEMP);
        TextOverlay titleOverlay = new TextOverlay(mTitle);
        titleOverlay.setFontSize(TITLE_FONT_SIZE);

        try {
            TextOverlayHelper.overlayJPEG(titleBack, tempTitle, titleOverlay);
            title = tempTitle;
        }
        catch (IOException e) {
            Log.w(TAG, "Failed to create overlayed title slide!");
        }

        int iSlide;
        for(iSlide = 0; iSlide < slideCount; iSlide++) {
            File image = null;
            if(mIncludePictures) {
                if(iSlide == 0) {
                    image = title;
                }
                else {
                    image = ImageFiles.getFile(mStory, iSlide);
                }
            }
            File audio = AudioFiles.getDramatization(mStory, iSlide);
            //fallback to draft audio
            if(!audio.exists()) {
                audio = AudioFiles.getDraft(mStory, iSlide);
            }
            //fallback to LWC audio
            if(!audio.exists()) {
                audio = AudioFiles.getLWC(mStory, iSlide);
            }
            //error
            if(!audio.exists()) {
                error("Audio missing for slide " + (iSlide + 1));
                return null;
            }

            File soundtrack = null;
            if(mIncludeBackgroundMusic) {
                soundtrack = AudioFiles.getSoundtrack(mStory, iSlide);
                if(soundtrack != null && !soundtrack.exists()) {
                    error("Soundtrack missing: " + soundtrack.getName());
                }
            }

            KenBurnsEffect kbfx = null;
            if(mIncludePictures && mIncludeKBFX && iSlide != 0) {
                kbfx = KenBurnsSpec.getKenBurnsEffect(mStory, iSlide);
            }

            String text = null;
            if(mIncludeText) {
                text = TextFiles.getSlideText(mStory, iSlide).getContent();
            }

            pages[iSlide] = new StoryPage(image, audio, 0, kbfx, text, soundtrack);
        }

        File copyrightImage = null;
        if(mIncludePictures) {
            copyrightImage = ImageFiles.getFile(mStory, ImageFiles.COPYRIGHT);
        }
        pages[iSlide++] = new StoryPage(copyrightImage, COPYRIGHT_SLIDE_US);

        //TODO: add hymn

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

        if(mLogProgress) {
            watcher.start();
        }
    }

    private void error(String message) {
        if(mContext != null) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
        else {
            Log.e(TAG, message);
        }
    }

    @Override
    public void close() {
        if(mStoryMaker != null) {
            mStoryMaker.close();
        }
    }
}

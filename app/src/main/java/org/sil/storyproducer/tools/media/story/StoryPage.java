package org.sil.storyproducer.tools.media.story;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;

import java.io.File;

/**
 * One StoryPage represents a unit of a story that will go into video format. Each StoryPage has
 * three parts: 1) image, 2) narration, and 3) Ken Burns effect. The narration dictates the length
 * of the page in the video, and the image and Ken Burns effect follow its queues.
 */
public class StoryPage {
    private final File mImage;
    private final File mNarrationAudio;
    private final File mSoundtrackAudio;
    private final long mDuration;
    private final KenBurnsEffect mKBFX;
    private final String mText;

    private static final int CACHE_SIZE = 3;

    private static final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        @Override
        protected Bitmap create(String key) {
            return BitmapFactory.decodeFile(key);
        }
    };

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param kbfx Ken Burns effect for the image.
     */
    public StoryPage(File image, File narrationAudio, KenBurnsEffect kbfx) {
        this(image, narrationAudio, kbfx, null);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    public StoryPage(File image, File narrationAudio, KenBurnsEffect kbfx, String text) {
        this(image, narrationAudio, 0, kbfx, text);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param duration length of page in microseconds.
     */
    public StoryPage(File image, long duration) {
        this(image, duration, null);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     */
    public StoryPage(File image, long duration, KenBurnsEffect kbfx) {
        this(image, duration, kbfx, null);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    public StoryPage(File image, long duration, KenBurnsEffect kbfx, String text) {
        this(image, null, duration, kbfx, text);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    private StoryPage(File image, File narrationAudio, long duration, KenBurnsEffect kbfx, String text) {
        this(image, narrationAudio, duration, kbfx, text, null);
    }

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     * @param soundtrackAudio soundtrack for page
     */
    public StoryPage(File image, File narrationAudio, long duration, KenBurnsEffect kbfx,
                      String text, File soundtrackAudio) {
        mImage = image;
        mNarrationAudio = narrationAudio;
        mDuration = duration;
        mKBFX = kbfx;
        mText = text;
        mSoundtrackAudio = soundtrackAudio;
    }

    /**
     * Get the audio duration.
     * @return duration in microseconds.
     */
    public long getDuration() {
        if(mNarrationAudio != null) {
            return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
        }
        else {
            return mDuration;
        }
    }

    public Bitmap getBitmap() {
        if(mImage == null) {
            return null;
        }
        else {
            return mCache.get(mImage.getPath());
        }
    }

    public File getNarrationAudio() {
        return mNarrationAudio;
    }

    public File getSoundtrackAudio() {
        return mSoundtrackAudio;
    }

    public KenBurnsEffect getKenBurnsEffect() {
        return mKBFX;
    }

    public String getText() {
        return mText;
    }
}

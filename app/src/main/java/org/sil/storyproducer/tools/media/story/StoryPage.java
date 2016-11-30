package org.sil.storyproducer.tools.media.story;

import android.graphics.Bitmap;

import org.sil.storyproducer.tools.media.KenBurnsEffect;
import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.File;

/**
 * One StoryPage represents a unit of a story that will go into video format. Each StoryPage has
 * three parts: 1) image, 2) narration, and 3) Ken Burns effect. The narration dictates the length
 * of the page in the video, and the image and Ken Burns effect follow its queues.
 */
public class StoryPage {
    private final File mImage;
    private final File mNarrationAudio;
    private final KenBurnsEffect mKBFX;

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param kbfx Ken Burns effect for the image.
     */
    public StoryPage(File image, File narrationAudio, KenBurnsEffect kbfx) {
        mImage = image;
        mNarrationAudio = narrationAudio;
        mKBFX = kbfx;
    }

    /**
     * Get the audio duration.
     * @return duration in microseconds.
     */
    public long getAudioDuration() {
        return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
    }

    public Bitmap getBitmap() {
        return StoryBitmapManager.get(mImage.getPath());
    }

    public File getNarrationAudio() {
        return mNarrationAudio;
    }

    public KenBurnsEffect getKenBurnsEffect() {
        return mKBFX;
    }
}

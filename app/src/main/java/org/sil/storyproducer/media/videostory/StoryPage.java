package org.sil.storyproducer.media.videostory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.sil.storyproducer.media.BitmapManager;
import org.sil.storyproducer.media.KenBurnsEffect;
import org.sil.storyproducer.media.MediaHelper;

import java.io.File;

public class StoryPage {
    private File mImage;
    private File mNarrationAudio;
    private KenBurnsEffect mKBFX;

    public StoryPage(File image, File narrationAudio, KenBurnsEffect kbfx) {
        mImage = image;
        mNarrationAudio = narrationAudio;
        mKBFX = kbfx;
    }

    public long getAudioDuration() {
        return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
    }

    public String getPath() {
        return mImage.getPath();
    }

    public Bitmap getBitmap() {
        return BitmapManager.get(mImage.getPath());
    }

    public File getNarrationAudio() {
        return mNarrationAudio;
    }

    public KenBurnsEffect getKenBurnsEffect() {
        return mKBFX;
    }
}

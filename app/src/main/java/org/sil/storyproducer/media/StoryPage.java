package org.sil.storyproducer.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

    public long getDuration() {
        return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
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

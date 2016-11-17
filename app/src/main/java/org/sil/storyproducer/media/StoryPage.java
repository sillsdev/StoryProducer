package org.sil.storyproducer.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class StoryPage {
    private File mImage;
    private File mNarrationAudio;
    private KenBurnsEffect mKBFX;

    private Bitmap mBitmap;

    public StoryPage(File image, File narrationAudio, KenBurnsEffect kbfx) {
        mImage = image;
        mNarrationAudio = narrationAudio;
        mKBFX = kbfx;
    }

    public long getDuration() {
        return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
    }

    public Bitmap getBitmap() {
        if(mBitmap == null) {
            mBitmap = BitmapFactory.decodeFile(mImage.getPath());
        }
        return mBitmap;
    }

    public File getNarrationAudio() {
        return mNarrationAudio;
    }

    public KenBurnsEffect getKenBurnsEffect() {
        return mKBFX;
    }
}

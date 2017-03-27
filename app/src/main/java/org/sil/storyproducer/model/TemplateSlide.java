package org.sil.storyproducer.model;

import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect;

import java.io.File;

/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
public class TemplateSlide {

    private File mNarrationAudio;
    private File mImage;
    private KenBurnsEffect mKBFX;
    private File mSoundtrack;
    private int mSoundtrackVolume;

    public TemplateSlide(File narrationAudio, File image, KenBurnsEffect kbfx, File soundtrack, int soundtrackVolume) {
        mNarrationAudio = narrationAudio;
        mImage = image;
        mKBFX = kbfx;
        mSoundtrack = soundtrack;
        mSoundtrackVolume = soundtrackVolume;
    }

    public File getNarrationAudio() {
        return mNarrationAudio;
    }

    public File getImage() {
        return mImage;
    }

    public KenBurnsEffect getKenBurnsEffect() {
        return mKBFX;
    }

    public File getSoundtrack() {
        return mSoundtrack;
    }

    public int getSoundtrackVolume() {
        return mSoundtrackVolume;
    }
}

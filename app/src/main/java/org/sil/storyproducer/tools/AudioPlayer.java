package org.sil.storyproducer.tools;

import android.media.MediaPlayer;

import java.io.IOException;

public class AudioPlayer {

    MediaPlayer mPlayer;

    /**
     * Constructor for Audio Player, no params
     */
    public AudioPlayer() {
        mPlayer = new MediaPlayer();
    }

    /**
     * Plays the audio with the given path
     * @param String for the path where the audio resides
     */
    public void playWithPath(String path) {
        try {
            mPlayer.setDataSource(path);
        } catch (IOException e) {
            //TODO maybe somehting with this exception
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            //TODO maybe somehting with this exception
            e.printStackTrace();
        }
        mPlayer.start();
    }

    /**
     * Pauses the audio if it is currenlty being played
     */
    public void pauseAudio() {
        if(mPlayer.isPlaying()) {
            try {
                mPlayer.pause();
            } catch (IllegalStateException e) {
                //TODO maybe somehting with this exception
                e.printStackTrace();
            }
        }
    }

    /**
     * Resumes the audio from where it was last paused
     */
    public void resumeAudio() {
        int pauseSpot = mPlayer.getCurrentPosition();
        mPlayer.seekTo(pauseSpot);
        mPlayer.start();
    }

    /**
     * Stops the audio if it is currenlty being played
     */
    public void stopAudio() {
        try {
            mPlayer.stop();
            mPlayer.release();
        } catch (IllegalStateException e) {
            //TODO maybe somehting with this exception
            e.printStackTrace();
        }
    }

    /**
     * returns the duration of the audio as an int
     * @return returns the duration of the audio as an int
     */
    public int getAudioDurationInSeconds() {
        return (int)(mPlayer.getDuration() * 0.001);
    }

    /**
     * sets the completion listener
     * @param listener
     */
    public void audioCompletionListener(MediaPlayer.OnCompletionListener listener) {
        mPlayer.setOnCompletionListener(listener);
    }

    /**
     * sets the volume of the audio
     * @param the float for the volume from 0.0 to 1.0
     */
    public void setVolume(float volume) {
        mPlayer.setVolume(volume, volume);
    }

    /**
     * returns if the audio is being played or not
     * @return true or false based on if the audio is being played
     */
    public boolean isAudioPlaying() {
        return mPlayer.isPlaying();
    }
}

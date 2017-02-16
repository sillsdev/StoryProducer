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
     * Only sets the path for the audio to
     * @param path String path for the audio
     */
    public void setPath(String path) {
        try {
            mPlayer.setDataSource(path);
        } catch (IOException e) {
            //TODO maybe something with this exception
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            //TODO maybe something with this exception
            e.printStackTrace();
        }
    }

    /**
     * Plays the audio with the given path
     * @param path for the path where the audio resides
     */
    public void playWithPath(String path) {
        try {
            mPlayer.setDataSource(path);
        } catch (IOException e) {
            //TODO maybe something with this exception
            e.printStackTrace();
        }
        try {
            mPlayer.prepare();
        } catch (IOException e) {
            //TODO maybe something with this exception
            e.printStackTrace();
        }
        mPlayer.start();
    }

    /**
     * Pauses the audio if it is currenlty being played
     */
    public void pauseAudio() {
        if(mPlayer != null && mPlayer.isPlaying()) {
            try {
                mPlayer.pause();
            } catch (IllegalStateException e) {
                //TODO maybe something with this exception
                e.printStackTrace();
            }
        }
    }

    /**
     * Resumes the audio from where it was last paused
     */
    public void resumeAudio() {
        if(mPlayer != null) {
            int pauseSpot = mPlayer.getCurrentPosition();
            mPlayer.seekTo(pauseSpot);
            mPlayer.start();
        }
    }

    /**
     * Stops the audio if it is currenlty being played
     */
    public void stopAudio() {
        if(mPlayer!= null && mPlayer.isPlaying()) {
            try {
                mPlayer.stop();
            } catch (IllegalStateException e) {
                //TODO maybe something with this exception
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the audio and releases it if it is currenlty being played
     */
    public void releaseAudio() {
        if(mPlayer!= null && mPlayer.isPlaying()) {
            try {

                mPlayer.stop();
                mPlayer.release();
            } catch (IllegalStateException e) {
                //TODO maybe something with this exception
                e.printStackTrace();
            } finally {
                mPlayer = null;   //this set to null so that an error doesn't occur if someone trys to release audio again
            }
        }
    }

    /**
     * returns the duration of the audio as an int
     * @return the duration of the audio as an int
     */
    public int getAudioDurationInSeconds() {
        return (int)(mPlayer.getDuration() * 0.001);
    }

    /**
     * returns the duration of the audio as an int in miliseconds
     * @return the duration of the audio as an int
     */
    public int getAudioDurationInMilliseconds() {
        return (int)mPlayer.getDuration();
    }

    /**
     * Seeks to the parameter in milliseconds
     * @param msec milliseconds for where to seek to in the audio
     */
    public void seekTo(int msec) {
        mPlayer.seekTo(msec);
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
     * @param volume the float for the volume from 0.0 to 1.0
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

package org.sil.storyproducer.tools.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryUri
import java.io.IOException

class AudioPlayer {

    private var mPlayer: MediaPlayer = MediaPlayer()
    private var fileExists: Boolean = false
    private var onCompletionListenerPersist: MediaPlayer.OnCompletionListener? = null

    var currentPosition: Int
        get() = if (isAudioPrepared) {
            mPlayer.currentPosition
        } else {
            0
        }
        set(value) {
            if (isAudioPrepared) {
                mPlayer.seekTo(value)
            }
        }

    /**
     * returns the duration of the audio as an int in milliseconds
     * @return the duration of the audio as an int
     */
    val audioDurationInMilliseconds: Int
        get() = if (isAudioPrepared) {
            mPlayer.duration
        } else {
            0
        }

    /**
     * returns if the audio is being played or not
     * @return true or false based on if the audio is being played
     */
    val isAudioPlaying: Boolean
        get() = isAudioPrepared && mPlayer.isPlaying

    var isAudioPrepared: Boolean = false
        private set

    fun setSource(context: Context, uri: Uri): Boolean {
        mPlayer.release()
        mPlayer = MediaPlayer()
        mPlayer.setOnCompletionListener(onCompletionListenerPersist)
        mPlayer.setDataSource(context, uri)
        fileExists = true
        isAudioPrepared = true
        mPlayer.prepare()
        currentPosition = 0
        return fileExists
    }

    /**
     * set the audio file from the worskspace data
     * @return true if the file exists, false if it does not.
     */

    fun setStorySource(context: Context, relPath: String,
                       storyName: String = Workspace.activeStory.title): Boolean {
        val uri: Uri = getStoryUri(relPath, storyName) ?: return false
        return setSource(context, uri)
    }

    fun playAudio() {
        currentPosition = 0
        resumeAudio()
    }

    /**
     * Pauses the audio if it is currently being played
     */
    fun pauseAudio() {
        if (mPlayer.isPlaying) {
            mPlayer.pause()
        }
    }

    /**
     * Resumes the audio from where it was last paused
     */
    fun resumeAudio() {
        if (fileExists) {
            mPlayer.start()
        }

    }

    /**
     * Stops the audio if it is currently being played
     */
    fun stopAudio() {
        if (mPlayer.isPlaying) mPlayer.pause()
        if (currentPosition != 0) currentPosition = 0
    }

    /**
     * Releases the MediaPlayer object after completion
     */
    fun release() {
        isAudioPrepared = false
        mPlayer.release()
    }

    /**
     * Seeks to the parameter in milliseconds
     * @param msec milliseconds for where to seek to in the audio
     */
    fun seekTo(msec: Int) {
        if (!fileExists) return
        mPlayer.seekTo(msec)
    }

    /**
     * sets the completion listener
     * @param listener handler for OnCompletionListener
     */
    fun onPlayBackStop(listener: MediaPlayer.OnCompletionListener) {
        onCompletionListenerPersist = listener
        mPlayer.setOnCompletionListener(listener)
    }

    /**
     * sets the volume of the audio
     * @param volume the float for the volume from 0.0 to 1.0
     */
    fun setVolume(volume: Float) {
        mPlayer.setVolume(volume, volume)
    }

    companion object {

        private val TAG = "AudioPlayer"
    }
}

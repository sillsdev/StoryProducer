package org.sil.storyproducer.tools.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryUri
import java.io.IOException

class AudioPlayer {

    private var mPlayer: MediaPlayer
    private var fileExists: Boolean = false
    private var onCompletionListenerPersist: MediaPlayer.OnCompletionListener? = null

    var currentPosition: Int
        get() =
            try{ mPlayer.currentPosition
            } catch (e : Exception){ 0 }
        set(value) {
            try {
                mPlayer.seekTo(value)
            } catch (e : Exception) {}
        }

    /**
     * returns the duration of the audio as an int in milliseconds
     * @return the duration of the audio as an int
     */
    val audioDurationInMilliseconds: Int
        get() = mPlayer.duration

    /**
     * returns if the audio is being played or not
     * @return true or false based on if the audio is being played
     */
    val isAudioPlaying: Boolean
        get() {
            try {
                return mPlayer.isPlaying
            } catch (e: IllegalStateException) {
                return false
            }

        }

    var isAudioPrepared: Boolean = false
        private set

    /**
     * Constructor for Audio Player, no params
     */
    init {
        mPlayer = MediaPlayer()
        fileExists = false
    }

    fun setSource(context: Context, uri: Uri) : Boolean {
        try {
            mPlayer.release()
            mPlayer = MediaPlayer()
            mPlayer.setOnCompletionListener(onCompletionListenerPersist)
            mPlayer.setDataSource(context, uri)
            fileExists = true
            isAudioPrepared = true
            mPlayer.prepare()
            currentPosition = 0
        } catch (e: Exception) {
            //TODO maybe do something with this exception
            fileExists = false
            isAudioPrepared = false
        }
        return fileExists
    }
    /**
     * set the audio file from the worskspace data
     * @return true if the file exists, false if it does not.
     */

    fun setStorySource(context: Context, relPath: String,
                       storyName: String = Workspace.activeStory.title) : Boolean {
        val uri: Uri = getStoryUri(relPath,storyName) ?: return false
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
            try {
                if(mPlayer.isPlaying)
                    mPlayer.pause()
            } catch (e: Exception) {}
        }
    }

    /**
     * Resumes the audio from where it was last paused
     */
    fun resumeAudio() {
        if(!fileExists) return
        try {
            if(fileExists) {
                mPlayer.start()
            }
        } catch (e: Exception) { }

    }

    /**
     * Stops the audio if it is currently being played
     */
    fun stopAudio() {
        try {
            if(mPlayer.isPlaying) mPlayer.pause()
            if(currentPosition != 0) currentPosition = 0
        } catch (e: Exception) {}
    }

    /**
     * Releases the MediaPlayer object after completion
     */
    fun release() {
        isAudioPrepared = false
        try {
            mPlayer.release()
        } catch (e : Exception) {}
    }

    /**
     * Seeks to the parameter in milliseconds
     * @param msec milliseconds for where to seek to in the audio
     */
    fun seekTo(msec: Int) {
        if(!fileExists) return
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

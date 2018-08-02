package org.sil.storyproducer.tools.media

import org.sil.storyproducer.tools.file.getStoryUri
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import org.sil.storyproducer.model.Workspace

import java.io.IOException

class AudioPlayer {

    private var mPlayer: MediaPlayer
    private var fileExists: Boolean = false
    private var isPrepared: Boolean = false

    /**
     * returns the duration of the audio as an int
     * @return the duration of the audio as an int
     */
    val audioDurationInSeconds: Int
        get() = (mPlayer.duration * 0.001).toInt()

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
                Log.w(TAG, "Failing silently", e)
                return false
            }

        }

    /**
     * Constructor for Audio Player, no params
     */
    init {
        mPlayer = MediaPlayer()
        fileExists = false
        isPrepared = false
    }

    /**
     * Only sets the path for the audio to
     * @param path String path for the audio
     */
    fun setSource(context: Context, uri: Uri) : Boolean {
        try {
            mPlayer.release()
            mPlayer = MediaPlayer()
            isPrepared = false
            mPlayer.setDataSource(context, uri)
            fileExists = true
        } catch (e: Exception) {
            //TODO maybe do something with this exception
            fileExists = false
            e.printStackTrace()
        }
        return fileExists
    }
    /**
     * set the audio file from the worskspace data
     * @return true if the file exists, false if it does not.
     */

    fun setStorySource(context: Context, relPath: String,
                       storyName: String = Workspace.activeStory.title) : Boolean {
        val uri: Uri? = getStoryUri(relPath,storyName)
        if(uri == null) return false
        return setSource(context, uri)
    }

    /**
     * Plays the audio with the given path
     */

    fun playAudio() {
        if(!fileExists) return
        try {
            if (!isPrepared) {
                mPlayer.prepare()
                isPrepared = true
            }
        } catch (e: IOException) {
            //TODO maybe do something with this exception
            e.printStackTrace()
        }

        mPlayer.start()
    }

    /**
     * Pauses the audio if it is currently being played
     */
    fun pauseAudio() {
        if (mPlayer.isPlaying) {
            try {
                mPlayer.pause()
            } catch (e: IllegalStateException) {
                //TODO maybe do something with this exception
                e.printStackTrace()
            }

        }
    }

    /**
     * Resumes the audio from where it was last paused
     */
    fun resumeAudio() {
        if(!fileExists) return
        try {
            if (!isPrepared) {
                mPlayer.prepare()
                isPrepared = true
            }
            val pauseSpot = mPlayer.currentPosition
            mPlayer.seekTo(pauseSpot)
            mPlayer.start()
        } catch (e: IOException) {
            //TODO maybe do something with this exception
            e.printStackTrace()
        }

    }

    /**
     * Stops the audio if it is currently being played
     */
    fun stopAudio() {
        if (isPrepared) {
            try {
                mPlayer.stop()
                isPrepared = false
            } catch (e: IllegalStateException) {
                //TODO maybe do something with this exception
                e.printStackTrace()
            }

        }
    }

    /**
     * Releases the MediaPlayer object after completion
     */
    fun release() {
        mPlayer.release()
    }

    /**
     * Seeks to the parameter in milliseconds
     * @param msec milliseconds for where to seek to in the audio
     */
    fun seekTo(msec: Int) {
        if(!fileExists) return
        try {
            if (!isPrepared) {
                mPlayer.prepare()
                isPrepared = true
            }
            mPlayer.seekTo(msec)
        } catch (e: IOException) {
            //TODO maybe do something with this exception
            e.printStackTrace()
        }

    }

    /**
     * sets the completion listener
     * @param listener handler for OnCompletionListener
     */
    fun onPlayBackStop(listener: MediaPlayer.OnCompletionListener) {
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

    fun fileExists(): Boolean {return fileExists}
}

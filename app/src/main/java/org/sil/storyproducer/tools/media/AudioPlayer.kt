package org.sil.storyproducer.tools.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.model.WORD_LINKS_DIR
import java.io.File

class AudioPlayer {

    private var mPlayer: MediaPlayer
    private var fileExists: Boolean = false
    private var dummyPlayStartTime = 0L // the time that playback started
    private var dummyPlayPosition = 0L  // the play position when paused
    private var onCompletionListenerPersist: MediaPlayer.OnCompletionListener? = null

    var isDummyAudio: Boolean = false    // when set a dummy 2s AudioPlayer object is defined
        get() = isDummyAudioPrepared

    private var isDummyAudioPlaying: Boolean = false    // simulated silent audio playback

    private var isDummyAudioPrepared: Boolean = false   // set if ready to playback dummy audio

    var currentPosition: Int
        get() = if (isDummyAudio) { // check if this is a dummy object first
            if (isDummyAudioPlaying) {  // if dummy is in simulated playback
                var pos = System.currentTimeMillis() - dummyPlayStartTime // calc current position
                if (pos > audioDurationInMilliseconds) {    // check if dummy 2s audio is finished
                    stopAudio()                             // stop dummy playback
                    dummyPlayPosition = dummyDurationInMilliseconds.toLong()    // set at end position
                    pos = dummyPlayPosition
                    onCompletionListenerPersist?.onCompletion(mPlayer)  // call completion callback
                }
                pos.toInt()   // return current position
            }
            else
                dummyPlayPosition.toInt()  // return paused position
        } else try {
                mPlayer.currentPosition     // use real currentPosition
            } catch (e : Exception) { 0 }   // or 0 on error
        set(value) {
            if (isDummyAudio) { // calc dummy start time or paused position
                if (isDummyAudioPlaying)
                    dummyPlayStartTime = System.currentTimeMillis() - value
                else
                    dummyPlayPosition = value.toLong()
            } else try {
                    mPlayer.seekTo(value)   // use real seek
                } catch (e : Exception) {}
            }


    /**
     * returns the duration of the audio as an int in milliseconds
     * @return the duration of the audio as an int
     */
    val audioDurationInMilliseconds: Int
        get() = if (isDummyAudio)
            dummyDurationInMilliseconds // return dummy duration
        else
            mPlayer.duration            // return real duration

    /**
     * returns if the audio is being played or not
     * @return true or false based on if the audio is being played
     */
    val isAudioPlaying: Boolean
        get() {
            if (isDummyAudio)
                return isDummyAudioPlaying  // return dummmy playing flag
            else try {
                return mPlayer.isPlaying    // return real isPlaying
            } catch (e: IllegalStateException) {
                return false
            }
        }

    var isAudioPrepared: Boolean = false
        get() = isDummyAudioPrepared || field
        private set

    /**
     * Constructor for Audio Player, no params
     */
    init {
        mPlayer = MediaPlayer()
        fileExists = false
    }

    fun setSource(context: Context, uri: Uri) : Boolean {
        if (uri == Uri.fromFile(File(""))) {
            fileExists = true
            isAudioPrepared = false
            isDummyAudioPrepared = true
            dummyPlayPosition = 0
        } else try {
            mPlayer.release()
            mPlayer = MediaPlayer()
            mPlayer.setOnCompletionListener(onCompletionListenerPersist)
            mPlayer.setDataSource(context, uri)
            fileExists = true
            isAudioPrepared = true
            isDummyAudioPrepared = false
            mPlayer.prepare()
            currentPosition = 0
        } catch (e: Exception) {
            //TODO maybe do something with this exception
            fileExists = false
            isAudioPrepared = false
            isDummyAudioPrepared = false
        }
        return fileExists
    }
    /**
     * set the audio file from the worskspace data
     * @return true if the file exists, false if it does not.
     */

    fun setStorySource(context: Context, relPath: String,
                       storyName: String = Workspace.activeStory.title) : Boolean {
        val uri: Uri = if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS){
            getStoryUri(relPath, WORD_LINKS_DIR) ?: return false
        }
        else{
            getStoryUri(relPath,storyName) ?: return false
        }
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
        if (isDummyAudio && isDummyAudioPlaying) {
            // clear dummy playing flag:
            isDummyAudioPlaying = false
            // calc dummy position:
            dummyPlayPosition = System.currentTimeMillis() - dummyPlayStartTime
        }
        else try {
            if(mPlayer.isPlaying)
                mPlayer.pause() // do a real audio pause
        } catch (e: Exception) {}
    }

    /**
     * Resumes the audio from where it was last paused
     */
    fun resumeAudio() {
        if (isDummyAudio && !isDummyAudioPlaying) {
            // set dummy playing flag:
            isDummyAudioPlaying = true
            // set dummy playback start time:
            dummyPlayStartTime =  System.currentTimeMillis() - dummyPlayPosition
        }
        else try {
            if(fileExists) {
                mPlayer.start()
            }
        } catch (e: Exception) { }

    }

    /**
     * Stops the audio if it is currently being played
     */
    fun stopAudio() {
        if (isDummyAudio) {
            // clear dummy playing flag:
            isDummyAudioPlaying = false
            // reset dummy playback position:
            dummyPlayPosition = 0
        } else try {
            if(mPlayer.isPlaying) mPlayer.pause()
            if(currentPosition != 0) currentPosition = 0
        } catch (e: Exception) {}
    }

    /**
     * Releases the MediaPlayer object after completion
     */
    fun release() {
        isDummyAudioPrepared = false
        isAudioPrepared = false
        isDummyAudioPrepared = false
        isDummyAudioPlaying = false
        dummyPlayPosition = 0L
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
        if (isDummyAudio) {
            // set dummy playback position:
            dummyPlayPosition = msec.toLong()
        }
        else
            mPlayer.seekTo(msec)    // do real seek
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
        if (!isDummyAudio)  // set real volume only if not dummy
            mPlayer.setVolume(volume, volume)
    }

    companion object {

        private val TAG = "AudioPlayer"
        const val dummyDurationInMilliseconds = 2000
    }
}

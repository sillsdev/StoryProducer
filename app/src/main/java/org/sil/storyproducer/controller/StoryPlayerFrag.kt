package org.sil.storyproducer.controller

import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.tools.media.AudioPlayer

abstract class StoryPlayerFrag : Fragment() {

    var playing = false
    var startTime = -1L
    lateinit var phaseType: PhaseType

    var startSlide:Int = 0
    var slideRange:Int = 1 //slides to be played by player

    var playPauseButton : ImageButton? = null
    var slideNumber : TextView? = null

    var audioPlayer = AudioPlayer()

    /**
     * Plays the story
     */
    open fun play() {
        seek(0)
        startTime = System.currentTimeMillis()
        playing = true
        setPlayPauseButton(playing)
        if(audioPlayer.isAudioPrepared) {
            audioPlayer.seekTo(0)
            audioPlayer.playAudio()
        }
    }

    /**
     * Stops story playback and returns to the beginning
     */
    open fun stop() {
        seek(1)
        playing = false
        setPlayPauseButton(playing)
        saveLog(System.currentTimeMillis())
        if(audioPlayer.isAudioPrepared) {
            audioPlayer.pauseAudio()
        }
    }

    /**
     * Turns the story sound off
     */
    open fun mute() {
        if(audioPlayer.isAudioPrepared) {
            audioPlayer.setVolume(0f)
        }
    }

    /**
     * Turns the story sound on
     */
    open fun unmute() {
        if(audioPlayer.isAudioPrepared) {
            audioPlayer.setVolume(1f)
        }
    }

    /**
     * Sets the position of the video playback
     */
    abstract fun seek(milliseconds: Int)

    /**
     * Gets the current position of the story playback
     */
    abstract fun getPosition() : Int

    /**
     * Gets the total length of the story
     */
    abstract fun getDuration() : Int

    abstract fun saveLog(pauseTime : Long)

    private fun setPlayPauseButton(playing : Boolean) {
        if(playPauseButton != null) {
            if (playing) {
                playPauseButton!!.setImageResource(R.drawable.ic_stop_white_48dp)
            } else {
                playPauseButton!!.setImageResource(R.drawable.ic_play_arrow_white_48dp)
            }
        }
    }

}

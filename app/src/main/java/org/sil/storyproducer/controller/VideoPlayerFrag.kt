package org.sil.storyproducer.controller

import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.VideoView
import org.sil.storyproducer.film.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.MediaHelper


class VideoPlayerFrag : StoryPlayerFrag() {

    private lateinit var videoView : VideoView
    private lateinit var seekBar : SeekBar
    private lateinit var videoAudio : MediaPlayer

    private var videoEnd = 0
    private var videoStart = 0
    private var narrationLength = 0

    private var seekBarUpdateThread : HandlerThread? = null
    private var seekBarUpdateHandler : Handler? = null
    private var seekBarUpdater = object : Runnable {
        // this updates the seek bar
        override fun run() {
            if(playing) {
                val videoDuration = videoEnd - videoStart
                val progress: Int
                val maxTime: Int
                if(videoDuration > narrationLength) {
                    progress = videoView.currentPosition
                    seekBar.progress = progress - videoStart
                    maxTime = videoEnd
                } else {
                    progress = audioPlayer.currentPosition
                    seekBar.progress = progress
                    maxTime = narrationLength

                    if(progress + videoStart >= videoEnd) {
                        videoView.pause()
                    }
                }

                if(progress >= maxTime) {
                    activity!!.runOnUiThread {
                        stop()
                    }
                }
            }

            seekBarUpdateHandler!!.postDelayed(this, 33)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_video_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoView = view.findViewById(R.id.video_player_video_view)
        playPauseButton = view.findViewById(R.id.video_player_play_pause_button)
        seekBar = view.findViewById(R.id.video_player_seekbar)
        slideNumber = view.findViewById(R.id.video_player_slide_number)

        videoStart = Workspace.activeStory.slides[startSlide].startTime
        videoEnd = Workspace.activeStory.slides[startSlide + slideRange - 1].endTime
        slideNumber!!.text = startSlide.toString()

        seekBar.max = videoEnd - videoStart

        videoView.setOnPreparedListener { mp ->
            videoAudio = mp

            if(slideRange == 1) {
                val audioFile = Workspace.activePhase.getReferenceAudioFile(startSlide)

                if(audioFile != "") {
                    mp.setVolume(0f, 0f)
                    audioPlayer.setStorySource(context!!, audioFile)
                    narrationLength = MediaHelper.getAudioDuration(
                            context!!,
                            getStoryUri(audioFile)!!
                    ).toInt() / 1000

                    if(narrationLength > seekBar.max) {
                        seekBar.max = narrationLength
                    }
                }
            }
        }

        videoView.setOnCompletionListener {
            stop()
        }

        playPauseButton!!.setOnClickListener {
            if(playing) {
                stop()
            } else {
                play()
            }
        }

        // if the seekbar is changed, call the seek function
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {}
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seek(progress)
                }
            }
        })
        // seek bar on end listener

        // prepare the video
        val videoUri = getStoryUri(Workspace.activeStory.fullVideo)
        videoView.setVideoURI(videoUri)
        videoView.seekTo(videoStart)
    }

    override fun onPause() {
        super.onPause()
        seekBarUpdateHandler!!.removeCallbacks(seekBarUpdater)
        seekBarUpdateThread!!.quit()
    }

    // These functions handle the seek bar thread when the video is paused/resumed
    override fun onResume() {
        super.onResume()
        seekBarUpdateThread = HandlerThread("Seek bar updater")
        seekBarUpdateThread!!.start()
        seekBarUpdateHandler = Handler(seekBarUpdateThread!!.looper)
        seekBarUpdateHandler!!.post(seekBarUpdater)
    }

    // These are some basic control functions that override from StoryPlayer
    // Some are handled differently if we are playing the full video or just a clip form it
    override fun play() {
        super.play()
        videoView.start()
    }

    override fun stop() {
        super.stop()
        videoView.seekTo(videoStart)
        videoView.pause()
    }

    override fun mute() {
        super.mute()
        videoAudio.setVolume(0f, 0f)
    }

    override fun unmute() {
        super.unmute()

        videoAudio.setVolume(1f, 1f)
    }

    override fun seek(milliseconds: Int) {
        videoView.seekTo(milliseconds + videoStart)
        seekBar.progress = milliseconds
    }

    override fun getPosition() : Int {
        return videoView.currentPosition
    }

    override fun getDuration() : Int {
        return videoView.duration
    }

    override fun saveLog(pauseTime: Long) {
        val durationPlayed = (pauseTime - startTime)
        if (durationPlayed<100 || startTime<0 || pauseTime <0){
            return
        }
        when (phaseType) {
            PhaseType.LEARN -> {
                val mResources = context!!.resources
                var ret = "Video Playback"

                //format duration:
                val secUnit = mResources.getString(R.string.SECONDS_ABBREVIATION)
                val minUnit = mResources.getString(R.string.MINUTES_ABBREVIATION)

                if (durationPlayed < 1000) {
                    ret += " (<1 $secUnit)"
                } else {
                    val roundedSecs = (durationPlayed / 1000.0 + 0.5).toInt()
                    val mins = roundedSecs / 60
                    var minString = ""
                    if (mins > 0) {
                        minString = "$mins $minUnit "
                    }
                    ret += " (" + minString + roundedSecs % 60 + " " + secUnit + ")"
                }
                saveLog(ret, 0, Workspace.activeStory.numSlides - 1)
            }
            PhaseType.DRAFT -> {
                saveLog(getString(R.string.LWC_PLAYBACK) + " Slide " + startSlide.toString())
            }
            PhaseType.COMMUNITY_CHECK -> {
                saveLog(getString(R.string.DRAFT_PLAYBACK) + " Slide " + startSlide.toString())
            }
            else -> {
                // Do nothing!
            }
        }
    }
}
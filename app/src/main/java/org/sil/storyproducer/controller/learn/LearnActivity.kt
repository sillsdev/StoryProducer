package org.sil.storyproducer.controller.learn

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLearnLog
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*

import kotlin.math.max
import kotlin.math.min

class LearnActivity : PhaseBaseActivity() {

    private var rootView: RelativeLayout? = null
    private var learnImageView: ImageView? = null
    private var playButton: ImageButton? = null
    private var videoSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var narrationPlayer: AudioPlayer = AudioPlayer()

    private var isVolumeOn = true
    private var isWatchedOnce = false

    //recording toolbar vars
    private var recordingToolbar: RecordingToolbar? = null

    private var isFirstTime = true         //used to know if it is the first time the activity is started up for playing the vid

    private var numOfSlides: Int = 0
    private var startPos = -1
    private var startTime: Long = -1
    private var curPos: Int = 0
    private val slideDurations: MutableList<Int> = ArrayList()
    private val slideStartTimes: MutableList<Int> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        rootView = findViewById(R.id.phase_frame)
        learnImageView = findViewById(R.id.fragment_image_view)
        playButton = findViewById(R.id.fragment_reference_audio_button)

        //setup seek bar listenters
        videoSeekBar = findViewById(R.id.videoSeekBar)
        videoSeekBar!!.max = numOfSlides  //only as long as there are numbered pages and titles.
        videoSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {}
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if(narrationPlayer!!.isAudioPlaying){
                        pauseStoryAudio()
                        playStoryAudio()
                    }else{
                        setSlideFromSeekbar()
                    }
                }
            }
        })

        //setup recording toolbar callbacks
        recordingToolbar = RecordingToolbar(this,
                layoutInflater.inflate(R.layout.toolbar_for_recording, rootView, false),
                rootView!!, true, false, false, false,
                null, object : RecordingToolbar.RecordingListener {
            override fun onStoppedRecording() {
                videoSeekBar!!.progress = 0
                curPos = 0
            }
            override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                pauseStoryAudio()
                videoSeekBar!!.progress = 0
                curPos = 0
            }
        }, 0)
        recordingToolbar!!.keepToolbarVisible()

        //setup volume switch callbacks
        val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
        //set the volume switch change listener
        volumeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                narrationPlayer.setVolume(1.0f)
                isVolumeOn = true
            } else {
                narrationPlayer.setVolume(0.0f)
                isVolumeOn = false
            }
        }

        //has learn already been watched?
        if (storyRelPathExists(this,Workspace.activeStory.learnAudioFile)) {
            val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
            volumeSwitch.isChecked = true
            isWatchedOnce = true
        }

        //get story audio duration
        numOfSlides = 0
        slideStartTimes.add(0)
        for (s in story.slides) {
            //don't play the copyright slides.
            if (s.slideType in arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE)) {
                numOfSlides++
                slideDurations.add((MediaHelper.getAudioDuration(this,
                        getStoryUri(s.narrationFile)!!) / 1000).toInt())
                slideStartTimes.add(slideStartTimes.last() + slideDurations.last())
            } else {
                break
            }
        }

        invalidateOptionsMenu()
    }


    public override fun onPause() {
        super.onPause()
        pauseStoryAudio()
        narrationPlayer.release()
        recordingToolbar!!.onPause()
        recordingToolbar!!.close()
    }

    public override fun onResume() {
        super.onResume()

        narrationPlayer = AudioPlayer()
        narrationPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if(videoSeekBar!!.progress >= videoSeekBar!!.max){
                //at the end of video so special case
                pauseStoryAudio()
                showStartPracticeSnackBar()
            } else {
                //just play the next slide!
                videoSeekBar?.progress = slideStartTimes[curPos+1]
                playStoryAudio()
            }
        })

        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread{
                    if(recordingToolbar!!.isRecording){
                        videoSeekBar?.progress = min((System.currentTimeMillis() - startTime).toInt(),videoSeekBar!!.max)
                        setSlideFromSeekbar()
                    }else{
                        videoSeekBar?.progress = slideStartTimes[curPos] + narrationPlayer.currentPosition
                    }
                }
            }
        },0,33)
    }

    private fun setSlideFromSeekbar() {
        val time = videoSeekBar!!.progress
        var i = 0
        for (d in slideStartTimes) {
            if (time < d) {
                if(i-1 != curPos){
                    curPos = i-1
                    setPic(learnImageView!!, curPos)
                    narrationPlayer.setStorySource(this, Workspace.activeStory.slides[curPos].narrationFile)
                }
                if(slideStartTimes.size > curPos)
                    videoSeekBar!!.progress = slideStartTimes[curPos]

                narrationPlayer.seekTo(time - slideStartTimes[curPos])
                break
            }
            i++
        }
    }

    private fun markLogStart() {
        startPos = videoSeekBar!!.progress
        startTime = System.currentTimeMillis()
    }

    private fun makeLogIfNecessary(request: Boolean = false) {
        if (narrationPlayer.isAudioPlaying || request) {
            if (startPos != -1) {
                val duration = System.currentTimeMillis() - startTime
                if(duration > 500){
                    saveLearnLog(this, startPos,videoSeekBar!!.progress, duration)
                }
                startPos = -1
            }
        }
    }

    /**
     * Button action for playing/pausing the audio
     * @param view button to set listeners for
     */
    fun onClickPlayPauseButton(view: View) {
        if (narrationPlayer.isAudioPlaying) {
            pauseStoryAudio()
        } else {
            if (videoSeekBar!!.progress >= videoSeekBar!!.max) {        //reset the video to the beginning because they already finished it
                videoSeekBar!!.progress = 0
            }
            playStoryAudio()
        }
    }

    /**
     * Plays the audio
     */
    internal fun playStoryAudio() {
        recordingToolbar!!.onPause()
        setSlideFromSeekbar()
        narrationPlayer.pauseAudio()
        markLogStart()
        narrationPlayer.setVolume(if (isVolumeOn) 1.0f else 0.0f) //set the volume on or off based on the boolean
        narrationPlayer.playAudio()
        playButton!!.setImageResource(R.drawable.ic_pause_white_48dp)
    }

    /**
     * helper function for pausing the video
     */
    private fun pauseStoryAudio() {
        makeLogIfNecessary()
        narrationPlayer.pauseAudio()
        playButton!!.setImageResource(R.drawable.ic_play_arrow_white_48dp)
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user that they should practice saying the story
     */
    private fun showStartPracticeSnackBar() {
        if (!isWatchedOnce) {
            val snackbar = Snackbar.make(findViewById(R.id.drawer_layout),
                    R.string.learn_phase_practice, Snackbar.LENGTH_INDEFINITE)
            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.lightWhite, null))
            val textView = snackBarView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            snackbar.show()
        }
        isWatchedOnce = true
    }
}

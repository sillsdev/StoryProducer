package org.sil.storyproducer.controller

import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLearnLog
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*
import kotlin.math.min

abstract class WholeStoryActivity : PhaseBaseActivity(), RecordingToolbar.RecordingListener {

    private var learnImageView: ImageView? = null
    private var playButton: ImageButton? = null
    private var videoSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var narrationPlayer = AudioPlayer()

    private var isVolumeOn = true
    private var isWatchedOnce = false

    private var recordingToolbar: RecordingToolbar = RecordingToolbar()

    private var numOfSlides: Int = 0
    private var seekbarStartTime: Long = -1
    private var logStartTime: Long = -1
    private var curPos: Int = -1 //set to -1 so that the first slide will register as "different"
    private val slideDurations: MutableList<Int> = ArrayList()
    private val slideStartTimes: MutableList<Int> = ArrayList()

    private var isLogging = false
    private var startPos = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        setToolbar()

        learnImageView = findViewById(R.id.fragment_image_view)
        playButton = findViewById(R.id.fragment_reference_audio_button)

        val playListener = View.OnClickListener {
            if (narrationPlayer.isAudioPlaying) {
                pauseStoryAudio()
            } else {
                if (videoSeekBar!!.progress >= videoSeekBar!!.max-100) {
                    //reset the video to the beginning because they already finished it (within 100 ms)
                    videoSeekBar!!.progress = 0
                }
                playStoryAudio()
            }
        }
        playButton?.setOnClickListener(playListener)

        //setup seek bar listenters
        videoSeekBar = findViewById(R.id.videoSeekBar)
        videoSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {}
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (recordingToolbar.isRecordingOrPlaying) {
                        //When recording, update the picture to the accurate location, preserving
                        seekbarStartTime = System.currentTimeMillis() - videoSeekBar!!.progress
                        setSlideFromSeekbar()
                    } else {
                        if (narrationPlayer.isAudioPlaying) {
                            pauseStoryAudio()
                            playStoryAudio()
                        } else {
                            setSlideFromSeekbar()
                        }
                        //always start at the beginning of the slide.
                        if (slideStartTimes.size > curPos)
                            videoSeekBar!!.progress = slideStartTimes[curPos]
                    }
                }
            }
        })

        //setup volume switch callbacks
        val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
        //set the volume switch change listener
        volumeSwitch.isChecked = true
        volumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isVolumeOn = if (isChecked) {
                narrationPlayer.setVolume(1.0f)
                true
            } else {
                narrationPlayer.setVolume(0.0f)
                false
            }
        }

        //has learn already been watched?
        isWatchedOnce = isWatchedOnce()

        //get story audio duration
        numOfSlides = 0
        slideStartTimes.add(0)
        for (s in story.slides) {
            //don't play the copyright slides.
            if (s.slideType in arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE)) {
                numOfSlides++
                slideDurations.add((MediaHelper.getAudioDuration(this,
                        getStoryUri(getAudioFileFromSlide(s))!!) / 1000).toInt())
                slideStartTimes.add(slideStartTimes.last() + slideDurations.last())
            } else {
                break
            }
        }
        videoSeekBar?.max = slideStartTimes.last()

        invalidateOptionsMenu()
    }

    abstract fun getAudioFileFromSlide(slide: Slide) : String
    abstract fun getToolbarBooleanArray() : BooleanArray
    abstract fun isWatchedOnce() : Boolean

    private fun setToolbar(){
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", getToolbarBooleanArray())
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        supportFragmentManager?.beginTransaction()?.replace(R.id.toolbar_for_recording_toolbar, recordingToolbar)?.commit()

        recordingToolbar.keepToolbarVisible()
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        if(isRecording){
            makeLogIfNecessary(true)
        }
        videoSeekBar!!.progress = 0
        setSlideFromSeekbar()
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        pauseStoryAudio()
        videoSeekBar!!.progress = 0
        curPos = 0
        //This gets the progress bar to show the right time.
        seekbarStartTime = System.currentTimeMillis()
        if(isRecording){
            markLogStart()
        }
    }

    public override fun onPause() {
        super.onPause()
        pauseStoryAudio()
        narrationPlayer.release()
    }

    public override fun onResume() {
        super.onResume()

        narrationPlayer = AudioPlayer()
        narrationPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if (narrationPlayer.isAudioPrepared) {
                if (curPos >= numOfSlides - 1) { //is it the last slide?
                    //at the end of video so special case
                    pauseStoryAudio()
                    showStartPracticeSnackBar()
                } else {
                    //just play the next slide!
                    videoSeekBar?.progress = slideStartTimes[curPos + 1]
                    playStoryAudio()
                }
            }
        })

        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread{
                    if(recordingToolbar.isRecordingOrPlaying){
                        videoSeekBar?.progress = min((System.currentTimeMillis() - seekbarStartTime).toInt(),videoSeekBar!!.max)
                        setSlideFromSeekbar()
                    }else{
                        if(curPos >= 0) videoSeekBar?.progress = slideStartTimes[curPos] + narrationPlayer.currentPosition
                    }
                }
            }
        },0,33)

        setSlideFromSeekbar()
    }

    private fun setSlideFromSeekbar() {
        val time = videoSeekBar!!.progress
        var i = 0
        for (d in slideStartTimes) {
            if (time < d) {
                if(i-1 != curPos){
                    curPos = i-1
                    setPic(learnImageView!!, curPos)
                    narrationPlayer.setStorySource(this, getAudioFileFromSlide(Workspace.activeStory.slides[curPos]))
                }
                break
            }
            i++
        }
    }

    private fun markLogStart() {
        if(!isLogging) {
            startPos = curPos
            logStartTime = System.currentTimeMillis()
        }
        isLogging = true
    }

    private fun makeLogIfNecessary(isRecording: Boolean = false) {
        if (isLogging) {
            if (startPos != -1) {
                val duration: Long = System.currentTimeMillis() - logStartTime
                if(duration > 2000){ //you need 2 seconds to listen to anything
                    saveLearnLog(this, startPos,curPos, duration, isRecording)
                }
                startPos = -1
            }
        }
        isLogging = false
    }

    /**
     * Plays the audio
     */
    internal fun playStoryAudio() {
        recordingToolbar.stopToolbarMedia()
        setSlideFromSeekbar()
        narrationPlayer.pauseAudio()
        markLogStart()
        seekbarStartTime = System.currentTimeMillis()
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
                    R.string.learn_phase_practice, Snackbar.LENGTH_LONG)
            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.lightWhite, null))
            val textView = snackBarView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            snackbar.show()
        }
        isWatchedOnce = true
    }
}
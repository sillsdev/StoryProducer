package org.sil.storyproducer.controller.learn

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.SnackbarManager
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLearnLog
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.io.File
import java.util.*
import kotlin.math.min

class LearnActivity : PhaseBaseActivity(), PlayBackRecordingToolbar.ToolbarMediaListener {
    private var learnImageView: ImageView? = null
    private var playButton: ImageButton? = null
    private var videoSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var narrationPlayer: AudioPlayer = AudioPlayer()

    private var isVolumeOn = true
    private var isWatchedOnce = false

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()

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

        //setup seek bar listenters
        videoSeekBar = findViewById(R.id.videoSeekBar)
        videoSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {}
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (recordingToolbar.isRecording || recordingToolbar.isAudioPlaying) {
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
        isWatchedOnce = storyRelPathExists(this,Workspace.activeStory.learnAudioFile)

        //get story audio duration
        numOfSlides = 0
        slideStartTimes.add(0)
        for (s in story.slides) {
            //don't play the copyright slides.
            if (s.slideType in arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE)) {
                numOfSlides++
                if (s.narrationFile.isNotEmpty()) {
                    slideDurations.add((MediaHelper.getAudioDuration(this,
                            getStoryUri(Story.getFilename(s.narrationFile))!!) / 1000).toInt())
                } else {
                    slideDurations.add(AudioPlayer.dummyDurationInMilliseconds) // Add a dummy 2s slide for missing audio message
                }
                slideStartTimes.add(slideStartTimes.last() + slideDurations.last())
            } else {
                break
            }
        }
        videoSeekBar?.max = slideStartTimes.last()

        // hide scripture text view as only needed if no audio
        val scriptureTextView = findViewById<TextView>(R.id.fragment_scripture_text)
        scriptureTextView.visibility = View.INVISIBLE

        // Adjust the Guideline control to scale the video view for 4:3 videos (also used for 16:9 videos)
        findViewById<Guideline>(R.id.guideline)
            ?.setGuidelineBegin((resources.displayMetrics.widthPixels.toFloat() / 4f * 3f).toInt())

        invalidateOptionsMenu()

        addAndStartPopupTutorials()

    }



    private fun addAndStartPopupTutorials() {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        mPopupHelpUtils = PopupHelpUtils(this)

        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 90,
            R.string.help_learn_phase_title, R.string.help_learn_phase_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.fragment_reference_audio_button,
            80, 90,
            R.string.help_learn_listen_title, R.string.help_learn_listen_body) {
                Workspace.activeStory.activityLogs.firstOrNull {
                    it.phase.phaseType == PhaseType.LEARN && it.description.contains("Playback Slide")  // TODO: LOCALIZATION: Temp string
            } != null
        }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.start_recording_button,
            50, 10,
            R.string.help_learn_practice_title, R.string.help_learn_practice_body) {
                Workspace.activeStory.learnAudioFile.isNotEmpty() && !recordingToolbar.isRecording
        }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.play_recording_button,
            50, 10,
            R.string.help_learn_listen2_title, R.string.help_learn_listen2_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 90,
            R.string.help_learn_next_phase_title, R.string.help_learn_next_phase_body)

        mPopupHelpUtils?.showNextPopupHelp()

    }

    public override fun onPause() {
        super.onPause()
        pauseStoryAudio()
        narrationPlayer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()

        narrationPlayer = AudioPlayer()
        narrationPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if(narrationPlayer.isAudioPrepared){
                if(curPos >= numOfSlides-1){ //is it the last slide?
                    //at the end of video so special case
                    pauseStoryAudio()
                    showStartPracticeSnackBar()
                    val scriptureTextView = findViewById<TextView>(R.id.fragment_scripture_text)
                    scriptureTextView.visibility = View.INVISIBLE
                } else {
                    //just play the next slide!
                    videoSeekBar?.progress = slideStartTimes[curPos+1]
                    playStoryAudio()
                }
            }
        })

        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread{
                    if(recordingToolbar.isRecording || recordingToolbar.isAudioPlaying){
                        videoSeekBar?.progress = min((System.currentTimeMillis() - seekbarStartTime).toInt(),videoSeekBar!!.max)
                        setSlideFromSeekbar()
                    }else{
                        if(curPos >= 0) videoSeekBar?.progress = slideStartTimes[curPos] + narrationPlayer.currentPosition
                    }
                }
            }
        },0,33)

        setSlideFromSeekbar()

//        Moved to OnCreate() to fix double display on new install permissions request
//        addAndStartPopupTutorials()
    }


    private fun setSlideFromSeekbar() {
        val time = videoSeekBar!!.progress
        var i = 0
        for (d in slideStartTimes) {
            if (time < d) {
                // if the current position is different or if the audio is not prepared
                // then setup the story source audio (fixes notion bug 816)
                if (i > 0 && i <= Workspace.activeStory.slides.size) {
                    if (i - 1 != curPos || !narrationPlayer.isAudioPrepared) {
                        curPos = i - 1
                        setPic(learnImageView!!, curPos)
                        val narrationFile = Workspace.activeStory.slides[curPos].narrationFile
                        if (narrationFile.isEmpty())
                            narrationPlayer.setSource(this, Uri.fromFile(File(""))) // set empty source for dummy 2s audio playback
                        else
                            narrationPlayer.setStorySource(this, Workspace.activeStory.slides[curPos].narrationFile)
                    }
                    break
                }
            }
            i++
        }
    }


    private fun setToolbar(){
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    override fun onStoppedToolbarRecording() {
        makeLogIfNecessary(true)
        super.onStoppedToolbarRecording()

//        mPopupHelpUtils.showNextPopupHelp()
    }

    override fun onStartedToolbarRecording() {
        super.onStartedToolbarRecording()

        markLogStart()

//        mPopupHelpUtils.showNextPopupHelp()
    }

    override fun onStoppedToolbarMedia() {
        videoSeekBar!!.progress = 0
        setSlideFromSeekbar()
    }

    override fun onStartedToolbarMedia() {
        pauseStoryAudio()
        videoSeekBar!!.progress = 0
        curPos = -1
        //This gets the progress bar to show the right time.
        seekbarStartTime = System.currentTimeMillis()
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
     * Button action for playing/pausing the audio
     * @param view button to set listeners for
     */
    fun onClickPlayPauseButton(@Suppress("UNUSED_PARAMETER") view: View) {
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

    private fun setScriptureText(textView: TextView) {
        if (curPos < 0 || curPos >= story.slides.size)
            return
        val slide = story.slides[curPos]
        textView.text = slide.content
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
        val scriptureTextView = findViewById<TextView>(R.id.fragment_scripture_text)
        if (narrationPlayer.isDummyAudio) { // if this slide has dummy audio show missing audio message
            SnackbarManager.show(findViewById(R.id.activity_learn),
                    getString(R.string.translate_revise_playback_no_lwc_audio, Workspace.activeStory.langCode),
                                AudioPlayer.dummyDurationInMilliseconds.toInt() - 600)
                    ?.setAction("Action", null)?.show()
            scriptureTextView.visibility = View.VISIBLE
            setScriptureText(scriptureTextView)
        } else {
            scriptureTextView.visibility = View.INVISIBLE
        }
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
            val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            SnackbarManager.saveNewSnackbar(snackbar)   // dismiss any existing snackbar and remember this one
            snackbar.show()
        }
        isWatchedOnce = true
    }
}

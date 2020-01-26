package org.sil.storyproducer.controller.learn

import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.util.*

class LearnFragment : Fragment(), PlayBackRecordingToolbar.ToolbarMediaListener {

    class DraftSlide(slideNum: Int, duration: Int, startTime: Int, filename: String) {
        val slideNum: Int = slideNum
        val duration: Int = duration
        val startTime: Int = startTime
        val filename: String = filename
    }

    private lateinit var rootView: View

    private lateinit var learnImageView: ImageView
    private lateinit var playButton: ImageButton
    private lateinit var seekBar: SeekBar

    private var mSeekBarTimer = Timer()
    private var narrationPlayer: AudioPlayer = AudioPlayer()
    private var seekbarStartTime: Long = -1

    private var isVolumeOn = true
    private var isWatchedOnce = false

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()

    private var currentSlideIndex: Int = 0
    private val slides: MutableList<DraftSlide> = ArrayList()

    private var logStartTime: Long = -1
    private var isLogging = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        rootView = inflater.inflate(R.layout.activity_learn, container, false)

        // Insert toolbar fragment into UI
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()
        recordingToolbar.keepToolbarVisible()

        learnImageView = rootView.findViewById(R.id.fragment_image_view)
        playButton = rootView.findViewById(R.id.fragment_reference_audio_button)
        seekBar = rootView.findViewById(R.id.videoSeekBar)

        playButton.setOnClickListener {
            if (narrationPlayer.isAudioPlaying) {
                pauseStoryAudio()
            } else {
                if (seekBar.progress >= seekBar.max - 100) {
                    //reset the video to the beginning because they already finished it (within 100 ms)
                    seekBar.progress = 0
                }
                playStoryAudio()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var wasPlayingBeforeTouch = false
            override fun onStopTrackingTouch(sBar: SeekBar) {
                if (wasPlayingBeforeTouch) {
                    // Always start at the beginning of the slide.
                    if (currentSlideIndex < slides.size) {
                        seekBar.progress = slides[currentSlideIndex].startTime
                    }
                    playStoryAudio()
                }
            }

            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasPlayingBeforeTouch = narrationPlayer.isAudioPlaying
            }

            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                setSlideFromSeekbar()
                //if (fromUser) {
                //    if (recordingToolbar.isRecording || recordingToolbar.isAudioPlaying) {
                //        //When recording, update the picture to the accurate location, preserving
                //        seekbarStartTime = System.currentTimeMillis() - videoSeekBar!!.progress
                //        setSlideFromSeekbar()
                //    } else {
                //        if (narrationPlayer.isAudioPlaying) {
                //            pauseStoryAudio()
                //            playStoryAudio()
                //        } else {
                //            setSlideFromSeekbar()
                //        }
                //        //always start at the beginning of the slide.
                //        if (slideStartTimes.size > curPos)
                //            videoSeekBar!!.progress = slideStartTimes[curPos]
                //    }
                //}
            }
        })

        val volumeSwitch = rootView.findViewById<Switch>(R.id.volumeSwitch)
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
        isWatchedOnce = Workspace.activeStory.learnAudioFile != null

        //get story audio duration
        var lastEndTime = 0
        Workspace.activeStory.slides.forEachIndexed { slideNum, slide ->
            // Don't play the copyright slides.
            if (slide.slideType == SlideType.FRONTCOVER || slide.slideType == SlideType.NUMBEREDPAGE) {
                val filename = slide.narration?.fileName
                if (filename != null) {
                    val duration = (MediaHelper.getAudioDuration(context!!, getStoryUri(filename)!!) / 1000).toInt()
                    val startTime = lastEndTime
                    lastEndTime = startTime + duration
                    slides.add(DraftSlide(slideNum, duration, startTime, filename))
                }
            }
        }

        seekBar.max = if (slides.isNotEmpty()) {
            val lastSlide = slides.last()
            lastSlide.startTime + lastSlide.duration
            slides.last().startTime
        } else {
            0
        }
        seekBar.progress = 0
        setSlideFromSeekbar()



        return rootView
    }

    override fun onPause() {
        super.onPause()
        pauseStoryAudio()
        narrationPlayer.release()
    }

    override fun onResume() {
        super.onResume()

        narrationPlayer = AudioPlayer()
        narrationPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if (narrationPlayer.isAudioPrepared) {
                if (currentSlideIndex >= slides.size - 1) { //is it the last slide?
                    //at the end of video so special case
                    pauseStoryAudio()
                } else {
                    //just play the next slide!
                    seekBar.progress = slides[currentSlideIndex + 1].startTime
                    playStoryAudio()
                }
            }
        })

        val learnActivity = activity!!
        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                learnActivity.runOnUiThread {
                    if (recordingToolbar.isRecording || recordingToolbar.isAudioPlaying) {
                        seekBar.progress = minOf((System.currentTimeMillis() - seekbarStartTime).toInt(), seekBar.max)
                    } else if (narrationPlayer.isAudioPrepared) {
                        seekBar.progress = slides[currentSlideIndex].startTime + narrationPlayer.currentPosition
                    } else {
                        seekBar.progress = 0
                    }
                }
            }
        }, 0, 33)

        setSlideFromSeekbar()
    }

    private fun setSlideFromSeekbar() {
        if (slides.isNotEmpty()) {
            val time = seekBar.progress
            var slideIndexBeforeSeekBar = slides.indexOfLast { it.startTime <= time }
            if (slideIndexBeforeSeekBar != currentSlideIndex || !narrationPlayer.isAudioPrepared) {
                currentSlideIndex = slideIndexBeforeSeekBar
                val slide = slides[currentSlideIndex]
                PhaseBaseActivity.setPic(context!!, learnImageView, slide.slideNum)
                narrationPlayer.setStorySource(context!!, slide.filename)
            }
        }
    }


    override fun onStoppedToolbarRecording() {
        makeLogIfNecessary(true)

        super.onStoppedToolbarRecording()
    }

    override fun onStartedToolbarRecording() {
        super.onStartedToolbarRecording()

        markLogStart()
    }

    override fun onStoppedToolbarMedia() {
        seekBar.progress = 0
        setSlideFromSeekbar()
    }

    override fun onStartedToolbarMedia() {
        pauseStoryAudio()
        seekBar.progress = 0
        currentSlideIndex = 0

        //This gets the progress bar to show the right time.
        seekbarStartTime = System.currentTimeMillis()
    }

    private fun markLogStart() {
        if (!isLogging) {
            //startPos = curPos
            logStartTime = System.currentTimeMillis()
        }
        isLogging = true
    }

    private fun makeLogIfNecessary(isRecording: Boolean = false) {
        if (isLogging) {
//            if (startPos != -1) {
                val duration: Long = System.currentTimeMillis() - logStartTime
                if (duration > 2000) { //you need 2 seconds to listen to anything
                    //saveLearnLog(this, startPos, curPos, duration, isRecording)
                }
                //startPos = -1
//            }
        }
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
        playButton.setImageResource(R.drawable.ic_pause_white_48dp)
    }

    /**
     * helper function for pausing the video
     */
    private fun pauseStoryAudio() {
        makeLogIfNecessary()
        narrationPlayer.pauseAudio()
        playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user that they should practice saying the story
     */
    private fun showStartPracticeSnackBar() {
        if (!isWatchedOnce) {
            val snackbar = Snackbar.make(rootView.findViewById(R.id.drawer_layout),
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

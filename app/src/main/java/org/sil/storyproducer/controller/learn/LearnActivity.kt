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

    class DraftSlide(slideNumber: Int, duration: Int, startTime: Int, filename: String) {
        val slideNumber: Int = slideNumber
        val duration: Int = duration
        val startTime: Int = startTime
        val filename: String = filename
    }

    private lateinit var rootView: View

    private lateinit var learnImageView: ImageView
    private lateinit var playButton: ImageButton
    private lateinit var seekBar: SeekBar

    private var mSeekBarTimer = Timer()
    private var narrationAudioPlayer: AudioPlayer = AudioPlayer()
    private var seekbarStartTime: Long = -1

    private var isVolumeOn = true
    private var userHasBegunListening = false
    private var viewIsCreated = false

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
            if (narrationAudioPlayer.isAudioPlaying) {
                pauseStoryAudio()
            } else {
                // If the video is basically already finished, restart it.
                if (seekBar.progress >= seekBar.max - 100) {
                    seekBar.progress = 0
                }
                playStoryAudio()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var wasPlayingBeforeTouch = false
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (wasPlayingBeforeTouch) {
                    // Always start at the beginning of the slide.
                    if (currentSlideIndex < slides.size) {
                        seekBar.progress = slides[currentSlideIndex].startTime
                    }
                    playStoryAudio()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                wasPlayingBeforeTouch = narrationAudioPlayer.isAudioPlaying
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress > 2000 || progress > seekBar.max - 100) {
                    userHasBegunListening = true
                }
                if (fromUser) {
                    setSlideFromSeekbar()
                }
            }
        })

        val volumeSwitch = rootView.findViewById<Switch>(R.id.volumeSwitch)
        volumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isVolumeOn = if (isChecked) {
                narrationAudioPlayer.setVolume(1.0f)
                true
            } else {
                narrationAudioPlayer.setVolume(0.0f)
                false
            }
        }

        // Compute story audio duration
        var lastEndTime = 0
        Workspace.activeStory.slides.forEachIndexed { slideNumber, slide ->
            // Don't play the copyright slides.
            if (slide.slideType == SlideType.FRONTCOVER || slide.slideType == SlideType.NUMBEREDPAGE) {
                val filename = slide.narration?.fileName
                if (filename != null) {
                    val duration = (MediaHelper.getAudioDuration(context!!, getStoryUri(filename)!!) / 1000).toInt()
                    val startTime = lastEndTime
                    lastEndTime = startTime + duration
                    slides.add(DraftSlide(slideNumber, duration, startTime, filename))
                }
            }
        }

        seekBar.max = if (slides.isNotEmpty()) {
            val lastSlide = slides.last()
            lastSlide.startTime + lastSlide.duration
        } else {
            0
        }
        seekBar.progress = 0
        setSlideFromSeekbar()

        viewIsCreated = true
        return rootView
    }

    override fun onPause() {
        super.onPause()
        narrationAudioPlayer.release()
    }
    override fun onResume() {
        super.onResume()
        narrationAudioPlayer = AudioPlayer()
        narrationAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if (narrationAudioPlayer.isAudioPrepared) {
                // If the video has reached the end, then pause; otherwise,
                // just play the next slide.
                if (currentSlideIndex >= slides.size - 1) {
                    pauseStoryAudio()
                } else {
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
                    if (recordingToolbar.isRecording) {
                        setSlideFromSeekbar()
                    }
                    if (recordingToolbar.isRecording || recordingToolbar.isAudioPlaying) {
                        seekBar.progress = minOf((System.currentTimeMillis() - seekbarStartTime).toInt(), seekBar.max)
                    } else if (narrationAudioPlayer.isAudioPrepared) {
                        seekBar.progress = slides[currentSlideIndex].startTime + narrationAudioPlayer.currentPosition
                    } else {
                        seekBar.progress = 0
                    }
                }
            }
        }, 0, 33)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (viewIsCreated) {
            if (isVisibleToUser) {
                showStartPracticeSnackBar()
            } else {
                pauseStoryAudio()
            }
        }
    }

    private fun setSlideFromSeekbar() {
        if (slides.isNotEmpty()) {
            val time = seekBar.progress
            var slideIndexBeforeSeekBar = slides.indexOfLast { it.startTime <= time }
            if (slideIndexBeforeSeekBar != currentSlideIndex || !narrationAudioPlayer.isAudioPrepared) {
                currentSlideIndex = slideIndexBeforeSeekBar
                val slide = slides[currentSlideIndex]
                PhaseBaseActivity.setPic(context!!, learnImageView, slide.slideNumber)
                narrationAudioPlayer.setStorySource(context!!, slide.filename)
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

    internal fun playStoryAudio() {
        recordingToolbar.stopToolbarMedia()
        setSlideFromSeekbar()
        narrationAudioPlayer.pauseAudio()
        markLogStart()
        seekbarStartTime = System.currentTimeMillis()
        narrationAudioPlayer.setVolume(if (isVolumeOn) 1.0f else 0.0f)
        narrationAudioPlayer.playAudio()
        playButton.setImageResource(R.drawable.ic_pause_white_48dp)
    }

    private fun pauseStoryAudio() {
        makeLogIfNecessary()
        narrationAudioPlayer.pauseAudio()
        playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
    }

    private fun showStartPracticeSnackBar() {
        if (!userHasBegunListening) {
            val snackbar = Snackbar.make(activity!!.findViewById(R.id.drawer_layout),
                    R.string.learn_phase_practice, Snackbar.LENGTH_LONG)
            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.lightWhite, null))
            val textView = snackBarView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            snackbar.show()
        }
    }
}

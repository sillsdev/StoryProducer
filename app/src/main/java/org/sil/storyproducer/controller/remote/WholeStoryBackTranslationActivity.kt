// TODO @pwhite: WholeStoryBackTranslationActivity and LearnActivity are
// extremely similar. The latter allows viewing of a *template*, and the former
// allows viewing of a *story*, which is essentially a translated template. The
// major difference is that the backtranslation should also allow uploading,
// but this is does not prevent us from extracting the common functionality.
package org.sil.storyproducer.controller.remote

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by annmcostantino on 1/14/2018.
 *
 * An interface for doing back translations on the whole story. There is an image and a seekbar
 * which provide a UI for watching the video with both slides and audio. There is also a recording
 * toolbar for recording and uploading audio. It is us
 */

class WholeStoryBackTranslationActivity : PhaseBaseActivity(), PlayBackRecordingToolbar.ToolbarMediaListener {

    class DraftSlide(slideNum: Int, duration: Int, startTime: Int, filename: String) {
        val slideNum: Int = slideNum
        val duration: Int = duration
        val startTime: Int = startTime
        val filename: String = filename
    }

    private lateinit var wholeStoryImageView: ImageView
    private lateinit var playButton: ImageButton
    private lateinit var seekBar: SeekBar

    private var mSeekBarTimer = Timer()
    private var draftPlayer: AudioPlayer = AudioPlayer()
    private var seekbarStartTime: Long = -1

    private var isVolumeOn = true

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()

    private var currentSlideIndex: Int = -1
    private val translatedSlides: MutableList<DraftSlide> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        supportFragmentManager?.beginTransaction()?.replace(R.id.toolbar_for_recording_toolbar, recordingToolbar)?.commit()
        recordingToolbar.keepToolbarVisible()

        wholeStoryImageView = findViewById(R.id.fragment_image_view)
        playButton = findViewById(R.id.fragment_reference_audio_button)
        seekBar = findViewById(R.id.videoSeekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var wasPlayingBeforeTouch = false
            override fun onStopTrackingTouch(sBar: SeekBar) {
                if (wasPlayingBeforeTouch) {
                    // Always start at the beginning of the slide.
                    if (currentSlideIndex < translatedSlides.size) {
                        seekBar.progress = translatedSlides[currentSlideIndex].startTime
                    }
                    playStoryAudio()
                }
            }

            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasPlayingBeforeTouch = draftPlayer.isAudioPlaying
            }

            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Log.e("@pwhite", "progress changed to $progress")
                }
                setSlideFromSeekbar()
            }
        })

        val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
        volumeSwitch.isChecked = true
        volumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isVolumeOn = if (isChecked) {
                draftPlayer.setVolume(1.0f)
                true
            } else {
                draftPlayer.setVolume(0.0f)
                false
            }
        }

        //get story audio duration
        var lastEndTime = 0
        story.slides.forEachIndexed { slideNum, slide ->
            // Don't play the copyright translatedSlides.
            if (slide.slideType == SlideType.FRONTCOVER || slide.slideType == SlideType.NUMBEREDPAGE) {
                val filename = Story.getFilename(slide.chosenDraftFile)
                if (storyRelPathExists(this, filename)) {
                    val duration = (MediaHelper.getAudioDuration(this, getStoryUri(filename)!!) / 1000).toInt()
                    val startTime = lastEndTime
                    lastEndTime = startTime + duration
                    translatedSlides.add(DraftSlide(slideNum, duration, startTime, filename))
                }
            }
        }

        seekBar.max = translatedSlides.last().startTime
        seekBar.progress = 0
        setSlideFromSeekbar()

        invalidateOptionsMenu()
    }

    public override fun onPause() {
        super.onPause()
        pauseStoryAudio()
        draftPlayer.release()
    }

    public override fun onResume() {
        super.onResume()
        draftPlayer = AudioPlayer()
        draftPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            Log.e("@pwhite", "curSlide is $currentSlideIndex")
            if (draftPlayer.isAudioPrepared) {
                if (currentSlideIndex >= translatedSlides.size - 1) { //is it the last slide?
                    //at the end of video so special case
                    pauseStoryAudio()
                } else {
                    //just play the next slide!
                    seekBar.progress = translatedSlides[currentSlideIndex + 1].startTime
                    playStoryAudio()
                }
            }
        })

        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (recordingToolbar.isRecording || recordingToolbar.isAudioPlaying) {
                        seekBar.progress = minOf((System.currentTimeMillis() - seekbarStartTime).toInt(), seekBar.max)
                    } else if (draftPlayer.isAudioPrepared) {
                        seekBar.progress = translatedSlides[currentSlideIndex].startTime + draftPlayer.currentPosition
                    } else {
                        seekBar.progress = 0
                    }
                }
            }
        }, 0, 33)

        setSlideFromSeekbar()
    }

    private fun setSlideFromSeekbar() {
        val time = seekBar.progress
        Log.e("@pwhite", "setSlideFromSeekbar: progress is ${seekBar.progress}, max is ${seekBar.max}")
        var slideIndexBeforeSeekBar = translatedSlides.indexOfLast { it.startTime <= time }
        if (slideIndexBeforeSeekBar != currentSlideIndex || !draftPlayer.isAudioPrepared) {
            currentSlideIndex = slideIndexBeforeSeekBar
            val slide = translatedSlides[currentSlideIndex]
            setPic(wholeStoryImageView, slide.slideNum)
            draftPlayer.setStorySource(this, slide.filename)
            Log.e("@pwhite", "setSlideFromSeekbar: ${slide.filename} ${draftPlayer.isAudioPrepared}")
        } else {
            Log.e("@pwhite", "setSlideFromSeekbar: skipping setStorySource $slideIndexBeforeSeekBar $currentSlideIndex ${draftPlayer.isAudioPrepared}")
        }
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.getItem(0)
        item.setIcon(R.drawable.ic_school_white_48dp)
        return true
    }

    /**
     * Button action for playing/pausing the audio
     * @param view button to set listeners for
     */
    fun onClickPlayPauseButton(view: View) {
        if (draftPlayer.isAudioPlaying) {
            pauseStoryAudio()
        } else {
            if (seekBar.progress >= seekBar.max - 100) {
                //reset the video to the beginning because they already finished it (within 100 ms)
                seekBar.progress = 0
            }
            playStoryAudio()
        }
    }

    /**
     * Plays the audio
     */
    internal fun playStoryAudio() {
        recordingToolbar.stopToolbarMedia()
        setSlideFromSeekbar()
        draftPlayer.pauseAudio()
        seekbarStartTime = System.currentTimeMillis()
        draftPlayer.setVolume(if (isVolumeOn) 1.0f else 0.0f) //set the volume on or off based on the boolean
        Log.e("@pwhite:", "playStoryAudio() here 1")
        draftPlayer.playAudio()
        Log.e("@pwhite:", "playStoryAudio() here 2")
        playButton.setImageResource(R.drawable.ic_pause_white_48dp)
    }

    /**
     * helper function for pausing the video
     */
    private fun pauseStoryAudio() {
        draftPlayer.pauseAudio()
        playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
    }
}


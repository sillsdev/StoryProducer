// TODO @pwhite: WholeStoryBackTranslationActivity and LearnActivity are
// extremely similar. The latter allows viewing of a *template*, and the former
// allows viewing of a *story*, which is essentially a translated template. The
// major difference is that the backtranslation should also allow uploading,
// but this is does not prevent us from extracting the common functionality.
package org.sil.storyproducer.controller.remote

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.VolleyError
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.NetworkResponse;
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.json.JSONException
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Recording
import org.sil.storyproducer.model.UploadState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import org.sil.storyproducer.controller.remote.sendProjectSpecificRequest
import org.sil.storyproducer.controller.remote.sendSlideSpecificRequest
import org.sil.storyproducer.controller.remote.getPhoneId
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.text.Charsets.UTF_8



/**
 * Created by annmcostantino on 1/14/2018.
 *
 * An interface for doing back translations on the whole story. There is an image and a seekbar
 * which provide a UI for watching the video with both slides and audio. There is also a recording
 * toolbar for recording and uploading audio. It is us
 */


class WholeStoryBackTranslationFrag : Fragment(), PlayBackRecordingToolbar.ToolbarMediaListener {

    class DraftSlide(val slideNumber: Int, val duration: Int, val startTime: Int, val filename: String) {}

    private lateinit var wholeStoryImageView: ImageView
    private lateinit var playButton: ImageButton
    private lateinit var seekBar: SeekBar

    private var mSeekBarTimer = Timer()
    private var draftPlayer: AudioPlayer = AudioPlayer()
    private var seekbarStartTime: Long = -1

    private var isVolumeOn = true
    private var viewIsCreated = false

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()
    private lateinit var uploadAudioButtonManager: UploadAudioButtonManager

    private var currentSlideIndex: Int = 0
    private val translatedSlides: MutableList<DraftSlide> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.activity_whole_story, container, false)

        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()
        recordingToolbar.keepToolbarVisible()

        wholeStoryImageView = rootView.findViewById(R.id.fragment_image_view)
        playButton = rootView.findViewById(R.id.fragment_reference_audio_button)
        seekBar = rootView.findViewById(R.id.videoSeekBar)

        playButton.setOnClickListener {
            if (draftPlayer.isAudioPlaying) {
                pauseStoryAudio()
            } else {
                // If the video is basically already finished, restart it.
                if (seekBar.progress >= seekBar.max - 100) {
                    seekBar.progress = 0
                }
                playStoryAudio()
            }
        }

        uploadAudioButtonManager = UploadAudioButtonManager(
            context!!,
            rootView.findViewById(R.id.upload_audio_botton),
            { Workspace.activeStory.wholeStoryBackTranslationUploadState },
            { Workspace.activeStory.wholeStoryBackTranslationUploadState = it },
            { org.sil.storyproducer.tools.file.getChosenFilename() },
            null)

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
                    setSlideFromSeekbar()
                }
            }
        })

        val volumeSwitch = rootView.findViewById<Switch>(R.id.volumeSwitch)
        volumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isVolumeOn = if (isChecked) {
                draftPlayer.setVolume(1.0f)
                true
            } else {
                draftPlayer.setVolume(0.0f)
                false
            }
        }

        // Compute story audio duration
        var lastEndTime = 0
        Workspace.activeStory.slides.forEachIndexed { slideNumber, slide ->
            // Don't play the copyright slides.
            if (slide.slideType == SlideType.FRONTCOVER || slide.slideType == SlideType.NUMBEREDPAGE) {

                var filename: String? = null
                try {
                    val combinedFilename =
                            Workspace.activeStory.slides[slideNumber].chosenTranslateReviseFile
                    val split: Array<String> = combinedFilename.split("|").toTypedArray()
                    filename = split[1]
                } catch(e: Exception) {
                    Toast.makeText(context!!, "No audio file found!!", Toast.LENGTH_LONG).show()
                }

                if (filename != null) {
                    val duration = (MediaHelper.getAudioDuration(context!!, getStoryUri(filename)!!) / 1000).toInt()
                    val startTime = lastEndTime
                    lastEndTime = startTime + duration
                    translatedSlides.add(DraftSlide(slideNumber, duration, startTime, filename))
                }
            }
        }

        seekBar.max = if (translatedSlides.isNotEmpty()) {
            val lastSlide = translatedSlides.last()
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
        draftPlayer.release()
    }

    override fun onResume() {
        super.onResume()
        draftPlayer = AudioPlayer()
        draftPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if (draftPlayer.isAudioPrepared) {
                // If the video has reached the end, then pause; otherwise,
                // just play the next slide.
                if (currentSlideIndex >= translatedSlides.size - 1) {
                    pauseStoryAudio()
                } else {
                    seekBar.progress = translatedSlides[currentSlideIndex + 1].startTime
                    playStoryAudio()
                }
            }
        })

        val currentActivity = activity!!
        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                currentActivity.runOnUiThread {
                    if (recordingToolbar.isRecording) {
                        setSlideFromSeekbar()
                    }
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
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (viewIsCreated && !isVisibleToUser) {
            pauseStoryAudio()
        }
    }

    private fun setSlideFromSeekbar() {
        if (translatedSlides.isNotEmpty()) {
            val time = seekBar.progress
            var slideIndexBeforeSeekBar = translatedSlides.indexOfLast { it.startTime <= time }
            if (slideIndexBeforeSeekBar != currentSlideIndex || !draftPlayer.isAudioPrepared) {

                currentSlideIndex = slideIndexBeforeSeekBar
                val slide = translatedSlides[currentSlideIndex]
                PhaseBaseActivity.setPic(context!!, wholeStoryImageView, slide.slideNumber)
                draftPlayer.setStorySource(context!!, slide.filename)
            }
        }
    }

    override fun onStoppedToolbarRecording() {
        super.onStoppedToolbarRecording()
        Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.NOT_UPLOADED
        uploadAudioButtonManager.refreshBackground()
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

    /**
     * Plays the audio
     */
    internal fun playStoryAudio() {
        recordingToolbar.stopToolbarMedia()
        setSlideFromSeekbar()
        draftPlayer.pauseAudio()
        seekbarStartTime = System.currentTimeMillis()
        draftPlayer.setVolume(if (isVolumeOn) 1.0f else 0.0f) //set the volume on or off based on the boolean
        draftPlayer.playAudio()
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


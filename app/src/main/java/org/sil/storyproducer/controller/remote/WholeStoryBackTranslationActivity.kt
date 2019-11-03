// TODO @pwhite: WholeStoryBackTranslationActivity and LearnActivity are
// extremely similar. The latter allows viewing of a *template*, and the former
// allows viewing of a *story*, which is essentially a translated template. The
// major difference is that the backtranslation should also allow uploading,
// but this is does not prevent us from extracting the common functionality.
package org.sil.storyproducer.controller.remote

import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.support.graphics.drawable.VectorDrawableCompat
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.android.volley.Request
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.BuildConfig

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.UploadState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.BackTranslationUpload.js
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
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
    private lateinit var uploadButton: ImageButton

    private lateinit var greenCheckmark: VectorDrawableCompat
    private lateinit var grayCheckmark: VectorDrawableCompat
    private lateinit var yellowCheckmark: VectorDrawableCompat

    private var mSeekBarTimer = Timer()
    private var draftPlayer: AudioPlayer = AudioPlayer()
    private var seekbarStartTime: Long = -1

    private var isVolumeOn = true

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()

    private var currentSlideIndex: Int = 0
    private val translatedSlides: MutableList<DraftSlide> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whole_story)

        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        supportFragmentManager?.beginTransaction()?.replace(R.id.toolbar_for_recording_toolbar, recordingToolbar)?.commit()
        recordingToolbar.keepToolbarVisible()

        wholeStoryImageView = findViewById(R.id.fragment_image_view)
        playButton = findViewById(R.id.fragment_reference_audio_button)
        seekBar = findViewById(R.id.videoSeekBar)
        uploadButton = findViewById(R.id.upload_audio_botton)

        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)!!
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)!!
        yellowCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_yellow, null)!!
        uploadButton.background = when (Workspace.activeStory.wholeStoryBackTranslationUploadState) {
            UploadState.UPLOADED -> greenCheckmark
            UploadState.NOT_UPLOADED -> grayCheckmark
            UploadState.UPLOADING -> yellowCheckmark
        }

        uploadButton.setOnClickListener {
            when (Workspace.activeStory.wholeStoryBackTranslationUploadState) {
                UploadState.UPLOADED -> Toast.makeText(this, "Selected recording already uploaded", Toast.LENGTH_SHORT).show()
                UploadState.NOT_UPLOADED -> {
                    Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.UPLOADING
                    uploadButton.background = yellowCheckmark
                    val audioRecording = Workspace.activeStory.wholeStoryBackTAudioFile
                    if (audioRecording != null) {

                        Toast.makeText(this, "Uploading audio", Toast.LENGTH_SHORT).show()
                        val input = getStoryChildInputStream(this, audioRecording.fileName)
                        val audioBytes = IOUtils.toByteArray(input)
                        val byteString = android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)
                        val phoneID = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
                        val js = HashMap<String, String>()
                        js["Key"] = getString(R.string.api_token)
                        js["PhoneId"] = phoneID
                        js["TemplateTitle"] = Workspace.activeStory.title
                        js["SlideNumber"] = Workspace.activeSlideNum.toString()
                        js["Data"] = byteString
                        val url = BuildConfig.ROCC_URL_PREFIX + getString(R.string.url_upload_audio)
                        val req = object : paramStringRequest(Method.POST, url, js, {
                            Log.i("LOG_VOLLEY_RESP_UPL", it)
                            Toast.makeText(applicationContext, R.string.audio_Sent, Toast.LENGTH_SHORT).show()
                            Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.UPLOADED
                            uploadButton.background = greenCheckmark
                        }, {
                            Log.e("LOG_VOLLEY_ERR_UPL", it.toString())
                            Log.e("LOG_VOLLEY", "HIT ERROR")
                            Toast.makeText(applicationContext, R.string.audio_Send_Failed, Toast.LENGTH_SHORT).show()
                            Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.NOT_UPLOADED
                            uploadButton.background = grayCheckmark
                        }) {
                            override fun getParams(): Map<String, String> {
                                return this.mParams
                            }
                        }
                        VolleySingleton.getInstance(applicationContext).addToRequestQueue(req)
                    }


                }
                UploadState.UPLOADING -> {
                    uploadButton.background = yellowCheckmark
                    Toast.makeText(this, "Upload already in progress", Toast.LENGTH_SHORT).show()
                }
            }
        }

        uploadButton.setOnLongClickListener {
            when (Workspace.activeStory.wholeStoryBackTranslationUploadState) {
                UploadState.UPLOADING -> {
                    Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(this, "Cancelling upload", Toast.LENGTH_SHORT).show()
                    uploadButton.background = grayCheckmark
                }
                UploadState.UPLOADED -> {
                    Workspace.activeStory.wholeStoryBackTranslationUploadState = UploadState.NOT_UPLOADED
                    Toast.makeText(this, "Ignoring previous upload", Toast.LENGTH_SHORT).show()
                    uploadButton.background = grayCheckmark
                }
                UploadState.NOT_UPLOADED -> Toast.makeText(this, "There have been no uploads yet", Toast.LENGTH_SHORT).show()
            }
            true
        }

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
                val filename = slide.draftRecordings.selectedFile?.fileName
                if (filename != null) {
                    val duration = (MediaHelper.getAudioDuration(this, getStoryUri(filename)!!) / 1000).toInt()
                    val startTime = lastEndTime
                    lastEndTime = startTime + duration
                    translatedSlides.add(DraftSlide(slideNum, duration, startTime, filename))
                }
            }
        }

        seekBar.max = if (translatedSlides.isNotEmpty()) {
            val lastSlide = translatedSlides.last()
            lastSlide.startTime + lastSlide.duration
            translatedSlides.last().startTime
        } else {
            0
        }
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
        if (translatedSlides.isNotEmpty()) {
            val time = seekBar.progress
            var slideIndexBeforeSeekBar = translatedSlides.indexOfLast { it.startTime <= time }
            if (slideIndexBeforeSeekBar != currentSlideIndex || !draftPlayer.isAudioPrepared) {
                currentSlideIndex = slideIndexBeforeSeekBar
                val slide = translatedSlides[currentSlideIndex]
                setPic(wholeStoryImageView, slide.slideNum)
                draftPlayer.setStorySource(this, slide.filename)
            }
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
     * @param view uploadButton to set listeners for
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


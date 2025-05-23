package org.sil.storyproducer.controller.voicestudio

import android.view.View
import android.widget.ImageButton
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.assignNewAudioRelPath
import org.sil.storyproducer.tools.file.getChosenFilename
import org.sil.storyproducer.tools.file.getTempAppendAudioRelPath
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.toolbar.MultiRecordRecordingToolbar
import java.io.FileNotFoundException

/**
 * A class responsible for more advanced recording functionality (allowing multiple recordings to
 * be appended together into one final recording).
 *
 * This class extends the recording, playback, and multi-recording listing functionality of its base
 * classes. A fourth button is added for finishing a set of recordings that are to be appended
 * together. A fifth button is added for sending the finished recording to a web server (not
 * currently implemented).
 */
class VoiceStudioRecordingToolbar: MultiRecordRecordingToolbar() {
    private lateinit var checkButton: ImageButton
    private lateinit var sendAudioButton: ImageButton
    
    private var enableSendAudioButton : Boolean = false

    private val audioTempName = getTempAppendAudioRelPath()

    override fun onPause() {
        isAppendingOn = false
        super.onPause()
    }

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        checkButton = toolbarButton(R.drawable.ic_stop_white_48dp, R.id.finish_recording_button, R.string.rec_toolbar_stop_button)
        rootView?.addView(checkButton)
        
        rootView?.addView(toolbarButtonSpace())

        sendAudioButton = toolbarButton(R.drawable.ic_send_audio_48dp, -1, R.string.rec_toolbar_send_button)
        if(enableSendAudioButton) {
            rootView?.addView(sendAudioButton)
            
            rootView?.addView(toolbarButtonSpace())
        }
    }

    override fun updateInheritedToolbarButtonVisibility() {
        super.updateInheritedToolbarButtonVisibility()

        if (!isAppendingOn) {
            checkButton.visibility = View.INVISIBLE
            checkButton.alpha = 0.5f
            checkButton.isEnabled = false
        }
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        checkButton.visibility = View.VISIBLE
        checkButton.alpha = 1.0f
        checkButton.isEnabled = true
        sendAudioButton.visibility = View.VISIBLE
        sendAudioButton.alpha = 1.0f
        sendAudioButton.isEnabled = true
    }

    override fun hideInheritedToolbarButtons(animated: Boolean) {
        super.hideInheritedToolbarButtons(animated)

        checkButton.visibility = View.INVISIBLE
        checkButton.alpha = 0.5f
        checkButton.isEnabled = false
//        sendAudioButton.visibility = View.INVISIBLE
        sendAudioButton.alpha = 0.5f
        sendAudioButton.isEnabled = false
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

        checkButton.setOnClickListener(checkButtonOnClickListener())
        sendAudioButton.setOnClickListener(sendButtonOnClickListener())
    }

    override fun micButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val wasRecording = voiceRecorder?.isRecording == true
            val wasAppending = isAppendingOn
            if (wasRecording)
                isAppendingOn = wasRecording

            stopToolbarMedia()

            if (wasRecording) {
                if (wasAppending) {
                    try {
                        AudioRecorder.concatenateAudioFiles(appContext, getChosenFilename(), audioTempName)
                    } catch (e: FileNotFoundException) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
                }

                micButton.setBackgroundResource(R.drawable.ic_mic_plus_48dp)
                micButton.contentDescription = getString(R.string.rec_toolbar_append_recording_button)

                if (!wasAppending) {
                    toolbarMediaListener.onStartedToolbarAppending()
                }
            } else {
                if (wasAppending) {
                    recordAudio(audioTempName)
                } else {
                    recordAudio(assignNewAudioRelPath(view?.context!!))
                }

                micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                micButton.contentDescription = getString(R.string.rec_toolbar_pause_recording_button)
                checkButton.visibility = View.VISIBLE
                checkButton.alpha = 1.0f
                checkButton.isEnabled = true
            }
        }
    }

    private fun checkButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {

            val wasAppending = isAppendingOn

            if (wasAppending && (voiceRecorder?.isRecording == true)) {
                stopToolbarMedia()
                try {
                    AudioRecorder.concatenateAudioFiles(appContext, getChosenFilename(), audioTempName)
                } catch (e: FileNotFoundException) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }else{
                stopToolbarMedia()
            }
            isAppendingOn = false

            micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
            micButton.contentDescription = getString(R.string.rec_toolbar_start_recording_button)

            checkButton.visibility = View.INVISIBLE
            checkButton.alpha = 0.5f
            checkButton.isEnabled = false
            sendAudioButton.visibility = View.VISIBLE
            sendAudioButton.alpha = 1.0f
            sendAudioButton.isEnabled = true

            if (wasAppending) {
                toolbarMediaListener.onStoppedToolbarAppending()
            }
        }
    }

    private fun sendButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
        }
    }
}

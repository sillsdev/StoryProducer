package org.sil.storyproducer.controller.dramatization

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.crashlytics.android.Crashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.assignNewAudioRelPath
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.io.FileNotFoundException

internal const val ENABLE_SEND_AUDIO_BUTTON = "EnableSendAudioButton"

class DramatizationRecordingToolbar: RecordingToolbar() {
    private lateinit var checkButton: ImageButton
    private lateinit var sendAudioButton: ImageButton
    
    private var enableSendAudioButton : Boolean = false

    private var isAppendingOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundleArguments = arguments
        if (bundleArguments != null) {
            enableSendAudioButton = bundleArguments.get(ENABLE_SEND_AUDIO_BUTTON) as Boolean
        }
    }

    override fun onPause() {
        super.onPause()

        isAppendingOn = false
    }

    override fun updateToolbarButtonVisibility() {
        super.updateToolbarButtonVisibility()

        if(!isAppendingOn){
            checkButton.visibility = View.INVISIBLE
        }
    }

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        checkButton = toolbarButton(R.drawable.ic_stop_white_48dp, R.id.finish_recording_button)
        rootView?.addView(checkButton)
        rootView?.addView(toolbarButtonSpace())

        sendAudioButton = toolbarButton(R.drawable.ic_send_audio_48dp, -1)
        if(enableSendAudioButton) {
            rootView?.addView(sendAudioButton)
            rootView?.addView(toolbarButtonSpace())
        }
    }

    override fun showSecondaryButtons() {
        super.showSecondaryButtons()

        checkButton.visibility = View.VISIBLE
        sendAudioButton.visibility = View.VISIBLE
    }

    override fun hideSecondaryButtons() {
        super.hideSecondaryButtons()

        checkButton.visibility = View.INVISIBLE
        sendAudioButton.visibility = View.INVISIBLE
    }

    override fun setToolbarOnClickListeners() {
        super.setToolbarOnClickListeners()

        checkButton.setOnClickListener(checkButtonOnClickListener())
        sendAudioButton.setOnClickListener(sendButtonOnClickListener())
    }

    override fun micButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val wasRecording = voiceRecorder?.isRecording == true
            stopToolbarMedia()
            if (wasRecording) {
                if (isAppendingOn) {
                    try {
                        AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), audioTempName)
                    } catch (e: FileNotFoundException) {
                        Crashlytics.logException(e)
                    }
                } else {
                    isAppendingOn = true
                }
                micButton.setBackgroundResource(R.drawable.ic_mic_plus_48dp)
            } else {
                if (isAppendingOn) {
                    recordAudio(audioTempName)
                } else {
                    recordAudio(assignNewAudioRelPath())
                }
                micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                checkButton.visibility = View.VISIBLE
            }
        }
    }

    private fun checkButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
            //Delete the temp file wav file
            if (isAppendingOn && (voiceRecorder?.isRecording == true)) {
                try {
                    AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), audioTempName)
                } catch (e: FileNotFoundException) {
                    Crashlytics.logException(e)
                }
            }
            isAppendingOn = false

            micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)

            checkButton.visibility = View.INVISIBLE
            sendAudioButton.visibility = View.VISIBLE
        }
    }

    private fun sendButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
        }
    }
}
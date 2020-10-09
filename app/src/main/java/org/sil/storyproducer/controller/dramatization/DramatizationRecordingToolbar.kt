package org.sil.storyproducer.controller.dramatization

import android.view.View
import android.widget.ImageButton
import org.sil.storyproducer.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
class DramatizationRecordingToolbar: MultiRecordRecordingToolbar() {
    private lateinit var checkButton: ImageButton
    private lateinit var sendAudioButton: ImageButton
    
    private var enableSendAudioButton : Boolean = false

    private var isAppendingOn = false
    private val audioTempName = getTempAppendAudioRelPath()

    override fun onPause() {
        isAppendingOn = false

        super.onPause()
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

    override fun updateInheritedToolbarButtonVisibility() {
        super.updateInheritedToolbarButtonVisibility()

        if(!isAppendingOn){
            checkButton.visibility = View.INVISIBLE
        }
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        checkButton.visibility = View.VISIBLE
        sendAudioButton.visibility = View.VISIBLE
    }

    override fun hideInheritedToolbarButtons() {
        super.hideInheritedToolbarButtons()

        checkButton.visibility = View.INVISIBLE
        sendAudioButton.visibility = View.INVISIBLE
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

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
                        AudioRecorder.concatenateAudioFiles(appContext, getChosenFilename(), audioTempName)
                    } catch (e: FileNotFoundException) {
                        FirebaseCrashlytics.getInstance().recordException(e)
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

            if (isAppendingOn && (voiceRecorder?.isRecording == true)) {
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
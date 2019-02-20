package org.sil.storyproducer.tools.toolbar

import android.app.Activity
import android.support.v4.widget.Space
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4

import java.io.FileNotFoundException


/**
 * This class is used to extend the capabilities of the RecordingToolbar [RecordingToolbar]. <br></br>
 * The purpose of this class is to add a pausing capability to the the recording toolbar. So, a
 * recording can have multiple appended recordings.
 */
class PausingRecordingToolbar
/**
 * Ctor
 *
 * @param activity                The activity from the calling class.
 * @param rootViewToolbarLayout   The rootViewToEmbedToolbarIn of the Toolbar layout called toolbar_for_recording.
 * must be of type LinearLayout so that buttons can be
 * evenly spaced.
 * @param rootViewLayout          The rootViewToEmbedToolbarIn of the layout that you want to embed the toolbar in.
 * @param enablePlaybackButton    Enable playback of recording.
 * @param enableDeleteButton      Enable the delete button, does not work as of now.
 * @param enableMultiRecordButton Enabled the play list button on the toolbar.
 * @param enableSendAudioButton   Enable sending audio to server
 * @param multiRecordModal        The modal that houses the multiple recordings.
 * @param recordingListener       The listener responsible for interactions between toolbar
 * and corresponding class. Used on stop and start of recording.
 */
@Throws(ClassCastException::class)
constructor(activity: Activity, rootViewToolbarLayout: View, rootViewLayout: RelativeLayout,
            enablePlaybackButton: Boolean, enableDeleteButton: Boolean, enableMultiRecordButton: Boolean, enableSendAudioButton: Boolean,
            multiRecordModal: Modal, recordingListener: RecordingToolbar.RecordingListener, slideNum: Int) : RecordingToolbar(activity, rootViewToolbarLayout, rootViewLayout, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton, multiRecordModal, recordingListener, slideNum) {
    private var enableCheckButton: Boolean = true
    private var isAppendingOn: Boolean = false
    private var checkButton: ImageButton? = null
    private val AUDIO_TEMP_NAME = getTempAppendAudioRelPath()


    init{
        voiceRecorder = AudioRecorderMP4(activity)
    }
    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    override fun stopToolbarMedia() {
        super.stopToolbarMedia()
        if(voiceRecorder.isRecording){
            if (enableCheckButton) {
                multiRecordButton.visibility = View.VISIBLE
            }
        }
    }

    /*
    Stop the appending session for the toolbar.
     */
    fun stopAppendingSession() {
        if (isAppendingOn) {
            //simulate a finish recording session and set isAppendingOn to false
            checkButton!!.callOnClick()
        }
    }

    /**
     * Calling class should be responsible for all other media
     * so [.stopPlayBackAndRecording] is not being used here.
     */
    override fun onPause() {
        super.onPause()
        if (voiceRecorder.isRecording) {
            //simulate a stop of recording.
            micButton.callOnClick()
        }
        //else stop other media from playing.
        stopAppendingSession()
    }



    /**
     * Used to hide any buttons that need to be hidden
     */
    override fun hideButtons() {
        super.hideButtons()
        if (enableCheckButton) {
            checkButton!!.visibility = View.INVISIBLE
        }
        if (isAppendingOn) {
            //simulate a finish recording session and set isAppendingOn to false
            checkButton!!.callOnClick()
        }
    }

    /**
     * Used to add buttons to the toolbar
     */
    override fun setupToolbarButtons() {
        //TODO merge the pausing recording toolbar and recording toolbar accepting "enableCheckButton" as an option.  Call it "enablePause".
        //This is needed because this function is called by the child constructor before this classes constructor can initialize the variable.
        enableCheckButton = true
        rootViewToolbarLayout.removeAllViews()
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        spaceLayoutParams.width = 0
        val drawables = intArrayOf(R.drawable.ic_mic_white_48dp, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_stop_white_48dp, R.drawable.ic_send_audio_48dp)
        val imageButtons = arrayOf(ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext))
        val buttonToDisplay = booleanArrayOf(true/*enable mic*/, enablePlaybackButton, enableMultiRecordButton, enableCheckButton, enableSendAudioButton)

        var buttonSpacing = Space(appContext)
        buttonSpacing.layoutParams = spaceLayoutParams
        toolbar.addView(buttonSpacing) //Add a space to the left of the first button.
        for (i in drawables.indices) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i])
                imageButtons[i].visibility = View.VISIBLE
                imageButtons[i].layoutParams = layoutParams
                toolbar.addView(imageButtons[i])

                buttonSpacing = Space(appContext)
                buttonSpacing.layoutParams = spaceLayoutParams
                toolbar.addView(buttonSpacing)
                when (i) {
                    0 -> micButton = imageButtons[i]
                    1 -> playButton = imageButtons[i]
                    2 -> multiRecordButton = imageButtons[i]
                    3 -> checkButton = imageButtons[i]
                    4 -> sendAudioButton = imageButtons[i]
                }
            }
        }
        setToolbarButtonIds()

        val playBackFileExist = storyRelPathExists(activity, Workspace.activePhase.getChosenFilename(slideNum))
        if (enablePlaybackButton) {
            playButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableCheckButton) {
            checkButton!!.visibility = if (playBackFileExist && isAppendingOn) View.VISIBLE else View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        setOnClickListeners()
    }

    override fun setToolbarButtonIds() {
        super.setToolbarButtonIds()
        checkButton?.id = org.sil.storyproducer.R.id.finish_recording_button
    }

    /**
     * Add listeners to the buttons on the toolbar. This child class does change the
     * mic button and adds the check button listeners on top of calling the parent's class
     * onClickListeners.
     */
    override fun setOnClickListeners() {
        super.setOnClickListeners()
        val micListener = View.OnClickListener {
            if (voiceRecorder.isRecording) {
                stopRecording()
                if (isAppendingOn) {
                    try {
                        AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), AUDIO_TEMP_NAME)
                    } catch (e: FileNotFoundException) {
                        Log.e(TAG, "Did not concatenate audio files", e)
                    }
                } else {
                    isAppendingOn = true
                    checkButton!!.visibility = View.VISIBLE
                }
                micButton.setBackgroundResource(R.drawable.ic_mic_plus_48dp)
                if (enableDeleteButton) {
                    deleteButton.visibility = View.VISIBLE
                }
                if (enablePlaybackButton) {
                    playButton.visibility = View.VISIBLE
                }
                if (enableMultiRecordButton) {
                    multiRecordButton.visibility = View.VISIBLE
                }
                if (enableCheckButton) {
                    checkButton!!.visibility = View.VISIBLE
                }
            } else {
                recordingListener.onStartedRecordingOrPlayback(true)
                if (isAppendingOn) {
                    startRecording(AUDIO_TEMP_NAME)
                }else{
                    startRecording(assignNewAudioRelPath())
                }
                micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                //checkButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                if (enableDeleteButton) {
                    deleteButton.visibility = View.INVISIBLE
                }
                if (enablePlaybackButton) {
                    playButton.visibility = View.INVISIBLE
                }
                if (enableMultiRecordButton) {
                    multiRecordButton.visibility = View.INVISIBLE
                }
                if (enableSendAudioButton) {
                    sendAudioButton.visibility = View.INVISIBLE
                }
                //if (enableCheckButton) {
                //  checkButton.setVisibility(View.INVISIBLE);
                //}
            }
        }
        micButton.setOnClickListener(micListener)

        if (enableCheckButton) {
            val checkListener = View.OnClickListener {
                //Delete the temp file wav file
                if (voiceRecorder.isRecording && isAppendingOn) {
                    stopRecording()
                    try {
                        AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), AUDIO_TEMP_NAME)
                    } catch (e: FileNotFoundException) {
                        Log.e(TAG, "Did not concatenate audio files", e)
                    }
                } else {
                    stopRecording()
                }
                deleteStoryFile(appContext,AUDIO_TEMP_NAME)
                recordingListener.onStoppedRecordingOrPlayback()
                //make the button invisible till after the next new recording
                isAppendingOn = false
                checkButton!!.visibility = View.INVISIBLE
                micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
                if (enableDeleteButton) {
                    deleteButton.visibility = View.VISIBLE
                }
                if (enablePlaybackButton) {
                    playButton.visibility = View.VISIBLE
                }
                if (enableMultiRecordButton) {
                    multiRecordButton.visibility = View.VISIBLE
                }
                if (enableSendAudioButton) {
                    sendAudioButton.visibility = View.VISIBLE
                }
            }

            checkButton!!.setOnClickListener(checkListener)
        }

    }

    companion object {
        private val TAG = "PauseRecordToolbar"
    }
}

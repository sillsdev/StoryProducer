package org.sil.storyproducer.tools.toolbar

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.assignNewAudioRelPath
import org.sil.storyproducer.tools.file.deleteStoryFile
import org.sil.storyproducer.tools.file.getTempAppendAudioRelPath
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4
import java.io.FileNotFoundException

private const val RECORDING_ANIMATION_DURATION = 1500

/**
 * The purpose of this class is to extend the animationToolbar while adding the recording animation
 * to the toolbar. <br></br><br></br>
 * This class utilizes an empty layout for the toolbar and floating action button found in this layout:
 * (toolbar_for_recording.xml). <br></br>
 * The toolbar is where buttons are added to.<br></br> The toolbar is then placed at the
 * bottom of the rootView that is passed in to the this class' constructor. So, the rootView
 * must be of type RelativeLayout because the code related to placing the toolbar in the
 * rootView requires the rootView to be of type RelativeLayout. See: [.setupToolbar]<br></br><br></br>
 * This class also saves the recording and allows playback <br></br> from the toolbar. see: [.createToolbar]
 * <br></br><br></br>
 */

class RecordingToolbar : Fragment(){

    var rootView: LinearLayout? = null
    // TODO Abstract out a toolbar button (ImageButton, isEnabled, visibility, icon, layout params)
    // TODO Refactor out keyterm phase stuff into a child class
    // TODO Refactor stuff for other phases into child classes for toolbar if possible/helpful
    // TODO Add a hideAllButtons function
    private var enablePlaybackButton : Boolean = false
    private var enableCheckButton : Boolean = false
    private var enableMultiRecordButton : Boolean = false
    private var enableSendAudioButton : Boolean = false
    private lateinit var recordingListener : RecordingListener
    private var slideNum : Int = 0

    private lateinit var appContext: Context
    private lateinit var micButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var checkButton: ImageButton
    private lateinit var multiRecordButton: ImageButton
    private lateinit var sendAudioButton: ImageButton

    private var transitionDrawable: TransitionDrawable? = null
    private var colorHandler: Handler? = null
    private var colorHandlerRunnable: Runnable? = null
    private var isToolbarRed = false
    
    private var isAppendingOn = false
    private val audioTempName = getTempAppendAudioRelPath()
    private var voiceRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isRecordingOrPlaying : Boolean
        get() {return voiceRecorder?.isRecording == true  || audioPlayer.isAudioPlaying}

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        recordingListener = try {
            context as RecordingListener
        }
        catch (e : ClassCastException){
            parentFragment as RecordingListener
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = activity?.applicationContext!!
        voiceRecorder = AudioRecorderMP4(activity!!)
        val bundleArguments = arguments
        if (bundleArguments != null) {
            val buttons = bundleArguments.get("buttonEnabled") as BooleanArray
            enablePlaybackButton = buttons[0]
            enableCheckButton = buttons[1]
            enableMultiRecordButton = buttons[2]
            enableSendAudioButton = buttons[3]
            slideNum = bundleArguments.get("slideNum") as Int
        }

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            stopToolbarAudioPlaying()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.toolbar_for_recording, container, false) as LinearLayout
        setupToolbarButtons()
        setupRecordingAnimationHandler()
        stopToolbarMedia()
        return rootView
    }

    interface RecordingListener {
        fun onStoppedRecordingOrPlayback(isRecording: Boolean)
        fun onStartedRecordingOrPlayback(isRecording: Boolean)
    }

    /**
     * This function can be called so that the toolbar is automatically opened, without animation,
     * when the fragment is drawn.
     */
    fun keepToolbarVisible() {
        rootView?.visibility = View.VISIBLE
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    fun stopToolbarMedia() {
        if (voiceRecorder?.isRecording == true) {
            stopToolbarVoiceRecording()
        }
        if (audioPlayer.isAudioPlaying) {
            stopToolbarAudioPlaying()
        }
    }

    private fun stopToolbarVoiceRecording(){
        stopRecording()
        micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
        if (enableCheckButton) {
            checkButton.visibility = View.VISIBLE
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

    private fun stopToolbarAudioPlaying()   {
        playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
        audioPlayer.stopAudio()
        recordingListener.onStoppedRecordingOrPlayback(false)
    }

    /**
     * Calling class should be responsible for all other media
     * so [.stopPlayBackAndRecording] is not being used here.
     */
    override fun onPause() {
        stopToolbarMedia()
        audioPlayer.release()
        isAppendingOn = false
        super.onPause()
    }

    private fun startRecording(recordingRelPath: String) {
        //TODO: make this logging more robust and encapsulated
        recordingListener.onStartedRecordingOrPlayback(true)
        voiceRecorder?.startNewRecording(recordingRelPath)
        startRecordingAnimation(false, 0)
    }

    private fun stopRecording() {
        voiceRecorder?.stop()
        stopRecordingAnimation()
        recordingListener.onStoppedRecordingOrPlayback(true)
    }

    /**
     * This function formats and aligns the buttons to the toolbar.
     */
    fun setupToolbarButtons() {
        rootView?.removeAllViews()
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        spaceLayoutParams.width = 0
        val drawables = intArrayOf(R.drawable.ic_mic_white_48dp, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_stop_white_48dp, R.drawable.ic_send_audio_48dp)
        val imageButtons = arrayOf(ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext))
        val buttonToDisplay = booleanArrayOf(true/*enable mic*/, enablePlaybackButton, enableMultiRecordButton, enableCheckButton, enableSendAudioButton)

        var buttonSpacing = android.widget.Space(appContext)
        buttonSpacing.layoutParams = spaceLayoutParams
        rootView?.addView(buttonSpacing) //Add a space to the left of the first button.
        for (i in drawables.indices) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i])
                imageButtons[i].visibility = View.VISIBLE
                imageButtons[i].layoutParams = layoutParams
                rootView?.addView(imageButtons[i])

                buttonSpacing = android.widget.Space(appContext)
                buttonSpacing.layoutParams = spaceLayoutParams
                rootView?.addView(buttonSpacing)
                when (i) {
                    0 -> {
                        micButton = imageButtons[i]
                        micButton.id = org.sil.storyproducer.R.id.start_recording_button
                    }
                    1 -> {
                        playButton = imageButtons[i]
                        playButton.id = org.sil.storyproducer.R.id.play_recording_button
                    }
                    2 -> {
                        multiRecordButton = imageButtons[i]
                        multiRecordButton.id = org.sil.storyproducer.R.id.list_recordings_button
                    }
                    3 -> {
                        checkButton = imageButtons[i]
                        checkButton.id = org.sil.storyproducer.R.id.finish_recording_button
                    }
                    4 -> {
                        sendAudioButton = imageButtons[i]
                    }
                }
            }
        }

        val playBackFileExist = storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum))
        if (enablePlaybackButton) {
            playButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableCheckButton) {
            checkButton.visibility = if (playBackFileExist && isAppendingOn) View.VISIBLE else View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        setOnClickListeners()
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    private fun setOnClickListeners() {
        micButton.setOnClickListener(getMicButtonOnClickListener())
        if (enablePlaybackButton) {
            playButton.setOnClickListener(getPlayButtonOnClickListener())
        }
        if (enableCheckButton) {
            checkButton.setOnClickListener(getCheckButtonOnClickListener())
        }
        if (enableMultiRecordButton) {
            multiRecordButton.setOnClickListener(getMultiRecordButtonOnClickListener())
        }
        if (enableSendAudioButton) {
            sendAudioButton.setOnClickListener(getSendButtonOnClickListener())
        }
    }

    private fun getMicButtonOnClickListener(): View.OnClickListener{
          return View.OnClickListener {
              val wasRecording = voiceRecorder?.isRecording == true
              stopToolbarMedia()
              if(enableCheckButton){
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
                          startRecording(audioTempName)
                      }
                      else {
                          startRecording(assignNewAudioRelPath())
                      }
                      micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                      if (enablePlaybackButton) {
                          playButton.visibility = View.INVISIBLE
                      }
                      if (enableMultiRecordButton) {
                          multiRecordButton.visibility = View.INVISIBLE
                      }
                      if (enableSendAudioButton) {
                          sendAudioButton.visibility = View.INVISIBLE
                      }
                  }
              }
              else {
                  if (!wasRecording) {
                      val recordingRelPath = assignNewAudioRelPath()
                      //we may be overwriting things in other phases, but we do not care.
                      if (storyRelPathExists(activity!!, recordingRelPath) && Workspace.activePhase.phaseType == PhaseType.LEARN) {
                          val dialog = AlertDialog.Builder(activity!!)
                                  .setTitle(activity!!.getString(R.string.overwrite))
                                  .setMessage(activity!!.getString(R.string.learn_phase_overwrite))
                                  .setNegativeButton(activity!!.getString(R.string.no)) { _, _ ->
                                      //do nothing
                                  }
                                  .setPositiveButton(activity!!.getString(R.string.yes)) { _, _ ->
                                      //overwrite audio
                                      recordAudio(recordingRelPath)
                                  }.create()
                          dialog.show()
                      } else {
                          recordAudio(recordingRelPath)
                      }
                  }
              }
          }
    }

    private fun getPlayButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            stopToolbarMedia()
            if (!audioPlayer.isAudioPlaying) {
                recordingListener.onStartedRecordingOrPlayback(false)
                if (audioPlayer.setStorySource(this.appContext,Workspace.activePhase.getChosenFilename())) {
                    audioPlayer.playAudio()
                    Toast.makeText(appContext, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show()
                    playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
                    //TODO: make this logging more robust and encapsulated
                    when (Workspace.activePhase.phaseType){
                        PhaseType.DRAFT -> saveLog(appContext.getString(R.string.DRAFT_PLAYBACK))
                        PhaseType.COMMUNITY_CHECK-> saveLog(appContext.getString(R.string.COMMENT_PLAYBACK))
                        else ->{}
                    }
                } else {
                    Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCheckButtonOnClickListener(): View.OnClickListener{
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

            //make the button invisible till after the next new recording
            isAppendingOn = false
            checkButton.visibility = View.INVISIBLE
            micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
            if (enableSendAudioButton) {
                sendAudioButton.visibility = View.VISIBLE
            }
        }
    }

    private fun getMultiRecordButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
            recordingListener.onStartedRecordingOrPlayback(false)
            RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
        }
    }

    private fun getSendButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
        }
    }

    /*
    * Start recording audio and hide buttons
     */
    private fun recordAudio(recordingRelPath: String) {
        startRecording(recordingRelPath)
        when(Workspace.activePhase.phaseType){
            PhaseType.DRAFT -> saveLog(activity?.getString(R.string.DRAFT_RECORDING)!!)
            PhaseType.COMMUNITY_CHECK -> saveLog(activity?.getString(R.string.COMMENT_RECORDING)!!)
            else -> {}
        }
        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
        if (enableCheckButton) {
            checkButton.visibility = View.INVISIBLE
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
    }

    /**
     *
     */
    private fun setupRecordingAnimationHandler() {
        val red = Color.rgb(255, 0, 0)
        var colorOfToolbar = Color.rgb(67, 179, 230)

        val relBackgroundColor = rootView?.background
        if (relBackgroundColor is ColorDrawable) {
            colorOfToolbar = relBackgroundColor.color
        }
        transitionDrawable = TransitionDrawable(arrayOf(ColorDrawable(colorOfToolbar), ColorDrawable(red)))
        rootView?.background = transitionDrawable

        colorHandler = Handler()
        colorHandlerRunnable = Runnable {
            //Animation to change the toolbar's color while recording
            isToolbarRed = if (isToolbarRed) {
                transitionDrawable!!.reverseTransition(RECORDING_ANIMATION_DURATION)
                false
            } else {
                transitionDrawable!!.startTransition(RECORDING_ANIMATION_DURATION)
                true
            }
            startRecordingAnimation(true, RECORDING_ANIMATION_DURATION)
        }
    }

    /**
     * This function is used to start the handler to run the runnable. <br></br>
     * [.setupRecordingAnimationHandler] should be called first before calling this function
     * to initialize the colorHandler and colorHandlerRunnable().
     *
     * @param isDelayed Used to signify that the runnable will be delayed in running.
     * @param delay     The time that will be delayed in ms if isDelayed is true.
     */
    private fun startRecordingAnimation(isDelayed: Boolean, delay: Int) {
        if (isRecordingAnimationEnabled())
        {
            if (colorHandler != null && colorHandlerRunnable != null) {
                if (isDelayed) {
                    colorHandler!!.postDelayed(colorHandlerRunnable, delay.toLong())
                } else {
                    colorHandler!!.post(colorHandlerRunnable)
                }
            }
        }
    }

    private fun isRecordingAnimationEnabled(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(activity?.resources?.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), false)
    }

    /**
     * Stops the animation from continuing. The removeCallbacks function removes all
     * colorHandlerRunnable from the MessageQueue and also resets the toolbar to its original color.
     * (transitionDrawable.resetTransition();)
     */
    private fun stopRecordingAnimation() {
        if (colorHandler != null && colorHandlerRunnable != null) {
            colorHandler!!.removeCallbacks(colorHandlerRunnable)
        }
        if (transitionDrawable != null) {
            transitionDrawable!!.resetTransition()
        }
    }
}

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
import android.widget.Space
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
// TODO Refactor out keyterm phase stuff into a child class
// TODO Refactor stuff for other phases into child classes for toolbar if possible/helpful
// TODO Refactor animation stuff into its own class
class RecordingToolbar : Fragment(){
    var rootView: LinearLayout? = null
    private lateinit var appContext: Context
    private lateinit var micButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var checkButton: ImageButton
    private lateinit var multiRecordButton: ImageButton
    private lateinit var sendAudioButton: ImageButton

    private var enablePlaybackButton : Boolean = false
    private var enableCheckButton : Boolean = false
    private var enableMultiRecordButton : Boolean = false
    private var enableSendAudioButton : Boolean = false

    private lateinit var recordingListener : RecordingListener
    private var voiceRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isRecordingOrPlaying : Boolean
        get() {return voiceRecorder?.isRecording == true  || audioPlayer.isAudioPlaying}
    private var isAppendingOn = false
    private val audioTempName = getTempAppendAudioRelPath()
    private var slideNum : Int = 0

    private var transitionDrawable: TransitionDrawable? = null
    private var colorHandler: Handler? = null
    private var colorHandlerRunnable: Runnable? = null
    private var isToolbarRed = false

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.toolbar_for_recording, container, false) as LinearLayout
        setupToolbarButtons()
        setupRecordingAnimationHandler()
        stopToolbarMedia()
        return rootView
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
        showSecondaryButtons()
    }

    private fun stopToolbarAudioPlaying()   {
        audioPlayer.stopAudio()
        playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
        recordingListener.onStoppedRecordingOrPlayback(false)
    }

    private fun stopRecording() {
        voiceRecorder?.stop()
        stopRecordingAnimation()
        recordingListener.onStoppedRecordingOrPlayback(true)
    }

    /**
     * This function sets button visibility based on existence of a playback file.
     */
    fun updateToolbarButtonVisibility(){
        val playBackFileExist = storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum))
        if(playBackFileExist){
            showSecondaryButtons()
        }
        else{
            hideSecondaryButtons()
        }
        if(!isAppendingOn){
            checkButton.visibility = View.INVISIBLE
        }
    }

    /**
     * This function formats and aligns the buttons to the toolbar.
     */
    private fun setupToolbarButtons() {
        rootView?.removeAllViews()

        rootView?.addView(toolbarButtonSpace())

        micButton = toolbarButton(R.drawable.ic_mic_white_48dp, org.sil.storyproducer.R.id.start_recording_button)
        rootView?.addView(micButton)
        rootView?.addView(toolbarButtonSpace())

        playButton = toolbarButton(R.drawable.ic_play_arrow_white_48dp, org.sil.storyproducer.R.id.play_recording_button)
        if(enablePlaybackButton) {
            rootView?.addView(playButton)
            rootView?.addView(toolbarButtonSpace())
        }

        multiRecordButton = toolbarButton(R.drawable.ic_playlist_play_white_48dp, org.sil.storyproducer.R.id.list_recordings_button)
        if(enableMultiRecordButton) {
            rootView?.addView(multiRecordButton)
            rootView?.addView(toolbarButtonSpace())
        }

        checkButton = toolbarButton(R.drawable.ic_stop_white_48dp, org.sil.storyproducer.R.id.finish_recording_button)
        if(enableCheckButton) {
            rootView?.addView(checkButton)
            rootView?.addView(toolbarButtonSpace())
        }

        sendAudioButton = toolbarButton(R.drawable.ic_send_audio_48dp, -1)
        if(enableSendAudioButton) {
            rootView?.addView(sendAudioButton)
            rootView?.addView(toolbarButtonSpace())
        }

        updateToolbarButtonVisibility()

        setOnClickListeners()
    }

    private fun toolbarButton(iconId: Int, buttonId: Int): ImageButton{
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val toolbarButton = ImageButton(appContext)
        toolbarButton.setBackgroundResource(iconId)
        toolbarButton.layoutParams = layoutParams
        toolbarButton.id = buttonId
        return toolbarButton
    }

    private fun toolbarButtonSpace(): Space{
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        val buttonSpace = Space(appContext)
        buttonSpace.layoutParams = spaceLayoutParams
        return buttonSpace
    }

    private fun showSecondaryButtons(){
        checkButton.visibility = View.VISIBLE
        playButton.visibility = View.VISIBLE
        multiRecordButton.visibility = View.VISIBLE
        sendAudioButton.visibility = View.VISIBLE
    }

    private fun hideSecondaryButtons(){
        checkButton.visibility = View.INVISIBLE
        playButton.visibility = View.INVISIBLE
        multiRecordButton.visibility = View.INVISIBLE
        sendAudioButton.visibility = View.INVISIBLE
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    private fun setOnClickListeners() {
        micButton.setOnClickListener(micButtonOnClickListener())
        playButton.setOnClickListener(playButtonOnClickListener())
        checkButton.setOnClickListener(checkButtonOnClickListener())
        multiRecordButton.setOnClickListener(multiRecordButtonOnClickListener())
        sendAudioButton.setOnClickListener(sendButtonOnClickListener())
    }

    private fun micButtonOnClickListener(): View.OnClickListener{
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
                          recordAudio(audioTempName)
                      }
                      else {
                          recordAudio(assignNewAudioRelPath())
                      }
                      micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                      checkButton.visibility = View.VISIBLE
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

    private fun playButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            stopToolbarMedia()
            if (!audioPlayer.isAudioPlaying) {
                recordingListener.onStartedRecordingOrPlayback(false)
                if (audioPlayer.setStorySource(this.appContext, Workspace.activePhase.getChosenFilename())) {
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

    private fun multiRecordButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
            recordingListener.onStartedRecordingOrPlayback(false)
            RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
        }
    }

    private fun sendButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()
        }
    }

    /*
    * Start recording audio and hide buttons
     */
    private fun recordAudio(recordingRelPath: String) {
        recordingListener.onStartedRecordingOrPlayback(true)
        voiceRecorder?.startNewRecording(recordingRelPath)
        startRecordingAnimation(false, 0)

        //TODO: make this logging more robust and encapsulated
        when(Workspace.activePhase.phaseType){
            PhaseType.DRAFT -> saveLog(activity?.getString(R.string.DRAFT_RECORDING)!!)
            PhaseType.COMMUNITY_CHECK -> saveLog(activity?.getString(R.string.COMMENT_RECORDING)!!)
            else -> {}
        }

        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)

        hideSecondaryButtons()
    }

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
        if (isRecordingAnimationEnabled()) {
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

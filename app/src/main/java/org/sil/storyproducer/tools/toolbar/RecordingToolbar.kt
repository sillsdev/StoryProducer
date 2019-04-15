package org.sil.storyproducer.tools.toolbar

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.assignNewAudioRelPath
import org.sil.storyproducer.tools.file.getTempAppendAudioRelPath
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4

internal const val ENABLE_PLAY_BACK_BUTTON = "EnablePlayBackButton"

/**
 * The purpose of this class is to extend the animationToolbar while adding the recording animation
 * to the toolbar.  This class utilizes an empty layout for the toolbar and floating action button
 * found in this layout: (toolbar_for_recording.xml).
 *
 * The toolbar will be added to a frame layout that will typically be placed at the bottom of a
 * layout.  See fragment_slide.xml for an example.  Arguments will be passed to the fragment
 * containing a boolean array of what buttons are used for the phase and the current slide number.
 *
 * @sample org.sil.storyproducer.controller.MultiRecordFrag.setToolbar
 *
 * This class also saves the recording and allows playback from the toolbar. see: [.createToolbar]
 */
open class RecordingToolbar : Fragment(){
    var rootView: LinearLayout? = null
    protected lateinit var appContext: Context
    protected lateinit var micButton: ImageButton
    private lateinit var playButton: ImageButton

    private var enablePlaybackButton : Boolean = false

    protected lateinit var recordingListener : RecordingListener
    protected var voiceRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isRecordingOrPlaying : Boolean
        get() {return voiceRecorder?.isRecording == true  || audioPlayer.isAudioPlaying}
    protected val audioTempName = getTempAppendAudioRelPath()
    private var slideNum : Int = 0

    private lateinit  var animationHandler: AnimationHandler

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
            enablePlaybackButton = bundleArguments.get(ENABLE_PLAY_BACK_BUTTON) as Boolean
            slideNum = bundleArguments.get(SLIDE_NUM) as Int
        }

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            stopToolbarAudioPlaying()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.toolbar_for_recording, container, false) as LinearLayout

        val initialColor: Int = Color.rgb(67, 179, 230)
        val targetColor: Int = Color.rgb(255, 0, 0)
        animationHandler = AnimationHandler(initialColor, targetColor)
        rootView?.background = animationHandler.transitionDrawable

        setupToolbarButtons()
        updateToolbarButtonVisibility()
        setToolbarOnClickListeners()

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
        animationHandler.stopAnimation()
        recordingListener.onStoppedRecordingOrPlayback(true)
    }

    /**
     * This function sets button visibility based on existence of a playback file.
     */
     open fun updateToolbarButtonVisibility(){
        val playBackFileExist = storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum))
        if(playBackFileExist){
            showSecondaryButtons()
        }
        else{
            hideSecondaryButtons()
        }
    }

    /**
     * This function formats and aligns the buttons to the toolbar.
     */
    protected open fun setupToolbarButtons() {
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
    }

    protected fun toolbarButton(iconId: Int, buttonId: Int): ImageButton{
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val toolbarButton = ImageButton(appContext)
        toolbarButton.setBackgroundResource(iconId)
        toolbarButton.layoutParams = layoutParams
        toolbarButton.id = buttonId
        return toolbarButton
    }

    protected fun toolbarButtonSpace(): Space{
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        val buttonSpace = Space(appContext)
        buttonSpace.layoutParams = spaceLayoutParams
        return buttonSpace
    }

    protected open fun showSecondaryButtons(){
        playButton.visibility = View.VISIBLE
    }

    protected open fun hideSecondaryButtons(){
        playButton.visibility = View.INVISIBLE
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    protected open fun setToolbarOnClickListeners() {
        micButton.setOnClickListener(micButtonOnClickListener())
        playButton.setOnClickListener(playButtonOnClickListener())
    }

    protected open fun micButtonOnClickListener(): View.OnClickListener{
          return View.OnClickListener {
              val wasRecording = voiceRecorder?.isRecording == true
              stopToolbarMedia()
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

    protected fun recordAudio(recordingRelPath: String) {
        recordingListener.onStartedRecordingOrPlayback(true)
        voiceRecorder?.startNewRecording(recordingRelPath)
        if(isAnimationEnabled()){
            animationHandler.startAnimation()
        }
        
        //TODO: make this logging more robust and encapsulated
        when(Workspace.activePhase.phaseType){
            PhaseType.DRAFT -> saveLog(activity?.getString(R.string.DRAFT_RECORDING)!!)
            PhaseType.COMMUNITY_CHECK -> saveLog(activity?.getString(R.string.COMMENT_RECORDING)!!)
            else -> {}
        }
        
        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)

        hideSecondaryButtons()
    }

    private fun isAnimationEnabled(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(activity?.resources?.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), false)
    }
}

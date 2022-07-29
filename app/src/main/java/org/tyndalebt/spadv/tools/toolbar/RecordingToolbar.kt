package org.tyndalebt.spadv.tools.toolbar

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.preference.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Space
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.PhaseType
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.model.logging.saveLog
import org.tyndalebt.spadv.tools.file.assignNewAudioRelPath
import org.tyndalebt.spadv.tools.file.storyRelPathExists
import org.tyndalebt.spadv.tools.media.AudioRecorder
import org.tyndalebt.spadv.tools.media.AudioRecorderMP4

/**
 * A class responsible for controlling the media and appearance of a recording toolbar.
 *
 * This is a base class for any toolbar that can record voice audio. The UI includes one recording
 * button. Other classes may extend this class and add additional buttons with other media sources
 * and controls (like audio playback) to the toolbar. This class controls media/button callbacks,
 * toolbar animation, button visibility/appearance.
 *
 * Many of the functions are designed to be extensible and generic.
 * The principle of each function doing one thing and one thing only makes extending or overriding
 * functions much simpler.
 *
 * Additionally, it was helpful to limit coupling between this class and other classes that use it.
 * A version of an observer/subscriber design pattern is currently used for communication between
 * this class and other classes that use it so as to limit coupling. This involves the
 * ToolbarMediaListener interface that other classes can extend.
 */
open class RecordingToolbar : Fragment(){
    var rootView: LinearLayout? = null
    protected lateinit var appContext: Context
    protected lateinit var micButton: ImageButton

    open lateinit var toolbarMediaListener : ToolbarMediaListener
    protected var voiceRecorder: AudioRecorder? = null
    val isRecording : Boolean
        get() {return voiceRecorder?.isRecording == true}

    private lateinit  var animationHandler: AnimationHandler

    override fun onAttach(context: Context) {
        super.onAttach(context)

        toolbarMediaListener = try {
            context as ToolbarMediaListener
        }
        catch (e : ClassCastException){
            parentFragment as ToolbarMediaListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = activity?.applicationContext!!
        
        voiceRecorder = AudioRecorderMP4(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.toolbar_for_recording, container, false) as LinearLayout

        val initialColor: Int = Color.rgb(67, 179, 230)
        val targetColor: Int = Color.rgb(255, 0, 0)
        animationHandler = AnimationHandler(initialColor, targetColor)
        rootView?.background = animationHandler.transitionDrawable

        setupToolbarButtons()
        updateInheritedToolbarButtonVisibility()
        setToolbarButtonOnClickListeners()

        stopToolbarMedia()

        return rootView
    }

    override fun onPause() {
        stopToolbarMedia()
        
        super.onPause()
    }

    interface ToolbarMediaListener {
        fun onStoppedToolbarMedia(){}
        fun onStartedToolbarMedia(){}
        fun onStoppedToolbarRecording(){
            onStoppedToolbarMedia()
        }
        fun onStartedToolbarRecording(){
            onStartedToolbarMedia()
        }
    }

    /**
     * This function is used to stop all the media sources on the toolbar.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    open fun stopToolbarMedia() {
        if (voiceRecorder?.isRecording == true) {
            stopToolbarVoiceRecording()
        }
    }

    private fun stopToolbarVoiceRecording(){
        voiceRecorder?.stop()
        
        animationHandler.stopAnimation()

        micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
        showInheritedToolbarButtons()
        
        toolbarMediaListener.onStoppedToolbarRecording()
    }

    protected fun recordAudio(recordingRelPath: String) {
        toolbarMediaListener.onStartedToolbarRecording()

        voiceRecorder?.startNewRecording(recordingRelPath)

        if(isAnimationEnabled()){
            animationHandler.startAnimation()
        }

        //TODO: make this logging more robust and encapsulated
        when(Workspace.activePhase.phaseType){
            PhaseType.TRANSLATE_REVISE -> saveLog(activity?.getString(R.string.DRAFT_RECORDING)!!)
            PhaseType.COMMUNITY_WORK -> saveLog(activity?.getString(R.string.COMMENT_RECORDING)!!)
            else -> {}
        }

        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
        hideInheritedToolbarButtons()
    }

    /**
     * This function can be called so that the toolbar is automatically opened, without animation,
     * when the fragment is drawn.
     */
    fun keepToolbarVisible() {
        rootView?.visibility = View.VISIBLE
    }

    /**
     * This function formats and aligns the buttons to the toolbar. Buttons are evenly spaced in the
     * toolbar.
     */
    protected open fun setupToolbarButtons() {
        rootView?.removeAllViews()

        rootView?.addView(toolbarButtonSpace())

        micButton = toolbarButton(R.drawable.ic_mic_white_48dp, R.id.start_recording_button)
        rootView?.addView(micButton)
        
        rootView?.addView(toolbarButtonSpace())
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

    /**
     * This function sets the visibility of any inherited buttons
     */
    open fun updateInheritedToolbarButtonVisibility(){}

    protected open fun showInheritedToolbarButtons(){}

    protected open fun hideInheritedToolbarButtons(){}

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    protected open fun setToolbarButtonOnClickListeners() {
        micButton.setOnClickListener(micButtonOnClickListener())
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

    /*
     * Allows for disabling animations specifically when running tests, because the animations make
     * UI testing more difficult.
     */
    private fun isAnimationEnabled(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(activity?.resources?.getString(org.tyndalebt.spadv.R.string.recording_toolbar_disable_animation), false)
    }
}
package org.sil.storyproducer.tools.toolbar

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.preference.*
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.WORD_LINKS_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4
import java.io.File

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
open class RecordingToolbar : Fragment() {

    companion object {
        const val REQUEST_CODE_AUDIO_EDIT = 151
    }

    var rootView: LinearLayout? = null
    protected lateinit var appContext: Context
    protected lateinit var micButton: ImageButton

    open lateinit var toolbarMediaListener : ToolbarMediaListener
    protected var voiceRecorder: AudioRecorder? = null
    val isRecording : Boolean
        get() {return voiceRecorder?.isRecording == true}

    private lateinit  var animationHandler: AnimationHandler

    private var editActivityFile : File? = null // temp audio file being edited by external app
    private var editActivityUri : Uri? = null   // Uri for sending to audio editing app

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
        micButton.contentDescription = getString(R.string.rec_toolbar_start_recording_button)
        showInheritedToolbarButtons()

        toolbarMediaListener.onStoppedToolbarRecording()
    }

    protected fun recordAudio(recordingRelPath: String) {
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
        micButton.contentDescription = getString(R.string.rec_toolbar_stop_button)
        hideInheritedToolbarButtons(true)

        toolbarMediaListener.onStartedToolbarRecording()


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

        micButton = toolbarButton(R.drawable.ic_mic_white_48dp, R.id.start_recording_button, R.string.rec_toolbar_start_recording_button)
        rootView?.addView(micButton)

        rootView?.addView(toolbarButtonSpace())
    }

    // returns true if a compatible audio editor is installed
    protected fun canUseExternalAudioEditor() : Boolean
    {
        var mimeType = "audio/m4a"  // the recording format for SP
        var internalEditFileUri = Uri.EMPTY

        // create an intent to detect an installed app that can handle editing AAC audio files
        val editIntent = Intent(Intent.ACTION_EDIT, internalEditFileUri)
        editIntent.setDataAndType(internalEditFileUri, mimeType)
        val packageManager = activity!!.packageManager
        // return true if any installed app can edit AAC files
        return editIntent.resolveActivity(packageManager) != null
    }

    protected fun toolbarButton(iconId: Int, buttonId: Int, resId: Int): ImageButton{
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val toolbarButtonRet = ImageButton(appContext)
        toolbarButtonRet.setBackgroundResource(iconId)
        toolbarButtonRet.layoutParams = layoutParams
        toolbarButtonRet.id = buttonId
        toolbarButtonRet.contentDescription = getString(resId)
        return toolbarButtonRet
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

    protected open fun hideInheritedToolbarButtons(animated: Boolean){}

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

    // gets the audio file Uri for the given story name in this phase of work
    fun getStorySource(relPath: String,
                       storyName: String = Workspace.activeStory.title) : Uri? {

        if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS) {
            return getStoryUri(relPath, WORD_LINKS_DIR)
        } else {
            return getStoryUri(relPath,storyName)
        }
    }

    protected open fun editButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {

            stopToolbarMedia()  // stop any current playback

            var chosenFileSubPath = getChosenFilename() // sub path of current translation audio file
            var chosenFileName = chosenFileSubPath.substringAfterLast('/')  // translation file name
            var chosenFileUri = getStorySource(chosenFileSubPath)  // Path under Workspace

            do {

                if (chosenFileUri == null ||
                    chosenFileSubPath.isEmpty() ||
                    chosenFileName.isEmpty()) {
                    Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
                    break
                }

                val context = this.appContext
                var mimeType = ""
                if (chosenFileSubPath.substringAfterLast('.').equals("mp3", true))
                    mimeType = "audio/mpeg"
                else if (chosenFileSubPath.substringAfterLast('.').equals("m4a", true))
                    mimeType = "audio/m4a"

                if (mimeType.isEmpty())
                    break   // unknown file type to try and edit - so exit

                // get an audio file path that we can share with the editing app
                // we cannot always pass a path to external public storage as
                // the editing app may not have access to external public files
                val internalDocsFolder =
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val internalEditFile = File(internalDocsFolder?.path + "/${chosenFileName}")

                // copy the file to 'internal' SP app storage area and grant write access to this file below
                copyToFilesDir(context, chosenFileUri, internalEditFile)

                // get file provider Uri for this internal edit file
                val internalEditFileUri = FileProvider.getUriForFile(
                    it.context, it.context.getApplicationContext()
                        .getPackageName().toString() + ".fileprovider", internalEditFile
                )

                // create an intent for sharable edit file and set data, type and flags
                val editIntent = Intent(Intent.ACTION_EDIT, internalEditFileUri)
                editIntent.setDataAndType(internalEditFileUri, mimeType)
                editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                // grant access to all potential intent activities
                val resInfoList = it.context.packageManager.queryIntentActivities(
                                        editIntent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    it.context.grantUriPermission(packageName, internalEditFileUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                // try and launch editor for our audio file
                try {
                    startActivityForResult(editIntent, REQUEST_CODE_AUDIO_EDIT)
                    // remember file and Uri for processing the resultant edits in onActivityResult()
                    editActivityFile = internalEditFile
                    editActivityUri = chosenFileUri

                } catch (e: ActivityNotFoundException) {
                    // Define what your app should do if no activity can handle the intent.
                    // TODO: Prompt user with a suggestion for a suitable audio editor here
                    Log.e("editAudio", "intent error: ActivityNotFoundException")
                }

            } while (false)
        }
    }

    /*
     * Allows for disabling animations specifically when running tests, because the animations make
     * UI testing more difficult.
     */
    private fun isAnimationEnabled(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(activity?.resources?.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), false)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        // check to see if this result is for the action edit we launched
        if (request == REQUEST_CODE_AUDIO_EDIT) {
            if (result == AppCompatActivity.RESULT_OK) {
                if (editActivityFile != null && editActivityUri != null) {
                    // copy the edited and saved over audio file back to original location
                    // TODO: Maybe we should create a new translation file to store it
                    copyFromFilesDir(this.appContext, editActivityFile!!, editActivityUri!!)
                }
            }
            // tell to user if the edit went ahead ok or not
            val editResultMsg = if (result == AppCompatActivity.RESULT_OK)
                activity!!.getString(R.string.translation_updated)
            else
                activity!!.getString(R.string.translation_edit_cancelled)
            Toast.makeText(context, editResultMsg, Toast.LENGTH_LONG).show()

            editActivityFile?.delete()  // always delete action edit temp audio transfer file
            editActivityFile = null     // File and Uri now no longer usable
            editActivityUri = null
        }
    }
}

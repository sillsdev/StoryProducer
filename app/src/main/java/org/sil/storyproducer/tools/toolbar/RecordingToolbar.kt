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
import android.support.design.widget.BottomSheetBehavior.*
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.controller.keyterm.KeyTermActivity
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.assignNewAudioRelPath
import org.sil.storyproducer.tools.file.getTempAppendAudioRelPath
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.hideKeyboard
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private const val RECORDING_ANIMATION_DURATION = 1500

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

class RecordingToolbar : Fragment(){

    var rootView: LinearLayout? = null
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
        val mArguments = arguments
        if (mArguments != null) {
            val buttons = mArguments.get("buttonEnabled") as BooleanArray
            enablePlaybackButton = buttons[0]
            enableCheckButton = buttons[1]
            enableMultiRecordButton = buttons[2]
            enableSendAudioButton = buttons[3]
            slideNum = mArguments.get("slideNum") as Int
        }

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
            audioPlayer.stopAudio()
            recordingListener.onStoppedRecordingOrPlayback(false)
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
        //hideFloatingActionButton();
        rootView?.visibility = View.VISIBLE
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    fun stopToolbarMedia() {
        if (voiceRecorder?.isRecording == true) {
            if(enableCheckButton){
                multiRecordButton.visibility = View.VISIBLE
            }
            stopRecording()
            micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
            //set playback button visible
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
        if (audioPlayer.isAudioPlaying) {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
            audioPlayer.stopAudio()
            recordingListener.onStoppedRecordingOrPlayback(false)
        }
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

        val playBackFileExist : Boolean = if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
            storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum), "keyterms")
        }
        else {
            storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum))
        }
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
        micButton.setOnClickListener {
            if(enableCheckButton){
                if (voiceRecorder?.isRecording == true) {
                    stopRecording()
                    if (isAppendingOn) {
                        try {
                            AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), audioTempName)
                        } catch (e: FileNotFoundException) {
                            Log.e("PauseRecordToolbar", "Did not concatenate audio files", e)
                        }
                    } else {
                        isAppendingOn = true
                        checkButton.visibility = View.VISIBLE
                    }
                    micButton.setBackgroundResource(R.drawable.ic_mic_plus_48dp)
                    if (enablePlaybackButton) {
                        playButton.visibility = View.VISIBLE
                    }
                    if (enableMultiRecordButton) {
                        multiRecordButton.visibility = View.VISIBLE
                    }
                    if (enableCheckButton) {
                        checkButton.visibility = View.VISIBLE
                    }
                } else {
                    stopToolbarMedia()
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
                if (voiceRecorder?.isRecording == true) {
                    stopToolbarMedia()
                } else {
                    //Now we need to start recording!
                    val recordingRelPath = assignNewAudioRelPath()
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
                    if (storyRelPathExists(activity!!, recordingRelPath) &&
                            //we may be overwriting things in other phases, but we do not care.
                            Workspace.activePhase.phaseType == PhaseType.LEARN) {
                        dialog.show()
                    } else {
                        recordAudio(recordingRelPath)
                    }
                }
            }
        }

        if (enablePlaybackButton) {
            val playListener = View.OnClickListener {
                if (audioPlayer.isAudioPlaying) {
                    audioPlayer.stopAudio()
                    playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
                    recordingListener.onStoppedRecordingOrPlayback(false)
                } else {
                    stopToolbarMedia()
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
            playButton.setOnClickListener(playListener)
        }
        if (enableCheckButton) {
            checkButton.setOnClickListener {
                //Delete the temp file wav file
                if (isAppendingOn && (voiceRecorder?.isRecording == true)) {
                    stopToolbarMedia()
                    try {
                        AudioRecorder.concatenateAudioFiles(appContext, Workspace.activePhase.getChosenFilename(), audioTempName)
                    } catch (e: FileNotFoundException) {
                        Log.e("PauseRecordToolbar", "Did not concatenate audio files", e)
                    }
                }else {
                    stopToolbarMedia()
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
        if (enableMultiRecordButton) {
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM && activity is KeyTermActivity){
                val bottomSheet = (activity as KeyTermActivity).bottomSheet
                from(bottomSheet).setBottomSheetCallback(object : BottomSheetCallback(){
                    override fun onStateChanged(view: View, newState: Int) {
                        setKeytermMultiRecordIcon(newState)
                        if(newState == STATE_COLLAPSED){
                            view.let { activity?.hideKeyboard(it) }
                        }
                        // Disables opening recording list when no recordings are available
                        if(Workspace.activeKeyterm.keytermRecordings.isEmpty()){
                            from(bottomSheet).state = STATE_COLLAPSED
                        }
                    }
                    override fun onSlide(view: View, newState: Float) {}
                })
                setKeytermMultiRecordIcon(from(bottomSheet).state)
            }

            val multiRecordModalButtonListener = View.OnClickListener {
                stopToolbarMedia()
                if (Workspace.activePhase.phaseType == PhaseType.KEYTERM && activity is KeyTermActivity) {
                    val bottomSheet = (activity as KeyTermActivity).bottomSheet

                    if(from(bottomSheet).state == STATE_EXPANDED) {
                        from(bottomSheet).state = STATE_COLLAPSED
                    }
                    else{
                        from(bottomSheet).state = STATE_EXPANDED
                    }
                } else if(Workspace.activePhase.phaseType != PhaseType.KEYTERM) {
                    recordingListener.onStartedRecordingOrPlayback(false)
                    RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
                }
            }
            multiRecordButton.setOnClickListener(multiRecordModalButtonListener)
        }
        if (enableSendAudioButton) {
            val sendAudioListener = View.OnClickListener {
                stopToolbarMedia()
                sendAudio()
            }
            sendAudioButton.setOnClickListener(sendAudioListener)
        }
    }

    /**
     * Depending on the state of the bottom sheet will determine the icon used.
     */
    private fun setKeytermMultiRecordIcon(state: Int){
        if(state == STATE_EXPANDED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_keyboard_arrow_down_white_48dp)
        }
        else if(state == STATE_COLLAPSED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_playlist_play_white_48dp)
        }
    }

    /*
    * Send single audio file to remote consultant
     */
    private fun sendAudio() {
/*
        Toast.makeText(appContext, R.string.audio_pre_send, Toast.LENGTH_SHORT).show()
        val phase = Workspace.activePhase
        val slideNum: Int
        val slide: File
        val totalSlides = Workspace.activeStory.slides.size
        if (phase.phaseType === PhaseType.BACKT) {
            slideNum = Workspace.activeSlideNum
            slide = AudioFiles.getBackTranslation(Workspace.activeStory.title, slideNum)
        } else {
            slideNum =  Workspace.activeStory.slides.size
            slide = AudioFiles.getWholeStory(Workspace.activeStory.title)
        }//Whole story bt audio will be uploaded as one slide past the final slide for the story

        requestRemoteReview(appContext, totalSlides)
        postABackTranslation(slideNum, slide)
*/
    }

    //Posts a single BT or WSBT
    fun postABackTranslation(slideNum: Int, slide: File) {
/*
        try {
            Upload(slide, appContext, slideNum)
        } catch (e: IOException) {
            e.printStackTrace()
        }

*/
    }

    //First time request for review
    fun requestRemoteReview(con: Context, numSlides: Int) {

 /*       // TODO replace with InstanceID getID for all phone ID locations
        val phone_id = Settings.Secure.getString(con.contentResolver,
                Settings.Secure.ANDROID_ID)
        js = HashMap()
        js!!["Key"] = con.resources.getString(R.string.api_token)
        js!!["PhoneId"] = phone_id
        js!!["TemplateTitle"] = StoryState.getStoryName()
        js!!["NumberOfSlides"] = Integer.toString(numSlides)

        val req = object : StringRequest(Request.Method.POST, con.getString(R.string.url_request_review), Response.Listener { response ->
            Log.i("LOG_VOLLEY_RESP_RR", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_RR", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            testErr = error.toString()
        }) {
            override fun getParams(): Map<String, String>? {
                return js
            }
        }


        VolleySingleton.getInstance(activity.applicationContext).addToRequestQueue(req)

 */   }

    //Subroutine to upload a single audio file
    @Throws(IOException::class)
    fun Upload(relPath: String, context: Context) {

/*
        val phone_id = Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ANDROID_ID)
        val templateTitle = Workspace.activeStory.title

        val currentSlide = Integer.toString(Workspace.activeSlideNum)
        val input = getStoryChildInputStream(context,relPath)
        val audioBytes = IOUtils.toByteArray(input)

        //get transcription text if it's there
        val prefs = context.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val transcription = prefs.getString(templateTitle + Workspace.activeSlideNum + TRANSCRIPTION_TEXT, "")
        val byteString = Base64.encodeToString(audioBytes, Base64.DEFAULT)

        js = HashMap()


        val log = LogFiles.getLog(FileSystem.getLanguage(), Workspace.activeStory.title)
        val logString = StringBuilder()
        if (log != null) {
            val logs = log.toTypedArray()

            //Grabs all applicable logs for this slide num
            val slideLogs = arrayOfNulls<String>(logs.size)

            for (log1 in logs) {
                if (log1 is ComChkEntry) {
                    if (log1.appliesToSlideNum(slide)) {
                        logString.append(log1.phase).append(" ").append(log1.description).append(" ").append(log1.dateTime).append("\n")
                    }

                } else if (log1 is LearnEntry) {
                    if (log1.appliesToSlideNum(slide)) {
                        logString.append(log1.phase).append(" ").append(log1.description).append(" ").append(log1.dateTime).append("\n")
                    }
                } else if (log1 is DraftEntry) {
                    if (log1.appliesToSlideNum(slide)) {
                        logString.append(log1.phase).append(" ").append(log1.description).append(" ").append(log1.dateTime).append("\n")
                    }
                } else {
                    val tempLog = log1 as LogEntry
                    if (tempLog.appliesToSlideNum(slide)) {
                        logString.append(tempLog.phase).append(" ").append(tempLog.description).append(" ").append(tempLog.dateTime).append("\n")
                    }

                }
            }
        }
        js!!["Log"] = logString.toString()
        js!!["Key"] = con.resources.getString(R.string.api_token)
        js!!["PhoneId"] = phone_id
        js!!["TemplateTitle"] = templateTitle
        js!!["SlideNumber"] = currentSlide
        js!!["Data"] = byteString
        js!!["BacktranslationText"] = transcription


        val req = object : paramStringRequest(Request.Method.POST, con.resources.getString(R.string.url_upload_audio), js, Response.Listener { response ->
            Log.i("LOG_VOLLEY_RESP_UPL", response)
            resp = response
            Toast.makeText(appContext, R.string.audio_Sent, Toast.LENGTH_SHORT).show()
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY_ERR_UPL", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            testErr = error.toString()
            Toast.makeText(appContext, R.string.audio_Send_Failed, Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                return this.mParams
            }
        }


        VolleySingleton.getInstance(activity.applicationContext).addToRequestQueue(req)

*/
    }

    /*
    * Start recording audio and hide buttons
     */
    private fun recordAudio(recordingRelPath: String) {
        stopToolbarMedia()
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

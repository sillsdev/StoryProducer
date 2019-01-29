package org.sil.storyproducer.tools.toolbar

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.media.MediaPlayer
import android.os.Handler
import android.support.v4.widget.Space
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.*
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder
import org.sil.storyproducer.tools.media.AudioRecorderMP4

import java.io.File
import java.io.IOException

private const val RECORDING_ANIMATION_DURATION = 1500
private const val STOP_RECORDING_DELAY = 0
private const val TAG = "AnimationToolbar"

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

open class RecordingToolbar
/**
 * The ctor.
 *
 * @param activity              The activity from the calling class.
 * @param rootViewToolbarLayout The viewToEmbedToolbarIn of the Toolbar layout called toolbar_for_recording.
 * must be of type LinearLayout so that buttons can be
 * evenly spaced.
 * @param rootView        The viewToEmbedToolbarIn of the layout that you want to embed the toolbar in.
 * @param enablePlaybackButton  Enable playback of recording.
 * @param enableDeleteButton    Enable the delete button, does not work as of now.
 * @param enableSendAudioButton Enable the sending of audio to the server
 */
@Throws(ClassCastException::class)
constructor(activity: Activity, rootViewToolbarLayout: View, rootView: View,
            protected var enablePlaybackButton: Boolean, protected var enableDeleteButton: Boolean,
            protected var enableMultiRecordButton: Boolean, protected var enableSendAudioButton: Boolean,
            private val multiRecordModal: Modal?, protected var recordingListener: RecordingListener,
            protected val slideNum: Int) : AnimationToolbar(activity) {

    //private FloatingActionButton fabPlus;
    protected var toolbar: LinearLayout = rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_toolbar)

    protected var rootViewToolbarLayout: LinearLayout = rootViewToolbarLayout as LinearLayout
    private val viewToEmbedToolbarIn: LinearLayout = rootView.findViewById(R.id.fragment_envelope) as LinearLayout
    protected var appContext: Context

    protected var micButton: ImageButton = ImageButton(activity)
    protected var playButton: ImageButton = ImageButton(activity)
    protected var deleteButton: ImageButton = ImageButton(activity)
    protected var multiRecordButton: ImageButton = ImageButton(activity)
    protected var sendAudioButton: ImageButton = ImageButton(activity)

    private var transitionDrawable: TransitionDrawable? = null
    private var colorHandler: Handler? = null
    private var colorHandlerRunnable: Runnable? = null
    private var isToolbarRed = false
    protected var voiceRecorder: AudioRecorder = AudioRecorderMP4(activity)
    protected var audioPlayer: AudioPlayer = AudioPlayer()
    val isRecordingOrPlaying : Boolean
        get() {return voiceRecorder.isRecording || audioPlayer.isAudioPlaying}

    init {
        super.initializeToolbar(rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_fab), toolbar)

        this.activity = activity
        this.appContext = activity.applicationContext //This is calling getApplicationContext because activity.getContext() cannot be accessed publicly.
        createToolbar()
        setupRecordingAnimationHandler()
        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
            audioPlayer.stopAudio()
            recordingListener.onStoppedRecordingOrPlayback()
        })
    }

    interface RecordingListener {
        fun onStoppedRecordingOrPlayback()
        fun onStartedRecordingOrPlayback(isRecording: Boolean)
    }

    /**
     * This function can be called so that the toolbar is automatically opened, without animation,
     * when the fragment is drawn.
     */
    fun keepToolbarVisible() {
        //hideFloatingActionButton();
        toolbar.visibility = View.VISIBLE
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    open fun stopToolbarMedia() {
        if (voiceRecorder.isRecording) {
            stopRecording()
            micButton.setBackgroundResource(R.drawable.ic_mic_white_48dp)
            //set playback button visible
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
        if (audioPlayer.isAudioPlaying) {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
            audioPlayer.stopAudio()
            recordingListener.onStoppedRecordingOrPlayback()
        }
    }

    /**
     * Calling class should be responsible for all other media
     * so [.stopPlayBackAndRecording] is not being used here.
     */
    open fun onPause() {
        stopToolbarMedia()
        audioPlayer.release()
    }
    
    open fun hideButtons() {
        if (enablePlaybackButton) {
            playButton.visibility = View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton.visibility = View.INVISIBLE
        }
        if (enableDeleteButton) {
            deleteButton.visibility = View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton.visibility = View.INVISIBLE
        }
    }

    protected open fun startRecording(recordingRelPath: String) {
        //TODO: make this logging more robust and encapsulated
        recordingListener.onStartedRecordingOrPlayback(true)
        voiceRecorder.startNewRecording(recordingRelPath)
        startRecordingAnimation(false, 0)
    }

    protected open fun stopRecording() {
        voiceRecorder.stop()
        stopRecordingAnimation()
        recordingListener.onStoppedRecordingOrPlayback()
    }

    private fun createToolbar() {
        setupToolbar()
        setupToolbarButtons()
    }

    /**
     * This function formats and aligns the buttons to the toolbar.
     */
    protected open fun setupToolbarButtons() {
        rootViewToolbarLayout.removeAllViews()
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val spaceLayoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        spaceLayoutParams.width = 0
        val drawables = intArrayOf(R.drawable.ic_mic_white_48dp, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_delete_forever_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_send_audio_48dp)
        val imageButtons = arrayOf(ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext))
        val buttonToDisplay = booleanArrayOf(true/*enable mic*/, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton)

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
                    2 -> deleteButton = imageButtons[i]
                    3 -> multiRecordButton = imageButtons[i]
                    4 -> sendAudioButton = imageButtons[i]
                }
            }
        }

        val playBackFileExist = storyRelPathExists(activity,Workspace.activePhase.getChosenFilename(slideNum))
        if (enablePlaybackButton) {
            playButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableMultiRecordButton) {
            multiRecordButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableDeleteButton) {
            deleteButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        if (enableSendAudioButton) {
            sendAudioButton.visibility = if (playBackFileExist) View.VISIBLE else View.INVISIBLE
        }
        setOnClickListeners()
    }

    /**
     * This function formats and aligns the toolbar and floating action button to the bottom of the relative layout of the
     * calling class.
     */
    protected fun setupToolbar() {
        val myParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        val myRules = intArrayOf(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.ALIGN_END)

        //Must remove all children of the layout, before appending them to the new rootView
        for (myRule in myRules) {
            myParams.addRule(myRule, viewToEmbedToolbarIn.id)
        }
        toolbar.layoutParams = myParams
        rootViewToolbarLayout.removeAllViews()
        viewToEmbedToolbarIn.addView(toolbar)
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    protected open fun setOnClickListeners() {
        val micListener = View.OnClickListener {
            if (voiceRecorder.isRecording) {
                stopRecording()
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
            } else {
                //Now we need to start recording!
                recordingListener.onStartedRecordingOrPlayback(true)
                val recordingRelPath = assignNewAudioRelPath()
                if (storyRelPathExists(activity,recordingRelPath)) {
                    val dialog = AlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.overwrite))
                            .setMessage(activity.getString(R.string.learn_phase_overwrite))
                            .setNegativeButton(activity.getString(R.string.no)) { dialog, id ->
                                //do nothing
                            }
                            .setPositiveButton(activity.getString(R.string.yes)) { dialog, id ->
                                //overwrite audio
                                recordAudio(recordingRelPath)
                            }.create()

                    dialog.show()
                } else {
                    recordAudio(recordingRelPath)
                }
            }
        }
        micButton.setOnClickListener(micListener)

        if (enablePlaybackButton) {
            val playListener = View.OnClickListener {
                if (audioPlayer.isAudioPlaying) {
                    audioPlayer.stopAudio()
                    playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
                    recordingListener.onStoppedRecordingOrPlayback()
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
        if (enableDeleteButton) {
            val deleteListener = View.OnClickListener { stopToolbarMedia() }
            deleteButton.setOnClickListener(deleteListener)
        }
        if (enableMultiRecordButton) {
            if (multiRecordModal != null) {
                val multiRecordModalButtonListener = View.OnClickListener {
                    stopToolbarMedia()
                    recordingListener.onStartedRecordingOrPlayback(false)
                    multiRecordModal.show()
                }

                multiRecordButton.setOnClickListener(multiRecordModalButtonListener)
            }

        }
        if (enableSendAudioButton) {
            val sendAudioListener = View.OnClickListener {
                stopToolbarMedia()
                sendAudio()
            }
            sendAudioButton.setOnClickListener(sendAudioListener)
        }
    }

    /*
        *Send single audio file to remote consultant
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
            PhaseType.DRAFT -> saveLog(activity.getString(R.string.DRAFT_RECORDING))
            PhaseType.COMMUNITY_CHECK -> saveLog(activity.getString(R.string.COMMENT_RECORDING))
            else -> {}
        }
        micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
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
    }

    /**
     * [See for handler](https://developer.android.com/reference/android/os/Handler.html)
     * <br></br>
     * [See for runnable](https://developer.android.com/reference/java/lang/Runnable.html)
     * <br></br>
     * [See for transition Drawable](https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable.html)
     * <br></br>
     * <br></br>
     * Call this function prior to calling the function to start the animation.  E.g.: <br></br>
     * [.setupRecordingAnimationHandler], should be called once<br></br>
     * [.startRecordingAnimation]{}
     * <br></br><br></br>
     * Essentially the function utilizes a Transition Drawable to interpolate between the red and
     * the toolbar color. (The colors are defined in an array and used in the transition drawable)
     * To schedule the running of the transition drawable a handler and runnable are used.<br></br><br></br>
     * The handler takes a runnable which schedules the transitionDrawable. The handler function
     * called postDelayed will delay the running of the next Runnable by the passed in value e.g.:
     * colorHandler.postDelayed(runnable goes here, time delay in MS). Make sure that isToolbarRed is set
     * to false initially.
     * <br></br><br></br>
     * Still confused about handlers, runnables, and the MessageQueue?
     * <br></br>
     * [See this excellent SO post for more info.](http://stackoverflow.com/questions/12877944/what-is-the-relationship-between-looper-handler-and-messagequeue-in-android)
     */
    private fun setupRecordingAnimationHandler() {
        val red = Color.rgb(255, 0, 0)
        var colorOfToolbar = Color.rgb(0, 0, 255) /*Arbitrary color value of blue used initially*/

        val relBackgroundColor = toolbar.background
        if (relBackgroundColor is ColorDrawable) {
            colorOfToolbar = relBackgroundColor.color
        }
        transitionDrawable = TransitionDrawable(arrayOf(ColorDrawable(colorOfToolbar), ColorDrawable(red)))
        toolbar.background = transitionDrawable

        colorHandler = Handler()
        colorHandlerRunnable = Runnable {
            //Animation to change the toolbar's color while recording
            if (isToolbarRed) {
                transitionDrawable!!.reverseTransition(RECORDING_ANIMATION_DURATION)
                isToolbarRed = false
            } else {
                transitionDrawable!!.startTransition(RECORDING_ANIMATION_DURATION)
                isToolbarRed = true
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
    protected fun startRecordingAnimation(isDelayed: Boolean, delay: Int) {
        if (colorHandler != null && colorHandlerRunnable != null) {
            if (isDelayed) {
                colorHandler!!.postDelayed(colorHandlerRunnable, delay.toLong())
            } else {
                colorHandler!!.post(colorHandlerRunnable)
            }
        }
    }

    /**
     * Stops the animation from continuing. The removeCallbacks function removes all
     * colorHandlerRunnable from the MessageQueue and also resets the toolbar to its original color.
     * (transitionDrawable.resetTransition();)
     */
    protected fun stopRecordingAnimation() {
        if (colorHandler != null && colorHandlerRunnable != null) {
            colorHandler!!.removeCallbacks(colorHandlerRunnable)
        }
        if (transitionDrawable != null) {
            transitionDrawable!!.resetTransition()
        }
    }
}

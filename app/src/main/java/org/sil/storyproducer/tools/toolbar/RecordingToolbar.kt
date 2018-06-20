package org.sil.storyproducer.tools.toolbar


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.provider.Settings
import android.support.v4.widget.Space
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.Modal
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.ComChkEntry
import org.sil.storyproducer.model.logging.DraftEntry
import org.sil.storyproducer.model.logging.LearnEntry
import org.sil.storyproducer.model.logging.LogEntry
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.Network.paramStringRequest
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.FileSystem
import org.sil.storyproducer.tools.file.LogFiles
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.AudioRecorder

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap

/**
 * The purpose of this class is to extend the animationToolbar while adding the recording animation
 * to the toolbar. <br></br><br></br>
 * This class utilizes an empty layout for the toolbar and floating action button found in this layout:
 * (toolbar_for_recording.xml). <br></br>
 * The toolbar is where buttons are added to.<br></br> The toolbar is then placed at the
 * bottom of the rootViewLayout that is passed in to the this class' constructor. So, the rootViewLayout
 * must be of type RelativeLayout because the code related to placing the toolbar in the
 * rootViewLayout requires the rootViewLayout to be of type RelativeLayout. See: [.setupToolbar]<br></br><br></br>
 * This class also saves the recording and allows playback <br></br> from the toolbar. see: [.createToolbar]
 * <br></br><br></br>
 */
open class RecordingToolbar
/**
 * The ctor.
 *
 * @param activity              The activity from the calling class.
 * @param rootViewToolbarLayout The rootViewToEmbedToolbarIn of the Toolbar layout called toolbar_for_recording.
 * must be of type LinearLayout so that buttons can be
 * evenly spaced.
 * @param rootViewLayout        The rootViewToEmbedToolbarIn of the layout that you want to embed the toolbar in.
 * @param enablePlaybackButton  Enable playback of recording.
 * @param enableDeleteButton    Enable the delete button, does not work as of now.
 * @param enableSendAudioButton Enable the sending of audio to the server
 * @param recordFilePath        The filepath that the recording will be saved under.
 */
@Throws(ClassCastException::class)
constructor(activity: Activity, rootViewToolbarLayout: View, rootViewLayout: RelativeLayout,
            protected var enablePlaybackButton: Boolean, protected var enableDeleteButton: Boolean,
            protected var enableMultiRecordButton: Boolean, protected var enableSendAudioButton: Boolean,
            protected var playbackRecordFilePath: String, protected var recordFilePath: String,
            private val multiRecordModal: Modal?, protected var recordingListener: RecordingListener) : AnimationToolbar(activity) {

    private val RECORDING_ANIMATION_DURATION = 1500
    private val STOP_RECORDING_DELAY = 0
    private val TAG = "AnimationToolbar"

    //private FloatingActionButton fabPlus;
    protected var toolbar: LinearLayout = rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_toolbar)

    protected var rootViewToolbarLayout: LinearLayout = rootViewToolbarLayout as LinearLayout
    private val rootViewToEmbedToolbarIn: View = rootViewLayout
    protected var appContext: Context

    protected var micButton: ImageButton = ImageButton(activity)
    protected var playButton: ImageButton = ImageButton(activity)
    protected var deleteButton: ImageButton = ImageButton(activity)
    protected var multiRecordButton: ImageButton = ImageButton(activity)
    protected var sendAudioButton: ImageButton = ImageButton(activity)
    private val auxiliaryMediaList: MutableList<AuxiliaryMedia> = ArrayList()


    var isRecording: Boolean = false
        protected set

    private var transitionDrawable: TransitionDrawable? = null
    private var colorHandler: Handler? = null
    private var colorHandlerRunnable: Runnable? = null
    private var isToolbarRed = false
    private var voiceRecorder: MediaRecorder? = null
    protected var audioPlayer: AudioPlayer = AudioPlayer()
    private val canOverwrite = false

    private var js: MutableMap<String, String>? = null
    private var resp: String? = null
    private var testErr: String? = null

    init {
        super.initializeToolbar(rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_fab), rootViewToolbarLayout.findViewById(R.id.toolbar_for_recording_toolbar))

        this.activity = activity
        this.appContext = activity.applicationContext //This is calling getApplicationContext because activity.getContext() cannot be accessed publicly.
        createToolbar()
        setupRecordingAnimationHandler()
        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener { playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp) })
    }

    interface RecordingListener {
        fun onStoppedRecording()
        fun onStartedRecordingOrPlayback(isRecording: Boolean)
    }

    /**
     * This function can be called so that the toolbar is automatically opened, without animation,
     * when the fragment is drawn.
     */
    fun keepToolbarVisible() {
        //hideFloatingActionButton();
        toolbar!!.visibility = View.VISIBLE
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    open fun stopToolbarMedia() {
        if (isRecording) {
            stopRecording()
            micButton.setBackgroundResource(R.drawable.ic_mic_white)
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
        }
    }

    fun releaseToolbarAudio() {
        audioPlayer.release()
    }

    /**
     * This function is used so that other potential media sources outside the toolbar can be
     * stopped if the toolbar is pressed.
     *
     * @param viewThatIsPlayingButton The button that is pressed to activate the media playback.
     * @param setButtonToDrawable     The drawable to set the trigger button to a different drawable
     * when the toolbar is touched and the media is stopped.
     * (The reset drawable for the button) Like pause to play button.
     * @param playingAudio            The audio source of playback.
     */
    fun onToolbarTouchStopAudio(viewThatIsPlayingButton: View, setButtonToDrawable: Int, playingAudio: AudioPlayer) {
        val auxiliaryMedia = AuxiliaryMedia()
        auxiliaryMedia.playingAudio = playingAudio
        auxiliaryMedia.setButtonToDrawableOnStop = setButtonToDrawable
        auxiliaryMedia.viewThatIsPlayingButton = viewThatIsPlayingButton
        auxiliaryMediaList!!.add(auxiliaryMedia)
    }

    /**
     * Calling class should be responsible for all other media
     * so [.stopPlayBackAndRecording] is not being used here.
     */
    open fun onClose() {
        stopToolbarMedia()
    }


    fun closeToolbar() {
        if (toolbar != null) {
            super.close()
        }
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

    protected fun startRecording() {
        //TODO: make this logging more robust and encapsulated
        if (Workspace.activePhase.phaseType === PhaseType.DRAFT) {
            LogFiles.saveLogEntry(DraftEntry.Type.DRAFT_RECORDING.makeEntry())
        }
        startAudioRecorder()
        startRecordingAnimation(false, 0)
        recordingListener.onStartedRecordingOrPlayback(true)

    }

    protected open fun stopRecording() {
        stopAudioRecorder()
        stopRecordingAnimation()
        recordingListener.onStoppedRecording()
    }

    //TODO finish adding deletion functionality.
    protected fun deleteRecording(): Boolean {
        return if (enableDeleteButton) {
            false
        } else {
            false
        }
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
        val drawables = intArrayOf(R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_delete_forever_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_send_audio_48dp)
        val imageButtons = arrayOf(ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext), ImageButton(appContext))
        val buttonToDisplay = booleanArrayOf(true/*enable mic*/, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton)

        var buttonSpacing = Space(appContext)
        buttonSpacing.layoutParams = spaceLayoutParams
        toolbar!!.addView(buttonSpacing) //Add a space to the left of the first button.
        for (i in drawables.indices) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i])
                imageButtons[i].visibility = View.VISIBLE
                imageButtons[i].layoutParams = layoutParams
                toolbar!!.addView(imageButtons[i])

                buttonSpacing = Space(appContext)
                buttonSpacing.layoutParams = spaceLayoutParams
                toolbar!!.addView(buttonSpacing)
                when (i) {
                    0 -> micButton = imageButtons[i]
                    1 -> playButton = imageButtons[i]
                    2 -> deleteButton = imageButtons[i]
                    3 -> multiRecordButton = imageButtons[i]
                    4 -> sendAudioButton = imageButtons[i]
                }
            }
        }

        val playBackFileExist = File(playbackRecordFilePath).exists()
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
        val myParams = arrayOf(/*new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT),*/
                RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT))
        val myRules = intArrayOf(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_END, RelativeLayout.ALIGN_PARENT_RIGHT)
        val myView = arrayOf<View>(/*fabPlus,*/toolbar)

        //Must remove all children of the layout, before appending them to the new rootView
        rootViewToolbarLayout.removeAllViews()
        for (i in myParams.indices) {
            for (myRule in myRules) {
                myParams[i].addRule(myRule, rootViewToEmbedToolbarIn.id)
            }
            myView[i].layoutParams = myParams[i]
            (rootViewToEmbedToolbarIn as RelativeLayout).addView(myView[i])
        }
        //Index corresponds to the myView array
        //fabPlus = (FloatingActionButton) myView[0];
        toolbar = myView[0] as LinearLayout
    }

    /**
     * Enables the buttons to have the appropriate onClick listeners.
     */
    protected open fun setOnClickListeners() {
        val micListener = View.OnClickListener {
            if (isRecording) {
                stopRecording()
                micButton.setBackgroundResource(R.drawable.ic_mic_white)
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
                //learn phase overwrite dialog
                if (Workspace.activePhase.phaseType === PhaseType.LEARN ||
                        Workspace.activePhase.phaseType === PhaseType.WHOLE_STORY) {
                    val recordingExists = File(recordFilePath).exists()
                    if (recordingExists) {
                        val dialog = AlertDialog.Builder(activity)
                                .setTitle(activity.getString(R.string.overwrite))
                                .setMessage(activity.getString(R.string.learn_phase_overwrite))
                                .setNegativeButton(activity.getString(R.string.no)) { dialog, id ->
                                    //do nothing
                                }
                                .setPositiveButton(activity.getString(R.string.yes)) { dialog, id ->
                                    //overwrite audio
                                    recordAudio()
                                }.create()

                        dialog.show()

                    } else {
                        recordAudio()
                    }
                } else {
                    recordAudio()
                }
            }
        }
        micButton.setOnClickListener(micListener)

        if (enablePlaybackButton) {
            val playListener = View.OnClickListener {
                if (audioPlayer.isAudioPlaying) {
                    audioPlayer.stopAudio()
                    playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
                } else {
                    stopPlayBackAndRecording()
                    if (audioPlayer.setSource(this.appContext,)) {

                        audioPlayer.playAudio()
                        Toast.makeText(appContext, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show()
                        playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
                        recordingListener.onStartedRecordingOrPlayback(false)
                        //TODO: make this logging more robust and encapsulated
                        if (Workspace.activePhase.phaseType === PhaseType.DRAFT) {
                            LogFiles.saveLogEntry(DraftEntry.Type.DRAFT_PLAYBACK.makeEntry())
                        }
                    } else {
                        Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            playButton.setOnClickListener(playListener)
        }
        if (enableDeleteButton) {
            val deleteListener = View.OnClickListener { stopPlayBackAndRecording() }
            deleteButton.setOnClickListener(deleteListener)
        }
        if (enableMultiRecordButton) {
            if (multiRecordModal != null) {
                val multiRecordModalButtonListener = View.OnClickListener {
                    stopPlayBackAndRecording()
                    multiRecordModal.show()
                }

                multiRecordButton.setOnClickListener(multiRecordModalButtonListener)
            }

        }
        if (enableSendAudioButton) {
            val sendAudioListener = View.OnClickListener {
                stopPlayBackAndRecording()
                sendAudio()
            }
            sendAudioButton.setOnClickListener(sendAudioListener)
        }
    }

    /*
        *Send single audio file to remote consultant
         */
    private fun sendAudio() {
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
    }

    //Posts a single BT or WSBT
    fun postABackTranslation(slideNum: Int, slide: File) {
        try {
            Upload(slide, appContext, slideNum)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    //First time request for review
    fun requestRemoteReview(con: Context, numSlides: Int) {

        // TODO replace with InstanceID getID for all phone ID locations
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

    }

    //Subroutine to upload a single audio file
    @Throws(IOException::class)
    fun Upload(fileName: File, con: Context, slide: Int) {

        val phone_id = Settings.Secure.getString(con.contentResolver,
                Settings.Secure.ANDROID_ID)
        val templateTitle = StoryState.getStoryName()

        val currentSlide = Integer.toString(slide)
        val input = FileInputStream(fileName)
        val audioBytes = IOUtils.toByteArray(input)

        //get transcription text if it's there
        val prefs = con.getSharedPreferences(R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val transcription = prefs.getString(templateTitle + slide + TRANSCRIPTION_TEXT, "")
        val byteString = Base64.encodeToString(audioBytes, Base64.DEFAULT)

        js = HashMap()


        val log = LogFiles.getLog(FileSystem.getLanguage(), StoryState.getStoryName())
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

    }

    /*
    * Start recording audio and hide buttons
     */
    private fun recordAudio() {
        stopPlayBackAndRecording()
        startRecording()
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

        val relBackgroundColor = toolbar!!.background
        if (relBackgroundColor is ColorDrawable) {
            colorOfToolbar = relBackgroundColor.color
        }
        transitionDrawable = TransitionDrawable(arrayOf(ColorDrawable(colorOfToolbar), ColorDrawable(red)))
        toolbar!!.background = transitionDrawable

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

    /**
     * The function that aids in starting an audio recorder.
     */
    protected open fun startAudioRecorder() {
        setVoiceRecorder(recordFilePath)
        try {
            isRecording = true
            voiceRecorder!!.prepare()
            voiceRecorder!!.start()
            Toast.makeText(appContext, R.string.recording_toolbar_recording_voice, Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not record voice.", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not record voice.", e)
        }

    }

    /**
     * The function that aids in stopping an audio recorder.
     */
    protected fun stopAudioRecorder() {
        try {
            isRecording = false
            //Delay stopping of voiceRecorder to capture all of the voice recorded.
            Thread.sleep(STOP_RECORDING_DELAY.toLong())
            voiceRecorder!!.stop()
            Toast.makeText(appContext, R.string.recording_toolbar_stop_recording_voice, Toast.LENGTH_SHORT).show()
        } catch (stopException: RuntimeException) {
            Toast.makeText(appContext, R.string.recording_toolbar_error_recording, Toast.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Voice recorder interrupted!", e)
        }

        voiceRecorder!!.release()
        voiceRecorder = null
    }

    /**
     * This function sets the voice recorder with a new voiceRecorder.
     *
     * @param fileName The file to output the voice recordings.
     */
    protected fun setVoiceRecorder(fileName: String) {
        voiceRecorder = AudioRecorder(fileName, activity)
    }

    //TODO The arraylist is being populated by null objects. This is because the other classes are releasing too much. Will be taken care of once lockeridge's branch Audio Player fix is merged into dev
    /**
     * This function stops all playback and all auxiliary media.
     */
    protected fun stopPlayBackAndRecording() {
        stopToolbarMedia()
        if (auxiliaryMediaList != null) {
            for (am in auxiliaryMediaList) {
                am.stopPlaying()
            }
        }
    }

    /**
     * Use this class to hold media that should be stopped when a toolbar button is pressed.
     * <br></br>Refer to function [.onToolbarTouchStopAudio] for more information.
     */
    protected inner class AuxiliaryMedia {
        internal var viewThatIsPlayingButton: View? = null
        internal var setButtonToDrawableOnStop: Int = 0
        internal var playingAudio: AudioPlayer? = null

        internal fun stopPlaying() {
            if (playingAudio != null && playingAudio!!.isAudioPlaying) {
                playingAudio!!.stopAudio()
                //playingAudio.releaseAudio();
                if (viewThatIsPlayingButton != null) {
                    viewThatIsPlayingButton!!.setBackgroundResource(setButtonToDrawableOnStop)
                }
            }
        }
    }

    companion object {
        val R_CONSULTANT_PREFS = "Consultant_Checks"
        private val TRANSCRIPTION_TEXT = "TranscriptionText"
    }
}

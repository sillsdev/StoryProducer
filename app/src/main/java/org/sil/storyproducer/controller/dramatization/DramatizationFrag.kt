package org.sil.storyproducer.controller.dramatization

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.file.TextFiles
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener

import java.io.File


class DramatizationFrag : Fragment() {

    private var rootView: View? = null
    private var rootViewToolbar: View? = null
    private var slideNumber: Int = 0
    private var slideText: EditText? = null
    private var storyName: String? = null
    private var phaseUnlocked: Boolean = false
    private var draftPlayer: AudioPlayer? = null
    private var draftAudioExists: Boolean = false
    private var dramatizationRecordingFile: File? = null
    private var draftPlayButton: ImageButton? = null


    private var recordingToolbar: PausingRecordingToolbar? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        val passedArgs = this.arguments
        slideNumber = passedArgs.getInt(SLIDE_NUM)
        storyName = StoryState.getStoryName()
        phaseUnlocked = StorySharedPreferences.isApproved(storyName, context)
        setRecordFilePath()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_dramatization, container, false)
        draftPlayButton = rootView!!.findViewById(R.id.fragment_dramatization_play_draft_button)
        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_dramatization_image_view) as ImageView, slideNumber)
        val slideNumberText = rootView!!.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText.text = slideNumber.toString() + ""
        slideText = rootView!!.findViewById(R.id.fragment_dramatization_edit_text)
        slideText!!.setText(TextFiles.getDramatizationText(StoryState.getStoryName(), slideNumber), TextView.BufferType.EDITABLE)

        if (phaseUnlocked) {
            rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
            closeKeyboardOnTouch(rootView)
            rootView!!.findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView!!)
        }

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_dramatize)
    }

    override fun onStart() {
        super.onStart()

        if (phaseUnlocked) {
            setToolbar(rootViewToolbar)
        }

        draftPlayer = AudioPlayer()
        val draftAudioFile = AudioFiles.getDraft(storyName!!, slideNumber)
        if (draftAudioFile.exists()) {
            draftAudioExists = true
            //FIXME
            //draftPlayer.setSource(draftAudioFile.getPath());
        } else {
            draftAudioExists = false
        }
        draftPlayer!!.onPlayBackStop(MediaPlayer.OnCompletionListener { draftPlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp) })

        setPlayStopDraftButton(rootView!!.findViewById<View>(R.id.fragment_dramatization_play_draft_button) as ImageButton)
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        if (recordingToolbar != null) {
            recordingToolbar!!.onPause()
        }
        closeKeyboard(rootView)
        TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText!!.text.toString())
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on stop.
     */
    override fun onStop() {
        super.onStop()
        draftPlayer!!.release()
        if (recordingToolbar != null) {
            recordingToolbar!!.onPause()
            recordingToolbar!!.releaseToolbarAudio()
        }

        closeKeyboard(rootView)
        TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText!!.text.toString())
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar!!.onPause()
                }
                closeKeyboard(rootView)
                TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText!!.text.toString())
            }
        }
    }

    /**
     * Used to stop playing and recording any media. The calling class should be responsible for
     * stopping its own media. Used in [DramaListRecordingsModal].
     */
    fun stopPlayBackAndRecording() {
        recordingToolbar!!.stopToolbarMedia()
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    fun hideButtonsToolbar() {
        recordingToolbar!!.hideButtons()
    }

    /**
     * Stop the toolbar from continuing the appending session.
     */
    fun stopAppendingRecordingFile() {
        recordingToolbar!!.stopAppendingSession()
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private fun setUiColors() {
        if (slideNumber == 0) {
            val rl = rootView!!.findViewById<RelativeLayout>(R.id.fragment_dramatization_root_layout)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
        }
    }

    /**
     * This function is used to the set the picture per slide.
     *
     * @param slideImage    The view that will have the picture rendered on it.
     * @param slideNum The respective slide number for the dramatization slide.
     */
    private fun setPic(slideImage: ImageView, slideNum: Int) {

        var slidePicture: Bitmap? = ImageFiles.getBitmap(storyName, slideNum)

        if (slidePicture == null) {
            Snackbar.make(rootView!!, R.string.dramatization_draft_no_picture, Snackbar.LENGTH_SHORT).show()
        }

        //Get the height of the phone.
        val phoneProperties = context.resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture!!, height)

        //Set the height of the image view
        slideImage.layoutParams.height = height
        slideImage.requestLayout()

        slideImage.setImageBitmap(slidePicture)
    }

    /**
     * sets the playback path
     */
    fun setPlayBackPath() {
        //FIXME
        //String playBackFilePath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).getPath();
        //recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * This function serves to set the play and stop button for the draft playback button.
     */

    private fun setPlayStopDraftButton(playPauseDraftButton: ImageButton) {

        if (!draftAudioExists) {
            //draft recording does not exist
            playPauseDraftButton.alpha = 0.8f
            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200))
        } else {
            //remove x mark from ImageButton play
            playPauseDraftButton.setImageResource(0)
        }
        playPauseDraftButton.setOnClickListener {
            if (!draftAudioExists) {
                Toast.makeText(context, R.string.dramatization_no_draft_recording_available, Toast.LENGTH_SHORT).show()
            } else if (draftPlayer!!.isAudioPlaying) {
                draftPlayer!!.pauseAudio()
                playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)
            } else {
                recordingToolbar!!.stopToolbarMedia()
                playPauseDraftButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                draftPlayer!!.playAudio()

                if (draftPlayer != null) { //if there is a draft available to play
                    recordingToolbar!!.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_arrow_white_48dp, draftPlayer!!)
                }
                Toast.makeText(context, R.string.dramatization_playback_draft_recording, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRecordFilePath() {
        var nextDraftIndex = AudioFiles.getDramatizationTitles(StoryState.getStoryName(), slideNumber).size + 1
        var recordFile = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber, getString(R.string.dramatization_record_file_drama_name, nextDraftIndex))
        while (recordFile.exists()) {
            nextDraftIndex++
            recordFile = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber, getString(R.string.dramatization_record_file_drama_name, nextDraftIndex))
        }
        dramatizationRecordingFile = recordFile
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar(toolbar: View?) {
        if (rootView is RelativeLayout) {
            val playBackFilePath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).path
            val recordingListener = object : RecordingListener {
                override fun onStoppedRecording() {
                    //update to new recording path
                    setRecordFilePath()
                    recordingToolbar!!.setRecordFilePath(dramatizationRecordingFile!!.absolutePath)
                }

                override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                    if (isRecording) {
                        val title = AudioFiles.getDramatizationTitle(dramatizationRecordingFile!!)
                        StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName())
                        //update to old recording or whatever was set by StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                        setPlayBackPath()
                    }
                }
            }
            val modal = DramaListRecordingsModal(context, slideNumber, this)

            //fix with proper slide num
            recordingToolbar = PausingRecordingToolbar(activity, toolbar, rootView as RelativeLayout?,
                    true, false, true, false, playBackFilePath, dramatizationRecordingFile!!.absolutePath, modal, recordingListener, 0)
            recordingToolbar!!.keepToolbarVisible()
        }
    }

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: [.closeKeyboard].
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     * the view will close the softkeyboard.
     */
    private fun closeKeyboardOnTouch(touchedView: View?) {
        touchedView?.setOnClickListener { closeKeyboard(touchedView) }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     */
    private fun closeKeyboard(viewToFocus: View?) {
        if (viewToFocus != null) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
    }

    companion object {
        val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
    }

}

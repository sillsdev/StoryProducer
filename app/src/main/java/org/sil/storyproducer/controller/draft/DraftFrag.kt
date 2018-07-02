package org.sil.storyproducer.controller.draft

import android.graphics.Bitmap
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.model.SlideText
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.model.logging.DraftEntry
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.file.LogFiles
import org.sil.storyproducer.tools.file.TextFiles
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener

import java.io.File

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class DraftFrag : Fragment() {
    private var rootView: View? = null
    private var rootViewToolbar: View? = null
    private var storyName: String? = null
    private var slideNumber: Int = 0
    private var slideText: SlideText? = null

    private var LWCAudioPlayer: AudioPlayer? = null
    private var recordFile: File? = null
    private val LWCAudioExists: Boolean = false
    private var LWCPlayButton: ImageButton? = null


    private val recordingToolbar: RecordingToolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passedArgs = this.arguments
        storyName = StoryState.getStoryName()
        slideNumber = passedArgs.getInt(SLIDE_NUM)
        slideText = TextFiles.getSlideText(storyName, slideNumber)
        setRecordFilePath()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater!!.inflate(R.layout.fragment_draft, container, false)
        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)

        LWCPlayButton = rootView!!.findViewById(R.id.fragment_draft_lwc_audio_button)

        LWCPlayButton = rootView!!.findViewById(R.id.fragment_draft_lwc_audio_button)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_draft_image_view) as ImageView, slideNumber)
        setScriptureText(rootView!!.findViewById<View>(R.id.fragment_draft_scripture_text) as TextView)
        setReferenceText(rootView!!.findViewById<View>(R.id.fragment_draft_reference_text) as TextView)
        setLWCAudioButton(LWCPlayButton!!)
        val slideNumberText = rootView!!.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText.text = slideNumber.toString() + ""

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_draft)
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                recordingToolbar?.onPause()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        setToolbar(rootViewToolbar)

        LWCAudioPlayer = AudioPlayer()
        //FIXME
        /*
        File LWCFile = AudioFiles.getNarration(storyName, slideNumber);
        if (LWCFile.exists()) {
            LWCAudioExists = true;
            LWCAudioPlayer.setSource(LWCFile.getPath());
        } else {
            LWCAudioExists = false;
        }
*/

        LWCAudioPlayer!!.onPlayBackStop(MediaPlayer.OnCompletionListener { LWCPlayButton!!.setBackgroundResource(R.drawable.ic_menu_play) })
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        recordingToolbar?.onPause()
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    override fun onStop() {
        super.onStop()

        recordingToolbar!!.stopToolbarMedia()
        LWCAudioPlayer!!.release()

        if (recordingToolbar != null) {
            recordingToolbar.onPause()
            recordingToolbar.releaseToolbarAudio()
        }

    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    fun hideButtonsToolbar() {
        recordingToolbar!!.hideButtons()
    }

    /**
     * sets the playback path
     */
    fun updatePlayBackPath() {
        //FIXME
        //String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        //recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * Stops the toolbar from recording or playing back media.
     * Used in [DraftListRecordingsModal]
     */
    fun stopPlayBackAndRecording() {
        recordingToolbar!!.stopToolbarMedia()
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private fun setUiColors() {
        if (slideNumber == 0) {
            var rl = rootView!!.findViewById<RelativeLayout>(R.id.fragment_draft_root_relayout_layout)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.fragment_draft_envelope)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.fragment_draft_text_envelope)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))


            var tv = rootView!!.findViewById<TextView>(R.id.fragment_draft_scripture_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            tv = rootView!!.findViewById(R.id.fragment_draft_reference_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
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
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private fun setScriptureText(textView: TextView) {
        textView.text = slideText!!.content
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private fun setReferenceText(textView: TextView) {
        val titleNamePriority = arrayOf(slideText!!.reference, slideText!!.subtitle, slideText!!.title)

        for (title in titleNamePriority) {
            if (title != null && title != "") {
                textView.text = title
                return
            }
        }
        textView.setText(R.string.draft_bible_story)
    }

    /**
     * This function sets the LWC playback to the correct audio file. Also, the LWC narration
     * button will have a listener added to it in order to detect playback when pressed.
     *
     * @param LWCPlayButton the button to set the listeners for
     */
    private fun setLWCAudioButton(LWCPlayButton: ImageButton) {
        LWCPlayButton.setOnClickListener {
            if (!LWCAudioExists) {
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                if (LWCAudioPlayer!!.isAudioPlaying) {
                    LWCAudioPlayer!!.stopAudio()
                    LWCPlayButton.setBackgroundResource(R.drawable.ic_menu_play)
                } else {
                    //stop other playback streams.
                    recordingToolbar!!.stopToolbarMedia()
                    LWCAudioPlayer!!.playAudio()
                    recordingToolbar?.onToolbarTouchStopAudio(LWCPlayButton, R.drawable.ic_menu_play, LWCAudioPlayer!!)

                    LWCPlayButton.setBackgroundResource(R.drawable.ic_stop_white_36dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    LogFiles.saveLogEntry(DraftEntry.Type.LWC_PLAYBACK.makeEntry())
                }
            }
        }
    }

    private fun setRecordFilePath() {
        var nextDraftIndex = AudioFiles.getDraftTitles(StoryState.getStoryName(), slideNumber).size + 1
        var recordFile = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber, getString(R.string.draft_record_file_draft_name, nextDraftIndex))
        while (recordFile.exists()) {
            nextDraftIndex++
            recordFile = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber, getString(R.string.draft_record_file_draft_name, nextDraftIndex))
        }
        this.recordFile = recordFile
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar(toolbar: View?) {
        if (rootView is RelativeLayout) {
            val playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).path
            val recordingListener = object : RecordingListener {
                override fun onStoppedRecording() {
                    val title = AudioFiles.getDraftTitle(recordFile!!)
                    StorySharedPreferences.setDraftForSlideAndStory(title, slideNumber, StoryState.getStoryName())     //save the draft  title for the recording
                    setRecordFilePath()
                    //FIXME
                    //recordingToolbar.setRecordFilePath(recordFile.getAbsolutePath());
                    updatePlayBackPath()
                }

                override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                    //not used here
                }
            }
            val modal = DraftListRecordingsModal(context, slideNumber, this)

            //FIXME
            //recordingToolbar = new RecordingToolbar(getActivity(), toolbar, (RelativeLayout) rootView,
            //        true, false, true, false, playBackFilePath, recordFile.getAbsolutePath(), modal , recordingListener);
            recordingToolbar!!.keepToolbarVisible()
            recordingToolbar.stopToolbarMedia()
        }
    }

    companion object {
        val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
    }
}

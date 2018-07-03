package org.sil.storyproducer.controller

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsList
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.DraftEntry
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.LogFiles
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : Fragment() {
    protected var rootView: View? = null
    protected var rootViewToolbar: View? = null

    protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    protected var referncePlayButton: ImageButton? = null

    protected var recordingToolbar: RecordingToolbar? = null
    protected var slideNum: Int = 0 //gets overwritten
    protected var slide: Slide = Workspace.activeSlide!! //this is a placeholder that gets overwritten in onCreate.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = this.arguments.getInt(SLIDE_NUM)
        slide = Workspace.activeStory.slides[slideNum]
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater!!.inflate(R.layout.fragment_multirecord, container, false)
        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
        setToolbar()

        referncePlayButton = rootView!!.findViewById(R.id.fragment_reference_audio_button)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_mr_image_view) as ImageView)
        setReferenceAudioButton(referncePlayButton!!)
        val slideNumberText = rootView!!.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText.text = slideNum.toString()

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

    override fun onResume() {
        super.onResume()

        referenceAudioPlayer = AudioPlayer()
        referenceAudioPlayer.setStorySource(context,Workspace.activePhase.getReferenceAudioFile(slideNum))

        referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener { referncePlayButton!!.setBackgroundResource(R.drawable.ic_menu_play) })
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        recordingToolbar!!.onPause()

        recordingToolbar!!.stopToolbarMedia()
        referenceAudioPlayer.release()

        recordingToolbar!!.releaseToolbarAudio()
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    fun hideButtonsToolbar() {
        recordingToolbar!!.hideButtons()
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
        if (slideNum == 0) {
            var rl = rootView!!.findViewById<RelativeLayout>(R.id.fragment_mr_root_relayout_layout)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.fragment_mr_envelope)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.fragment_mr_text_envelope)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))


            var tv = rootView!!.findViewById<TextView>(R.id.fragment_mr_scripture_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            tv = rootView!!.findViewById(R.id.fragment_mr_reference_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private fun setPic(slideImage: ImageView) {
        var slidePicture: Bitmap? = getStoryImage(context,slideNum)

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

    private fun setReferenceAudioButton(playButton: ImageButton) {
        playButton.setOnClickListener {
            if (!storyRelPathExists(context,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                if (referenceAudioPlayer.isAudioPlaying) {
                    referenceAudioPlayer.stopAudio()
                    playButton.setBackgroundResource(R.drawable.ic_menu_play)
                } else {
                    //stop other playback streams.
                    recordingToolbar!!.stopToolbarMedia()
                    referenceAudioPlayer.playAudio()
                    recordingToolbar?.onToolbarTouchStopAudio(playButton, R.drawable.ic_menu_play, referenceAudioPlayer)

                    playButton.setBackgroundResource(R.drawable.ic_stop_white_36dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    LogFiles.saveLogEntry(DraftEntry.Type.LWC_PLAYBACK.makeEntry())
                }
            }
        }
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar() {
        val recordingListener = object : RecordingListener {
            override fun onStoppedRecording() {
                //updatePlayBackPath()
            }

            override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                //not used here
            }
        }
        val rList = RecordingsList(context, this)

        recordingToolbar = RecordingToolbar(this.activity, rootViewToolbar!!, rootView as RelativeLayout,
                true, false, true, false,  rList , recordingListener, slideNum);
        recordingToolbar!!.keepToolbarVisible()
        recordingToolbar!!.stopToolbarMedia()
    }
    companion object {
        const val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"

    }
}

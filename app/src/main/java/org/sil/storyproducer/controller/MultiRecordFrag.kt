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
abstract class MultiRecordFrag : SlidePhaseFrag() {

    protected var recordingToolbar: RecordingToolbar? = null


    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootViewToolbar = inflater!!.inflate(R.layout.toolbar_for_recording, container, false)
        setToolbar()
        return rootView
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

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        recordingToolbar?.onPause()
        recordingToolbar?.stopToolbarMedia()
        recordingToolbar?.releaseToolbarAudio()
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    fun hideButtonsToolbar() {
        recordingToolbar?.hideButtons()
    }


    /**
     * Stops the toolbar from recording or playing back media.
     * Used in [DraftListRecordingsModal]
     */
    fun stopPlayBackAndRecording() {
        recordingToolbar?.stopToolbarMedia()
    }



    override fun setReferenceAudioButton(playButton: ImageButton) {
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
    protected open fun setToolbar() {
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
}
package org.sil.storyproducer.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag() {

    protected var recordingToolbar: RecordingToolbar? = null


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
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
}

package org.sil.storyproducer.controller

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R

import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class MultiRecordFrag : SlidePhaseFrag(), RecordingListener{

    protected var recordingToolbar: RecordingToolbar = RecordingToolbar()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if(Workspace.activeStory.slides[slideNum].slideType != SlideType.LOCALCREDITS){
            setToolbar()
        }
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
                recordingToolbar.pause()
            }
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        recordingToolbar.pause()
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    fun hideButtonsToolbar() {
        recordingToolbar.hideButtons()
    }

    override fun stopPlayBackAndRecording() {
        super.stopPlayBackAndRecording()
        recordingToolbar.stopToolbarMedia()
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        //updatePlayBackPath()
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        stopPlayBackAndRecording()
    }

    protected open fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,false,true,false))
        bundle.putInt("slideNum", slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }
}

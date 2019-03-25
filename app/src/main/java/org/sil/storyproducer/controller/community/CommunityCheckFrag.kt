package org.sil.storyproducer.controller.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityCheckFrag : MultiRecordFrag(), RecordingToolbar.RecordingListener {
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_community_check, container, false)

        setPic(rootView!!.findViewById(R.id.fragment_image_view))
        setToolbar()
        dispList = RecordingsListAdapter.RecordingsListModal(context!!, recordingToolbar)
        dispList?.embedList(rootView!! as ViewGroup)
        dispList?.setSlideNum(slideNum)
        //This enables the "onStartedPlaybackOrRecording" to be invoked.
        dispList?.setParentFragment(this)
        dispList?.show()

        setupCameraAndEditButton()

        return rootView
    }

    override fun onPause() {
        super.onPause()
        dispList?.stopAudio()
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     */

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        dispList?.stopAudio()
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        dispList?.updateRecordingList()
        dispList?.recyclerView?.adapter?.notifyDataSetChanged()
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        stopPlayBackAndRecording()
        //this is needed here to - when you are playing the reference audio and start recording
        //the new audio file pops up, and in the wrong format.
        dispList?.updateRecordingList()
    }

    override fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(false,false,false,false))
        bundle.putInt("slideNum", slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
        recordingToolbar.stopToolbarMedia()
    }

    override fun stopPlayBackAndRecording() {
        super.stopPlayBackAndRecording()
        dispList!!.stopAudio()
    }
}

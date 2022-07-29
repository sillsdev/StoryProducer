package org.tyndalebt.spadv.controller.communitywork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.MultiRecordFrag
import org.tyndalebt.spadv.controller.adapter.RecordingsListAdapter
import org.tyndalebt.spadv.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityWorkFrag : MultiRecordFrag() {
    override var recordingToolbar: RecordingToolbar = RecordingToolbar()
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_community_work, container, false)

        setPic(rootView!!.findViewById(R.id.fragment_image_view))
        setToolbar()
        dispList = RecordingsListAdapter.RecordingsListModal(context!!, recordingToolbar)
        dispList?.embedList(rootView!! as ViewGroup)
        dispList?.setSlideNum(slideNum)
        //This enables the "onStartedToolbarMedia" to be invoked.
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

    override fun onStartedToolbarMedia() {
        super.onStartedToolbarMedia()

        dispList!!.stopAudio()
        //this is needed here to - when you are playing the reference audio and start recording
        //the new audio file pops up, and in the wrong format.
        dispList?.resetRecordingList()
    }

    override fun onStartedSlidePlayBack() {
        super.onStartedSlidePlayBack()

        dispList!!.stopAudio()
    }
}

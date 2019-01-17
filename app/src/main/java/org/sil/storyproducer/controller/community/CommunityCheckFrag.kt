package org.sil.storyproducer.controller.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityCheckFrag : MultiRecordFrag() {
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_community_check, container, false)

        setPic(rootView!!.findViewById(R.id.fragment_image_view))
        setToolbar()
        recordingToolbar?.toolbar?.findViewWithTag<ImageButton>("tag")?.setOnClickListener {
            recordingToolbar?.setMicListener()
            (dispList?.recyclerView?.adapter)?.notifyDataSetChanged()
        }
        dispList = RecordingsListAdapter.RecordingsListModal(rootView, context!!, recordingToolbar!!)
        dispList?.embedList(rootView!! as ViewGroup)
        dispList?.show()
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

    override fun setToolbar() {
        val recordingListener = object : RecordingToolbar.RecordingListener {
            override fun onStoppedRecording() {
            }

            override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                stopPlayBackAndRecording()
            }
        }

        recordingToolbar = RecordingToolbar(activity!!, rootView!!,
                false, false, false, false, recordingListener, slideNum)
        recordingToolbar?.keepToolbarVisible()
        recordingToolbar?.stopToolbarMedia()
    }

    /**
     * Stops the toolbar from recording or playing back media.
     * Used in [DraftListRecordingsModal]
     */
    override fun stopPlayBackAndRecording() {
        super.stopPlayBackAndRecording()
        dispList!!.stopAudio()
    }
}
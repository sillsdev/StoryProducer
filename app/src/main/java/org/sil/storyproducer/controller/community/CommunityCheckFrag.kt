package org.sil.storyproducer.controller.community

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityCheckFrag : Fragment(), RecordingToolbar.RecordingListener, MultiRecordFrag.PlaybackListener {
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null
    private var recordingToolbar: RecordingToolbar = RecordingToolbar()
    private val multiRecordFrag = MultiRecordFrag()
    private var slideNum: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_community_check, container, false)

        slideNum = arguments?.getInt(SlidePhaseFrag.SLIDE_NUM)!!

        setSlide()
        setToolbar()
        dispList = RecordingsListAdapter.RecordingsListModal(context!!, recordingToolbar)
        dispList?.embedList(rootView as ViewGroup)
        dispList?.setSlideNum(slideNum)
        //This enables the "onStartedPlaybackOrRecording" to be invoked.
        dispList?.setParentFragment(this)
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

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        dispList?.updateRecordingList()
        dispList?.recyclerView?.adapter?.notifyDataSetChanged()
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        dispList?.stopAudio()
        multiRecordFrag.stopPlayback()
        //this is needed here to - when you are playing the reference audio and start recording
        //the new audio file pops up, and in the wrong format.
        dispList?.updateRecordingList()
    }

    private fun setSlide(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        multiRecordFrag.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.slide_phase, multiRecordFrag).commit()
    }

    private fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(false,false,false,false))
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
        recordingToolbar.stopToolbarMedia()
    }

    override fun onStoppedPlayback() {}

    override fun onStartedPlayback() {
        dispList?.stopAudio()
        recordingToolbar.stopToolbarMedia()
    }
}

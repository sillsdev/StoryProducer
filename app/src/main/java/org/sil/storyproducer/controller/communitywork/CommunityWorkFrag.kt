package org.sil.storyproducer.controller.communitywork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityWorkFrag : MultiRecordFrag() {

    override var recordingToolbar: RecordingToolbar = RecordingToolbar()
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_community_work, container, false)

        setPic(rootView!!.findViewById(R.id.fragment_image_view))
        setToolbar()
        dispList = RecordingsListAdapter.RecordingsListModal(requireContext(), recordingToolbar)
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


    override fun onResume() {
        super.onResume()

        addAndStartPopupMenus(slideNum)
    }

    private fun addAndStartPopupMenus(slideNumber: Int) {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        mPopupHelpUtils = PopupHelpUtils(this)

        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 75,
            R.string.help_community_phase_title, R.string.help_community_phase_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.seek_bar,
            82, 70,
            R.string.help_community_swipe_title, R.string.help_community_swipe_body) {
            Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER
        }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.fragment_reference_audio_button,
            80, 90,
            R.string.help_community_play_title, R.string.help_community_play_body) {
//                Workspace.activeStory.activityLogs.firstOrNull {
//                    it.phase.phaseType == PhaseType.COMMUNITY_WORK && it.description.contains("Draft Playback")  // TODO: LOCALIZATION: Temp string
//                } != null
                true    // Always show next for community play action in case there is not recorded audio for this slide
            }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.start_recording_button,
            50, 10,
            R.string.help_community_record_title, R.string.help_community_record_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.seek_bar,
            82, 70,
            R.string.help_community_continue_title, R.string.help_community_continue_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 75,
            R.string.help_community_revise_title, R.string.help_community_revise_body)
        mPopupHelpUtils?.addPopupHelpItem(
                R.id.toolbar,
                60, 75,
                R.string.help_community_nextphase_title, R.string.help_community_nextphase_body)

        mPopupHelpUtils?.showNextPopupHelp()

        (requireActivity() as PhaseBaseActivity).setBasePopupHelpUtils(mPopupHelpUtils!!)

    }
}

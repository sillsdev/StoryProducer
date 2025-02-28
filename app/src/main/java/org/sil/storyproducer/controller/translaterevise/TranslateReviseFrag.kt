package org.sil.storyproducer.controller.translaterevise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Translate + Revise phase.
 * This is where a user can translate the story slide by slide
 */
class TranslateReviseFrag : MultiRecordFrag() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        showHideReferenceAudioPlay()
    }

    private fun addAndStartPopupTutorials(slideNumber: Int) {

        if (mPopupHelpUtils != null) {
            mPopupHelpUtils?.dismissPopup()
            mPopupHelpUtils = null
        }

        if (slideNumber == 0) {
            mPopupHelpUtils = PopupHelpUtils(this, this.javaClass)

            mPopupHelpUtils?.addHtml5HelpItem(R.id.toolbar, "html5/RevisePhase/The Learn Phase2.html")

            mPopupHelpUtils?.addPopupHelpItem(
                R.id.toolbar,
                50, 90,
                R.string.help_translate_phase_title, R.string.help_translate_phase_body)
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.seek_bar,
                86, 70,
                R.string.help_translate_titleslide_title, R.string.help_translate_titleslide_body)
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.fragment_scripture_text,
                25, 2,
                R.string.help_translate_pick_title, R.string.help_translate_pick_body)
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.edit_text_view,
                20, 90,
                R.string.help_translate_enter_title, R.string.help_translate_enter_body) {
                    Workspace.activeSlide?.let { it.slideType == SlideType.FRONTCOVER && it.translatedContent.isNotEmpty() } ?: false
            }
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.start_recording_button,
                80, 10 ,
                R.string.help_translate_record_title, R.string.help_translate_record_body) {
                    Workspace.activeSlide?.let {
                        it.slideType == SlideType.FRONTCOVER && it.translateReviseAudioFiles.isNotEmpty() &&
                                !recordingToolbar.isRecording } ?: false
            }
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.seek_bar,
                    86, 90,
                R.string.help_translate_nextslide1_title, R.string.help_translate_nextslide1_body)

        } else if (Workspace.activeStory.slides[slideNumber].isNumberedPage()) {
            mPopupHelpUtils = PopupHelpUtils(this, this.javaClass, 1)   // always use slide 1
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.seek_bar,
                    82, 70,
                    R.string.help_translate_storyslide_title, R.string.help_translate_storyslide_body)
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.fragment_reference_audio_button,
                80, 90,
                R.string.help_translate_listen_title, R.string.help_translate_listen_body)
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.start_recording_button,
                80, 10,
                R.string.help_translate_record_slide_title, R.string.help_translate_record_slide_body) {
                    Workspace.activeSlide?.let {
                        it.slideType == SlideType.NUMBEREDPAGE && it.translateReviseAudioFiles.isNotEmpty() &&
                                !recordingToolbar.isRecording } ?: false
            }
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.play_recording_button,
                50, 10,
                R.string.help_translate_review_title, R.string.help_translate_review_body)
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.seek_bar,
                    82, 70,
                    R.string.help_translate_continue_title, R.string.help_translate_continue_body)
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.toolbar,
                    60, 75,
                    R.string.help_translate_finish_title, R.string.help_translate_finish_body)

        }

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.showNextPopupHelp()

        (requireActivity() as PhaseBaseActivity).setBasePopupHelpUtils(mPopupHelpUtils!!)

    }

    override fun onResume() {
        super.onResume()

        addAndStartPopupTutorials(slideNum)

        showHideReferenceAudioPlay()
    }

    private fun showHideReferenceAudioPlay() {
        val playButton = view?.findViewById<ImageButton>(R.id.fragment_reference_audio_button)
        val videoSeekBar = view?.findViewById<SeekBar>(R.id.videoSeekBar)
        if (Workspace.activeStory.slides[slideNum].slideType == SlideType.LOCALSONG) {
            playButton?.alpha = 0.5f
            playButton?.isEnabled = false
            videoSeekBar?.alpha = 0.5f
            videoSeekBar?.isEnabled = false
        } else {
            playButton?.alpha = 1.0f
            playButton?.isEnabled = true
            videoSeekBar?.alpha = 1.0f
            videoSeekBar?.isEnabled = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(null, rootView!!.findViewById(R.id.fragment_scripture_text))
        setReferenceText(rootView!!.findViewById(R.id.fragment_reference_text))

        showHideReferenceAudioPlay()

        return rootView
    }

}

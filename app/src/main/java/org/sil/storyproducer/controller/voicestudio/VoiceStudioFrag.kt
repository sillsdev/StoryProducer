package org.sil.storyproducer.controller.voicestudio

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar


class VoiceStudioFrag : MultiRecordFrag() {

    override var recordingToolbar: RecordingToolbar = VoiceStudioRecordingToolbar()
    private var slideText: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        checkAllMarked(requireContext())

        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false)

        setPic(rootView?.findViewById<View>(R.id.fragment_image_view) as ImageView)
        slideText = rootView?.findViewById(R.id.fragment_dramatization_edit_text)
        slideText?.setText(Workspace.activeStory.slides[slideNum].translatedContent, TextView.BufferType.EDITABLE)

        val lockTextImage = rootView?.findViewById<TextView>(R.id.lockScreenText)
        BaseActivity.setLockTextAndImage(requireContext(), lockTextImage)

        if (Workspace.activeStory.isApproved) {
            setToolbar()
            closeKeyboardOnTouch(rootView)
            rootView?.findViewById<View>(R.id.lock_overlay)?.visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView!!)
        }

        //Make the text bigger if it is the front Page.
        if(Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER){
            slideText?.setTextSize(COMPLEX_UNIT_DIP,24f)
            slideText?.hint = requireContext().getString(R.string.voice_studio_edit_title_text_hint)
        }

        setupCameraAndEditButton()

        return rootView
    }

    /**
     * This function serves to stop the audio streams from continuing after voicestudio has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        closeKeyboard(rootView)
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                closeKeyboard(rootView)
            }
        }
    }

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: [.closeKeyboard].
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     * the view will close the softkeyboard.
     */
    private fun closeKeyboardOnTouch(touchedView: View?) {
        touchedView?.setOnClickListener { closeKeyboard(touchedView) }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     */
    private fun closeKeyboard(viewToFocus: View?) {
        if (viewToFocus != null) {
            val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
        if(slideText!!.visibility == View.VISIBLE) {
            //Don't update with a press when in title and local credits slides.
            val newText = slideText!!.text.toString()
            if (newText != Workspace.activeStory.slides[slideNum].translatedContent) {
                Workspace.activeStory.slides[slideNum].translatedContent = newText
                setPic(rootView!!.findViewById(R.id.fragment_image_view))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        addAndStartPopupMenus(slideNum)
    }

    override fun onStoppedToolbarAppending() {
        super.onStoppedToolbarAppending()
        addAndStartPopupMenus(slideNum)
    }

    override fun onStartedToolbarAppending() {
        super.onStartedToolbarAppending()
        addAndStartPopupMenus(slideNum)
    }

    private fun addAndStartPopupMenus(slideNumber: Int) {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        if (!recordingToolbar.isAppendingOn) {

            mPopupHelpUtils = PopupHelpUtils(this, 0)    // the main help for this fragment

            mPopupHelpUtils?.addPopupHelpItem(
                R.id.toolbar,
                50, 75,
                R.string.help_voice_phase_title, R.string.help_voice_phase_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.fragment_reference_audio_button,
                80, 90,
                R.string.help_voice_listen_title, R.string.help_voice_listen_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.start_recording_button,
                80, 10,
                R.string.help_voice_record_title, R.string.help_voice_record_body) {
                Workspace.activeSlide?.let { it.chosenVoiceStudioFile.isNotEmpty() } ?: false
            }
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.play_recording_button,
                    50, 10,
                    R.string.help_voice_replay_title, R.string.help_voice_replay_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.seek_bar,
                86, 70,
                R.string.help_voice_continue_title, R.string.help_voice_continue_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                    R.id.seek_bar,
                    -1, -1,
                    R.string.help_voice_final_title, R.string.help_voice_final_body
            )

        } else {

            mPopupHelpUtils = PopupHelpUtils(this, 1)    // the alternate help for this fragment

            mPopupHelpUtils?.addPopupHelpItem(
                R.id.phase_frame,
                -1, -1,
                R.string.help_voice_paused_title, R.string.help_voice_paused_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.play_recording_button,
                50, 10,
                R.string.help_voice_pausedreplay_title, R.string.help_voice_pausedreplay_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.fragment_reference_audio_button,
                80, 90,
                R.string.help_voice_pausedlisten_title, R.string.help_voice_pausedlisten_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.start_recording_button,
                80, 10,
                R.string.help_voice_pausedrecord_title, R.string.help_voice_pausedrecord_body
            )
            mPopupHelpUtils?.addPopupHelpItem(
                R.id.finish_recording_button,
                20, 10,
                R.string.help_voice_pausedstop_title, R.string.help_voice_pausedstop_body
            )
        }

            // only update help popups display when not recording
        mPopupHelpUtils?.showNextPopupHelp()

        (requireActivity() as PhaseBaseActivity).setBasePopupHelpUtils(mPopupHelpUtils!!)

    }
}

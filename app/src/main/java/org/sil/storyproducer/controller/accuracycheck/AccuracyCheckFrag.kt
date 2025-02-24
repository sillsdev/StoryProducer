package org.sil.storyproducer.controller.accuracycheck

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.logging.LogListAdapter
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
class AccuracyCheckFrag : SlidePhaseFrag() {

    var logDialog: AlertDialog? = null
    var greenCheckmark: VectorDrawableCompat ?= null
    var grayCheckmark: VectorDrawableCompat ?= null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        greenCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)
        grayCheckmark = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)

        checkAllMarked(requireContext())

        return inflater.inflate(R.layout.fragment_accuracy_check, container, false)?.apply {
            this@AccuracyCheckFrag.rootView = this
            setPic(findViewById<View>(R.id.fragment_image_view) as ImageView)
            setScriptureText(rootView, rootView!!.findViewById(R.id.fragment_scripture_text))
            setReferenceText(rootView!!.findViewById(R.id.fragment_reference_text))
            setCheckmarkButton(findViewById<View>(R.id.concheck_checkmark_button) as ImageButton)
            setLogsButton(findViewById<View>(R.id.concheck_logs_button) as ImageButton)
        }
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     * @param isVisibleToUser
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                referenceAudioPlayer.stopAudio()
            }
        }
    }


    /**
     * Sets on click listener for consultant to check off the slide and approve
     * @param button the check button
     */
    private fun setCheckmarkButton(button: ImageButton) {
        if (Workspace.activeStory.slides[slideNum].isChecked) {
            button.background = greenCheckmark
        } else {
            button.background = grayCheckmark
        }
        button.setOnClickListener(View.OnClickListener {
            if (Workspace.activeStory.isApproved) {
                Toast.makeText(context, "Story already approved", Toast.LENGTH_SHORT).show()    // TODO: Add string to strings.xml
                return@OnClickListener
            }
            if (Workspace.activeStory.slides[slideNum].isChecked) {
                button.background = grayCheckmark
                Workspace.activeStory.slides[slideNum].isChecked = false
            } else {
                button.background = greenCheckmark
                Workspace.activeStory.slides[slideNum].isChecked = true
                if (checkAllMarked(requireContext())) {
                    // SP645 - BW 06/15/2022 new Affirm Accuracy Check dialog for SIL Story Producer
                    // Other forks of story producer may want to keep the ConsultantPassword dialog instead.
                    // We could use a Feature Flags library to enable/choose the desired approach.
                    // for now, comment out the other
                    //if (xxx)
                        showAffirmAccuracyCheckDialog()
                    //else
                    //    showConsultantPasswordDialog()
                }
            }
        })
    }

    /**
     * Set an on click listener to launch the interface to view the logs for that slide
     * @param button the logs button
     */
    private fun setLogsButton(button: ImageButton) {
        //TODO: use non-deprecated method; currently used to support older devices
        button.background = VectorDrawableCompat.create(resources, R.drawable.ic_logs_blue, null)
        button.setOnClickListener {
            makeLogView()
            logDialog?.show()
        }
    }

    private fun makeLogView() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val linf = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogLayout = linf.inflate(R.layout.activity_log_view, null)

        val listView = dialogLayout!!.findViewById<ListView>(R.id.log_list_view)
        val lla = LogListAdapter(requireContext(), slideNum)
        listView.adapter = lla
        val tb = dialogLayout.findViewById<Toolbar>(R.id.toolbar2)
        //Note that user-facing slide number is 1-based while it is 0-based in code.
        tb.title = requireContext().getString(R.string.logging_slide_log_view_title, slideNum)
        val exit = dialogLayout.findViewById<ImageButton>(R.id.exitButton)
        val learnCB = dialogLayout.findViewById<CheckBox>(R.id.LearnCheckBox)
        val draftCB = dialogLayout.findViewById<CheckBox>(R.id.DraftCheckBox)
        val comChkCB = dialogLayout.findViewById<CheckBox>(R.id.CommunityCheckCheckBox)
        learnCB.setOnCheckedChangeListener { _, checked -> lla.updateList(checked, draftCB.isChecked, comChkCB.isChecked) }
        draftCB.setOnCheckedChangeListener { _, checked -> lla.updateList(learnCB.isChecked, checked, comChkCB.isChecked) }
        comChkCB.setOnCheckedChangeListener { _, checked -> lla.updateList(learnCB.isChecked, draftCB.isChecked, checked) }
        alertDialog.setView(dialogLayout)
        logDialog = alertDialog.create()
        exit.setOnClickListener {
            logDialog?.dismiss()
        }
    }


    /**
     * Launches a dialog for the consultant to enter a password once all slides approved
     */
    private fun showConsultantPasswordDialog() {
        val password = EditText(context)
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        password.id = org.sil.storyproducer.R.id.password_text_field;

        // Programmatically set layout properties for edit text field
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        // Apply layout properties
        password.layoutParams = params
        val passwordDialog = AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.consultant_password_title))
                .setMessage(getString(R.string.consultant_password_message))
                .setView(password)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.submit), null)
                .create()
        // This is set to dismiss the keyboard manually on dialog dismiss
        passwordDialog.setOnDismissListener { toggleKeyboard(false, view) }

        // This manually sets the submit button listener so that the dialog doesn't always submit
        // If the password is incorrect, we want to stay on the dialog and give an error message
        passwordDialog.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val passwordText = password.text.toString()
                if (passwordText.contentEquals(PASSWORD)) {
                    saveConsultantApproval()
                    dialog.dismiss()
                    launchDramatizationPhase()
                } else {
                    password.error = getString(R.string.consultant_incorrect_password_message)
                }
            }
        }

        passwordDialog.show()
        toggleKeyboard(true, password)
    }

    /**
     * Launches a dialog for affirming the accuracy check once all slides 'approved'
     */
    // SP645 - BW 06/15/2022 new Affirm Accuracy Check dialog for SIL Story Producer
    private fun showAffirmAccuracyCheckDialog() {
        val affirmDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                .setTitle(getString(R.string.affirm_accuracy_check_title))
                .setMessage(getString(R.string.affirm_accuracy_check_message))
                .setPositiveButton(getString(R.string.yes), null)
                .setNegativeButton(getString(R.string.no), null)
                .setNeutralButton(getString(R.string.affirm_accuracy_check_NotaBibleStory), null)
                .create()

        // This sets the three button listeners
        affirmDialog.setOnShowListener { dialog ->
            // Sets the Yes button listener so that we save 'approval', dismiss the dialog,
            // and continue to the Voice Studio phase
            val YesButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            YesButton.setOnClickListener {
                saveConsultantApproval()
                dialog.dismiss()
                launchDramatizationPhase()
            }
            //Sets the No button listener so that the user is prompted to do proper accuracy check
            val NoButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            NoButton.setOnClickListener {
                dialog.dismiss()
                showRequestAccuracyCheckDialog()
            }
            //Sets the Neutral button listener -
            // affirmation is not applicable;  save 'approval' and proceed to voice studio phase
            val NeutButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            NeutButton.setOnClickListener {
                saveConsultantApproval()
                dialog.dismiss()
                launchDramatizationPhase()
            }
         }

        affirmDialog.show()
    }

    /**
     * Launches a dialog for requesting a Bible accuracy check, when user did not affirm it
     */
    private fun showRequestAccuracyCheckDialog() {
        val requestAccCkDialog = AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.request_accuracy_check_message))
                .setPositiveButton(getString(R.string.ok), null)
                .create()
        requestAccCkDialog.show()
    }

    /**
     * Updates the shared preference file to mark the story as approved
     */
    private fun saveConsultantApproval() {
        Workspace.activeStory.isApproved = true
    }

    /**
     * Launches the voicestudio phase for the story and starts back at first slide
     * TODO: moving back to first slide is currently broken
     */
    private fun launchDramatizationPhase() {
        Toast.makeText(context, R.string.voice_studio_move_time, Toast.LENGTH_LONG).show()
        //Move to voicestudio, slide 0.
        Workspace.activeSlideNum = 0
        (activity as PhaseBaseActivity).jumpToPhase(Phase(PhaseType.VOICE_STUDIO))
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private fun toggleKeyboard(showKeyBoard: Boolean, aView: View?) {
        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (showKeyBoard) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else {
            imm.hideSoftInputFromWindow(aView!!.windowToken, 0)
        }
    }

    companion object {
        val CONSULTANT_PREFS = "Consultant_Checks"
        val IS_CONSULTANT_APPROVED = "isApproved"
        private val PASSWORD = "appr00ved"
    }


    override fun onResume() {
        super.onResume()

        addAndStartPopupTutorials(slideNum)

    }
    private fun addAndStartPopupTutorials(slideNumber: Int) {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        mPopupHelpUtils = PopupHelpUtils(this, this.javaClass)

        mPopupHelpUtils?.addHtml5HelpItem(R.id.toolbar, "html5/AccuracyPhase/The Learn Phase2.html")

        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 75,
            R.string.help_accuracy_phase_title, R.string.help_accuracy_phase_body)
        mPopupHelpUtils?.addPopupHelpItem(
                R.id.seek_bar,
                82, 70,
                R.string.help_community_swipe_title, R.string.help_community_swipe_body) {
            Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER
        }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.concheck_logs_button,
            60, 5,
            R.string.help_accuracy_history_title, R.string.help_accuracy_history_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.fragment_reference_audio_button,
            80, 90,
            R.string.help_accuracy_play_title, R.string.help_accuracy_play_body)
        mPopupHelpUtils?.addPopupHelpItem(
                R.id.toolbar,
                50, 75,
                R.string.help_accuracy_revise_title, R.string.help_accuracy_revise_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.concheck_checkmark_button,
            40, 5,
            R.string.help_accuracy_confirm_title, R.string.help_accuracy_confirm_body) {
                Workspace.activeSlide?.let { it.slideType in arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE, SlideType.LOCALSONG)
                        && it.isChecked} ?: false   // enable Next when active slide is checked
        }
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.seek_bar,
            82, 70,
            R.string.help_accuracy_continue_title, R.string.help_accuracy_continue_body)

        mPopupHelpUtils?.showNextPopupHelp()

        (requireActivity() as PhaseBaseActivity).setBasePopupHelpUtils(mPopupHelpUtils!!)

    }
}

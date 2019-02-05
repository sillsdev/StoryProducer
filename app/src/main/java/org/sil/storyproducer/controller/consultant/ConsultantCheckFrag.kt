package org.sil.storyproducer.controller.consultant

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.logging.LogView
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
class ConsultantCheckFrag : SlidePhaseFrag() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_consultant_check, container, false)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)

        setScriptureText(rootView!!.findViewById<View>(R.id.fragment_scripture_text) as TextView)
        setReferenceText(rootView!!.findViewById<View>(R.id.fragment_reference_text) as TextView)
        setCheckmarkButton(rootView!!.findViewById<View>(R.id.concheck_checkmark_button) as ImageButton)
        setLogsButton(rootView!!.findViewById<View>(R.id.concheck_logs_button) as ImageButton)

        return rootView
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
        //TODO replace T/f with storing MD5 or SHA1 of the draft audio.
        if (Workspace.activeStory.slides[slideNum].isChecked) {
            //TODO: use non-deprecated method; currently used to support older devices
            button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)
        } else {
            //TODO: use non-deprecated method; currently used to support older devices
            button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)
        }
        button.setOnClickListener(View.OnClickListener {
            if (Workspace.activeStory.isApproved) {
                Toast.makeText(context, "Story already approved", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (Workspace.activeStory.slides[slideNum].isChecked) {
                button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_gray, null)
                Workspace.activeStory.slides[slideNum].isChecked = false
            } else {
                button.background = VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null)
                Workspace.activeStory.slides[slideNum].isChecked = true
                if (checkAllMarked()) {
                    showConsultantPasswordDialog()
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
        button.setOnClickListener { LogView.makeModal(context) }
    }

    /**
     * Checks each slide of the story to see if all slides have been approved
     * @return true if all approved, otherwise false
     */
    private fun checkAllMarked(): Boolean {
        //dont check the last slide, it's the copyright.
        val numStorySlides = Workspace.activeStory.slides.size - 1
        for (i in 0 until numStorySlides) {
            if (!Workspace.activeStory.slides[i].isChecked) {
                return false
            }
        }
        return true
    }

    /**
     * Launches a dialog for the consultant to enter a password once all slides approved
     */
    private fun showConsultantPasswordDialog() {
        val password = EditText(context)
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Programmatically set layout properties for edit text field
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        // Apply layout properties
        password.layoutParams = params
        val passwordDialog = AlertDialog.Builder(context)
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
     * Updates the shared preference file to mark the story as approved
     */
    private fun saveConsultantApproval() {
        Workspace.activeStory.isApproved = true
    }

    /**
     * Launches the dramatization phase for the story and starts back at first slide
     * TODO: moving back to first slide is currently broken
     */
    private fun launchDramatizationPhase() {
        Toast.makeText(context, "Congrats!", Toast.LENGTH_SHORT).show()
        //Move to dramatization, slide 0.
        Workspace.activeSlideNum = 0
        (activity as PhaseBaseActivity).jumpToPhase(Phase(PhaseType.DRAMATIZATION))
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

}

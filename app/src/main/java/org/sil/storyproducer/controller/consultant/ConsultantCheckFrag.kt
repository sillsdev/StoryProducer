package org.sil.storyproducer.controller.consultant

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.InputType
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.logging.LogView
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.SlideText
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.FileSystem
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.file.TextFiles

import java.io.File

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
class ConsultantCheckFrag : Fragment() {
    private var storyName: String? = null
    private var slidePosition: Int = 0
    private var rootView: View? = null
    private var isChecked: Boolean = false
    private var draftPlayer: AudioPlayer? = null
    private var slideText: SlideText? = null
    private var slideTextView: TextView? = null
    private var draftAudioExists: Boolean = false
    private var draftAudioPaused: Boolean = false
    private var draftPlaybackButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passedArgs = this.arguments
        slidePosition = passedArgs.getInt(SLIDE_NUM)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        rootView = inflater!!.inflate(R.layout.fragment_consultant_check, container, false)
        draftPlaybackButton = rootView!!.findViewById(R.id.concheck_draft_playback_button)
        storyName = StoryState.getStoryName()
        slideText = TextFiles.getSlideText(storyName, slidePosition)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_concheck_image_view) as ImageView, slidePosition)
        setScriptureText(rootView!!.findViewById<View>(R.id.fragment_concheck_scripture_text) as TextView)
        setReferenceText(rootView!!.findViewById<View>(R.id.fragment_concheck_reference_text) as TextView)
        setDraftPlaybackButton(rootView!!.findViewById<View>(R.id.concheck_draft_playback_button) as ImageButton)
        setCheckmarkButton(rootView!!.findViewById<View>(R.id.concheck_checkmark_button) as ImageButton)
        setLogsButton(rootView!!.findViewById<View>(R.id.concheck_logs_button) as ImageButton)
        slideTextView = rootView!!.findViewById(R.id.slide_number_text)
        slideTextView!!.text = slidePosition.toString() + ""

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        val item = menu!!.getItem(0)
        item.setIcon(R.drawable.ic_concheck)
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
                draftPlayer!!.stopAudio()
                draftPlaybackButton!!.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_play_blue, null))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        draftPlayer = AudioPlayer()
        val draftFile = AudioFiles.getDraft(storyName!!, slidePosition)
        if (draftFile.exists()) {
            draftAudioExists = true
            //FIXME
            //draftPlayer.setSource(draftFile.getPath());
        } else {
            draftAudioExists = false
        }
        draftAudioPaused = false
        draftPlayer!!.onPlayBackStop(MediaPlayer.OnCompletionListener {
            //TODO: use non-deprecated method; currently used to support older devices
            draftPlaybackButton!!.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_play_blue, null))
        })
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        draftPlayer!!.stopAudio()
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    override fun onStop() {
        super.onStop()
        draftPlayer!!.stopAudio()
        draftPlayer!!.release()
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private fun setUiColors() {
        if (slidePosition == 0) {
            var rl = rootView!!.findViewById<RelativeLayout>(R.id.concheck_relative_layout)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            rl = rootView!!.findViewById(R.id.concheck_button_layout)
            rl.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            var tv = rootView!!.findViewById<TextView>(R.id.fragment_concheck_scripture_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
            tv = rootView!!.findViewById(R.id.fragment_concheck_reference_text)
            tv.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDark))
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private fun setPic(slideImage: ImageView, slideNum: Int) {
        var slidePicture: Bitmap? = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum)

        if (slidePicture == null) {
            Snackbar.make(rootView!!, "Could Not Find Picture", Snackbar.LENGTH_SHORT).show()
        }

        //Get the height of the phone.
        val phoneProperties = context.resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture!!, height)

        //Set the height of the image view
        slideImage.layoutParams.height = height
        slideImage.requestLayout()

        slideImage.setImageBitmap(slidePicture)
    }


    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private fun setScriptureText(textView: TextView) {

        textView.text = slideText!!.content
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private fun setReferenceText(textView: TextView) {
        val titleNamePriority = arrayOf(slideText!!.reference, slideText!!.subtitle, slideText!!.title)

        for (title in titleNamePriority) {
            if (title != null && title != "") {
                textView.text = title
                return
            }
        }
    }

    /**
     * This function sets the draft playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param button the ImageButton view handler to set the onclicklistener to
     */
    private fun setDraftPlaybackButton(button: ImageButton) {
        //TODO: use non-deprecated method; currently used to support older devices
        button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_play_blue, null))
        button.setOnClickListener {
            //stop other playback streams.
            val wasPlaying = draftPlayer!!.isAudioPlaying
            if (draftAudioExists && !wasPlaying) {
                if (draftAudioPaused) {
                    draftPlayer!!.resumeAudio()
                    draftAudioPaused = false
                } else {
                    draftPlayer!!.playAudio()
                }
                //TODO: use non-deprecated method; currently used to support older devices
                button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_pause_blue, null))
                Toast.makeText(context, "Playing Draft Audio", Toast.LENGTH_SHORT).show()
            } else if (wasPlaying) {
                draftPlayer!!.pauseAudio()
                draftAudioPaused = true
                //TODO: use non-deprecated method; currently used to support older devices
                button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_play_blue, null))
            } else {
                Toast.makeText(context, "No Draft Audio Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets on click listener for consultant to check off the slide and approve
     * @param button the check button
     */
    private fun setCheckmarkButton(button: ImageButton) {
        val prefs = activity.getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val prefsKeyString = storyName + slidePosition + IS_CHECKED
        isChecked = prefs.getBoolean(prefsKeyString, false)
        if (isChecked) {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null))
        } else {
            //TODO: use non-deprecated method; currently used to support older devices
            button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_red, null))
        }
        button.setOnClickListener(View.OnClickListener {
            val isApproved = prefs.getBoolean(storyName!! + IS_CONSULTANT_APPROVED, false)
            if (isApproved) {
                Toast.makeText(context, "Story already approved", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (isChecked) {
                //TODO: use non-deprecated method; currently used to support older devices
                button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_red, null))
                isChecked = false
                prefsEditor.putBoolean(prefsKeyString, false)
                prefsEditor.apply()
            } else {
                //TODO: use non-deprecated method; currently used to support older devices
                button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_checkmark_green, null))
                isChecked = true
                prefsEditor.putBoolean(prefsKeyString, true)
                prefsEditor.commit()
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
        button.setBackgroundDrawable(VectorDrawableCompat.create(resources, R.drawable.ic_logs_blue, null))
        button.setOnClickListener { LogView.makeModal(context) }
    }

    /**
     * Checks each slide of the story to see if all slides have been approved
     * @return true if all approved, otherwise false
     */
    private fun checkAllMarked(): Boolean {
        var marked: Boolean
        val prefs = activity.getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val numStorySlides = FileSystem.getContentSlideAmount(storyName)
        for (i in 0 until numStorySlides) {
            marked = prefs.getBoolean(storyName + i + IS_CHECKED, false)
            if (!marked) {
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
        val dialog = AlertDialog.Builder(context)
                .setTitle(getString(R.string.consultant_password_title))
                .setMessage(getString(R.string.consultant_password_message))
                .setView(password)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.submit), null)
                .create()
        // This is set to dismiss the keyboard manually on dialog dismiss
        dialog.setOnDismissListener { toggleKeyboard(false, view) }

        // This manually sets the submit button listener so that the dialog doesn't always submit
        // If the password is incorrect, we want to stay on the dialog and give an error message
        dialog.setOnShowListener { dialog ->
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

        dialog.show()
        toggleKeyboard(true, password)
    }

    /**
     * Updates the shared preference file to mark the story as approved
     */
    private fun saveConsultantApproval() {
        val prefsEditor = activity.getSharedPreferences(CONSULTANT_PREFS, Context.MODE_PRIVATE).edit()
        prefsEditor.putBoolean(storyName!! + IS_CONSULTANT_APPROVED, true)
        prefsEditor.apply()
    }

    /**
     * Launches the dramatization phase for the story and starts back at first slide
     * TODO: moving back to first slide is currently broken
     */
    private fun launchDramatizationPhase() {
        Toast.makeText(context, "Congrats!", Toast.LENGTH_SHORT).show()
        val dramatizationPhaseIndex = 4
        val phases = StoryState.getPhases()
        StoryState.setCurrentPhase(phases[dramatizationPhaseIndex])
        val intent = Intent(context, StoryState.getCurrentPhase().getTheClass())
        intent.putExtra(SLIDE_NUM, 0)
        activity.startActivity(intent)
    }

    /**
     * This function toggles the soft input keyboard. Allowing the user to have the keyboard
     * to open or close seamlessly alongside the rest UI.
     * @param showKeyBoard The boolean to be passed in to determine if the keyboard show be shown.
     * @param aView The view associated with the soft input keyboard.
     */
    private fun toggleKeyboard(showKeyBoard: Boolean, aView: View?) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (showKeyBoard) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else {
            imm.hideSoftInputFromWindow(aView!!.windowToken, 0)
        }
    }

    companion object {

        val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
        val CONSULTANT_PREFS = "Consultant_Checks"
        val IS_CONSULTANT_APPROVED = "isApproved"
        private val IS_CHECKED = "isChecked"
        private val PASSWORD = "appr00ved"
    }

}

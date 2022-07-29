package org.tyndalebt.spadv.controller.export

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.phase.PhaseBaseActivity
import org.tyndalebt.spadv.model.VIDEO_DIR
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.tools.file.workspaceRelPathExists
import org.tyndalebt.spadv.tools.media.story.AutoStoryMaker
import org.tyndalebt.spadv.tools.stripForFilename


class FinalizeActivity : PhaseBaseActivity() {

    private lateinit var mEditTextTitle: EditText
    private lateinit var mLayoutConfiguration: View
    private lateinit var mLayoutCancel: View
    private lateinit var mCheckboxSoundtrack: CheckBox
    private lateinit var mCheckboxPictures: CheckBox
    private lateinit var mCheckboxText: CheckBox
    private lateinit var mCheckboxKBFX: CheckBox
    private lateinit var mCheckboxSong: CheckBox
    private lateinit var mButtonStart: Button
    private lateinit var mButtonCancel: Button
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mButtonCredits: Button

    private val mOutputPath: String get() {
        val num = if(Workspace.activeStory.titleNumber != "") "${Workspace.activeStory.titleNumber}_" else {""}
        val name = mEditTextTitle.text.toString()
        var ethno = Workspace.registration.getString("ethnologue", "")
        if(ethno != "") ethno = "${ethno}_"
        val fx = if(mCheckboxSoundtrack.isChecked) {"Fx"} else {""}
        val px = if(mCheckboxPictures.isChecked) {"Px"} else {""}
        val mv = if(mCheckboxKBFX.isChecked) {"Mv"} else {""}
        val tx = if(mCheckboxText.isChecked) {"Tx"} else {""}
        val sg = if(mCheckboxSong.isChecked) {"Sg"} else {""}
        return "$num${name}_$ethno$fx$px$mv$tx$sg.mp4"
    }

    private var mProgressUpdater: Thread? = null

    private val PROGRESS_UPDATER = Runnable {
        var isDone = false
        while (!isDone) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                //If progress updater is interrupted, just stop.
                return@Runnable
            }

            var progress: Double
            synchronized(storyMakerLock) {
                //Stop if storyMaker was cancelled by someone else.
                if (storyMaker == null) {
                    updateProgress(0)
                    return@Runnable
                }

                progress = storyMaker!!.progress
                isDone = storyMaker!!.isDone
            }
            updateProgress((progress * PROGRESS_MAX).toInt())
        }
        val isSuccess = storyMaker!!.isSuccess

        runOnUiThread {
            stopExport()
            if(isSuccess)
                Toast.makeText(baseContext, "Video created!", Toast.LENGTH_LONG).show()
            else
                Toast.makeText(baseContext, "Error!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Unlock the start/cancel buttons after a brief time period.
     */
    private val BUTTON_UNLOCKER = Runnable {
        try {
            Thread.sleep(BUTTON_LOCK_DURATION_MS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            buttonLocked = false
            runOnUiThread {
                mButtonStart.isEnabled = true
                mButtonCancel.isEnabled = true
            }
        }
    }

    private var storyMaker: AutoStoryMaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finalize)
        setupViews()
        invalidateOptionsMenu()
        if (Workspace.activeStory.isApproved) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.layout_export_configuration)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        toggleVisibleElements()
        watchProgress()

        //attach the listeners after everything else is setup.
        mCheckboxPictures.setOnCheckedChangeListener { _, _ -> toggleVisibleElements(mCheckboxPictures) }
        mCheckboxKBFX.setOnCheckedChangeListener { _, _ -> toggleVisibleElements(mCheckboxKBFX) }
        mCheckboxText.setOnCheckedChangeListener { _, _ -> toggleVisibleElements(mCheckboxText) }
        mCheckboxSong.setOnCheckedChangeListener { _, _ -> toggleVisibleElements(mCheckboxSong) }
    }

    override fun onPause() {
        mProgressUpdater!!.interrupt()
        savePreferences()

        super.onPause()
    }

    /**
     * Remove focus from EditText when tapping outside. See http://stackoverflow.com/a/28939113/4639640
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    private fun setupViews() {
        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.

        mEditTextTitle = findViewById(R.id.editText_export_title)
        mEditTextTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val temp = s.toString().stripForFilename()
                if (temp != s.toString()) {
                    s!!.replace(0, s.length, temp)
                }

                // Update check mark icon
                val checkMarkIcon = findViewById<View>(R.id.checkmark_file_name) as ImageView
                if(temp.isEmpty()) {
                    checkMarkIcon.setImageResource(R.drawable.ic_check_circle_grey_outline_24)
                } else {
                    checkMarkIcon.setImageResource(R.drawable.ic_check_circle_outline_24)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        mLayoutConfiguration = findViewById(R.id.layout_export_configuration)
        mLayoutCancel = findViewById(R.id.layout_cancel)

        mCheckboxSoundtrack = findViewById(R.id.checkbox_export_soundtrack)
        mCheckboxPictures = findViewById(R.id.checkbox_export_pictures)
        mCheckboxKBFX = findViewById(R.id.checkbox_export_KBFX)
        mCheckboxText = findViewById(R.id.checkbox_export_text)
        mCheckboxSong = findViewById(R.id.checkbox_export_song)

        mButtonStart = findViewById(R.id.button_export_start)
        mButtonCancel = findViewById(R.id.button_export_cancel)
        mButtonCredits = findViewById(R.id.button_local_credits)
        setOnClickListeners()

        mProgressBar = findViewById(R.id.progress_bar_export)
        mProgressBar.max = PROGRESS_MAX
        mProgressBar.progress = 0

        // Safety check to ensure that the credits exist
        if(Workspace.activeStory.localCredits.isEmpty()) {
            Workspace.activeStory.localCredits = getString(R.string.LC_starting_text)
        }

        // Update the check mark if the file name has previously been changed
        if(mEditTextTitle.text.toString().isNotEmpty()) {
            val checkMarkIcon = findViewById<View>(R.id.checkmark_file_name) as ImageView
            checkMarkIcon.setImageResource(R.drawable.ic_check_circle_outline_24)
        }

        // Update the check mark if the credits have previously been changed
        if(Workspace.isLocalCreditsChanged(this)) {
            val checkMarkIcon = findViewById<View>(R.id.checkmark_local_credits) as ImageView
            checkMarkIcon.setImageResource(R.drawable.ic_check_circle_outline_24)
        }
    }

    /**
     * Setup listeners for start/cancel.
     */
    private fun setOnClickListeners() {

        mButtonStart.setOnClickListener {
            if (!buttonLocked) {
                tryStartExport()
            }
            lockButtons()
        }

        mButtonCancel.setOnClickListener {
            if (!buttonLocked) {
                stopExport()
            }
            lockButtons()
        }

        mButtonCredits.setOnClickListener{
            if(!buttonLocked) {
                val editText = EditText(this)
                editText.id = R.id.edit_text_input

                // Programmatically set layout properties for edit text field
                val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                // Apply layout properties
                editText.layoutParams = params
                editText.minLines = 5
                editText.setText(Workspace.activeStory.localCredits)

                val dialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.enter_text))
                        .setView(editText)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(getString(R.string.save)) { _, _ ->
                            val checkMarkIcon = findViewById<View>(R.id.checkmark_local_credits) as ImageView
                            if(editText.text.toString().isNotEmpty()) {
                                Workspace.activeStory.localCredits = editText.text.toString()

                                if(Workspace.isLocalCreditsChanged(this)) {
                                    Toast.makeText(this, getString(R.string.local_credits_changed), Toast.LENGTH_LONG)
                                    checkMarkIcon.setImageResource(R.drawable.ic_check_circle_outline_24)
                                }
                            }

                            if(!Workspace.isLocalCreditsChanged(this)) {
                                Toast.makeText(this, getString(R.string.local_credits_unchanged), Toast.LENGTH_LONG)
                                checkMarkIcon.setImageResource(R.drawable.ic_check_circle_grey_outline_24)
                            }

                            // Hide the Keyboard Programmatically
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                        }.create()

                dialog.show()
            }
            lockButtons()
        }
    }

    /**
     * Ensure the proper elements are visible based on checkbox dependencies and whether export process is going.
     */
    private fun toggleVisibleElements(currentCheckbox: CheckBox? = null) {
        var visibilityPreExport = View.VISIBLE
        var visibilityWhileExport = View.GONE
        synchronized(storyMakerLock) {
            if (storyMaker != null) {

                visibilityPreExport = View.GONE
                visibilityWhileExport = View.VISIBLE
            }
        }

        mLayoutConfiguration.visibility = visibilityPreExport
        mLayoutCancel.visibility = visibilityWhileExport
        mButtonStart.visibility = visibilityPreExport

        if (mCheckboxPictures.isChecked) {
            mCheckboxKBFX.visibility = View.VISIBLE
            mCheckboxText.visibility = View.VISIBLE
        }else{
            mCheckboxKBFX.visibility = View.GONE
            mCheckboxKBFX.isChecked = false
            mCheckboxText.visibility = View.GONE
            mCheckboxText.isChecked = false
        }

        // 4/01/2022 - DKH, Issue 0R11: Allow text with movement during video creation
        // With the implementation of SP456 (Add grey rectangle to backdrop text "sub titles"),
        // the text "sub titles" are now readable during picture movement.  Remove the restriction
        // that would not allow text "sub titles" to be displayed during picture movement.
        /*
        if (mCheckboxText.isChecked && mCheckboxKBFX.isChecked) {
            if(currentCheckbox == mCheckboxText){
                 mCheckboxKBFX.isChecked = false
            }else{
                 mCheckboxText.isChecked = false
            }
        }
        */
        // Check if there is a song to play
        if (mCheckboxSong.isChecked && (Workspace.getSongFilename() == "")){
            // you have to have a song to include it!
            Toast.makeText(this, getString(R.string.export_local_song_unrecorded), Toast.LENGTH_SHORT).show()
            mCheckboxSong.isChecked = false
        }
    }

    /**
     * Save current configuration options to shared preferences.
     */
    private fun savePreferences() {
        val editor = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()

        editor.putBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_PICTURES, mCheckboxPictures.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_TEXT, mCheckboxText.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_KBFX, mCheckboxKBFX.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_SONG, mCheckboxSong.isChecked)

        editor.putString("$PREF_KEY_SHORT_NAME ${Workspace.activeStory.shortTitle}", mEditTextTitle.text.toString())

        editor.apply()
    }

    /**
     * Load configuration options from shared preferences.
     */
    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

        mCheckboxSoundtrack.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, true)
        mCheckboxPictures.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_PICTURES, true)
        mCheckboxText.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_TEXT, false)
        mCheckboxKBFX.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_KBFX, true)
        mCheckboxSong.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_SONG, true)
        mEditTextTitle.setText(prefs.getString("$PREF_KEY_SHORT_NAME ${Workspace.activeStory.shortTitle}", ""))

        // Update the check mark if the file name has previously been changed
        if(mEditTextTitle.text.toString().isNotEmpty()) {
            val checkMarkIcon = findViewById<View>(R.id.checkmark_file_name) as ImageView
            checkMarkIcon.setImageResource(R.drawable.ic_check_circle_outline_24)
        }
    }

    private fun tryStartExport() {
        // If the credits are unchanged, don't make the video.
        if(!Workspace.isLocalCreditsChanged(this)){
            Toast.makeText(this, this.resources.getText(
                    R.string.export_local_credits_unchanged), Toast.LENGTH_LONG).show()
            return
        }

        // If there is no title, don't make video
        if(mEditTextTitle.text.toString() == ""){
            Toast.makeText(this, this.resources.getText(
                    R.string.export_no_filename), Toast.LENGTH_LONG).show()
            return
        }

        // Else, check if the file already exists...
        if (workspaceRelPathExists(this, "$VIDEO_DIR/$mOutputPath")) {
            val dialog = android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.export_location_exists_title))
                    .setMessage(getString(R.string.export_location_exists_message))
                    .setNegativeButton(getString(R.string.no), null)
                    .setPositiveButton(getString(R.string.yes)) { _, _ -> startExport() }.create()

            dialog.show()
        } else {
            startExport()
        }
    }

    private fun startExport() {
        savePreferences()
        synchronized(storyMakerLock) {
            storyMaker = AutoStoryMaker(this)

            storyMaker!!.mIncludeBackgroundMusic = mCheckboxSoundtrack.isChecked
            storyMaker!!.mIncludePictures = mCheckboxPictures.isChecked
            storyMaker!!.mIncludeText = mCheckboxText.isChecked
            storyMaker!!.mIncludeKBFX = mCheckboxKBFX.isChecked
            storyMaker!!.mIncludeSong = mCheckboxSong.isChecked

            storyMaker!!.videoRelPath = mOutputPath
        }

        storyMaker!!.start()
        watchProgress()
    }

    private fun stopExport() {
        synchronized(storyMakerLock) {
            if (storyMaker != null) {
                storyMaker!!.close()
                storyMaker = null
            }
        }
        //update the list view
        toggleVisibleElements()
    }

    private fun watchProgress() {
        mProgressUpdater = Thread(PROGRESS_UPDATER)
        mProgressUpdater!!.start()
        toggleVisibleElements()
    }

    private fun updateProgress(progress: Int) {
        runOnUiThread { mProgressBar.progress = progress }
    }

    /**
     * Lock the start/cancel buttons temporarily to give the StoryMaker some time to get started/stopped.
     */
    private fun lockButtons() {
        buttonLocked = true
        //Unlock button in a short bit.
        Thread(BUTTON_UNLOCKER).start()

        mButtonStart.isEnabled = false
        mButtonCancel.isEnabled = false
    }

    companion object {
        private val TAG = "FinalizeActivity"

        private val BUTTON_LOCK_DURATION_MS: Long = 1000
        private val PROGRESS_MAX = 1000

        private val PREF_FILE = "Export_Config"

        private val PREF_KEY_INCLUDE_BACKGROUND_MUSIC = "include_background_music"
        private val PREF_KEY_INCLUDE_PICTURES = "include_pictures"
        private val PREF_KEY_INCLUDE_TEXT = "include_text"
        private val PREF_KEY_INCLUDE_KBFX = "include_kbfx"
        private val PREF_KEY_INCLUDE_SONG = "include_song"
        private val PREF_KEY_SHORT_NAME = "short_name"

        @Volatile
        private var buttonLocked = false
        private val storyMakerLock = Any()
    }
}

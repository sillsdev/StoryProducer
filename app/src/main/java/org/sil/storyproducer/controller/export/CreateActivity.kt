package org.sil.storyproducer.controller.export

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import org.sil.storyproducer.tools.media.story.AutoStoryMaker
import org.sil.storyproducer.tools.stripForFilename
import java.util.*
import java.util.regex.Pattern

class CreateActivity : PhaseBaseActivity() {

    private var mEditTextTitle: EditText? = null
    private var mLayoutConfiguration: View? = null
    private var mLayoutCancel: View? = null
    private var mCheckboxSoundtrack: CheckBox? = null
    private var mCheckboxPictures: CheckBox? = null
    private var mCheckboxText: CheckBox? = null
    private var mCheckboxKBFX: CheckBox? = null
    private var mCheckboxSong: CheckBox? = null
    private var mButtonStart: Button? = null
    private var mButtonCancel: Button? = null
    private var mProgressBar: ProgressBar? = null

    private val mOutputPath: String get() {
        val num = if(Workspace.activeStory.titleNumber != "") "${Workspace.activeStory.titleNumber}_" else {""}
        val name = mEditTextTitle!!.text.toString()
        var ethno = Workspace.registration.getString("ethnologue", "")
        if(ethno != "") ethno = "${ethno}_"
        val fx = if(mCheckboxSoundtrack!!.isChecked) {"Fx"} else {""}
        val px = if(mCheckboxPictures!!.isChecked) {"Px"} else {""}
        val mv = if(mCheckboxKBFX!!.isChecked) {"Mv"} else {""}
        val tx = if(mCheckboxText!!.isChecked) {"Tx"} else {""}
        val sg = if(mCheckboxSong!!.isChecked) {"Sg"} else {""}
        return "$num${name}_$ethno$fx$px$mv$tx$sg.mp4"
    }

    private var mTextConfirmationChecked: Boolean = false

    private var mProgressUpdater: Thread? = null

    private val PROGRESS_UPDATER = Runnable {
        var isDone = false
        var isSuccess = false
        while (!isDone) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                //If progress updater is interrupted, just stop.
                return@Runnable
            }

            var progress = 0.0
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
        isSuccess = storyMaker!!.isSuccess

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
                mButtonStart!!.isEnabled = true
                mButtonCancel!!.isEnabled = true
            }
        }
    }

    private var storyMaker: AutoStoryMaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
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
        mCheckboxPictures!!.setOnCheckedChangeListener { _, _ -> toggleVisibleElements() }
        mCheckboxKBFX!!.setOnCheckedChangeListener { _, _ -> toggleVisibleElements() }
        mCheckboxText!!.setOnCheckedChangeListener { _, _ -> toggleVisibleElements() }
        mCheckboxSong!!.setOnCheckedChangeListener { _, _ -> toggleVisibleElements() }
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
        mEditTextTitle!!.addTextChangedListener( object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val temp = s.toString().stripForFilename()
                if(temp != s.toString()){
                    s!!.replace(0,s.length,temp)
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
        setOnClickListeners()

        mProgressBar = findViewById(R.id.progress_bar_export)
        mProgressBar!!.max = PROGRESS_MAX
        mProgressBar!!.progress = 0

    }

    /**
     * Setup listeners for start/cancel.
     */
    private fun setOnClickListeners() {

        mButtonStart!!.setOnClickListener {
            if (!buttonLocked) {
                tryStartExport()
            }
            lockButtons()
        }

        mButtonCancel!!.setOnClickListener {
            if (!buttonLocked) {
                stopExport()
            }
            lockButtons()
        }

    }

    /**
     * Ensure the proper elements are visible based on checkbox dependencies and whether export process is going.
     */
    private fun toggleVisibleElements() {
        var visibilityPreExport = View.VISIBLE
        var visibilityWhileExport = View.GONE
        synchronized(storyMakerLock) {
            if (storyMaker != null) {

                visibilityPreExport = View.GONE
                visibilityWhileExport = View.VISIBLE
            }
        }

        mLayoutConfiguration!!.visibility = visibilityPreExport
        mLayoutCancel!!.visibility = visibilityWhileExport
        mButtonStart!!.visibility = visibilityPreExport

        if (mCheckboxPictures!!.isChecked) {
            mCheckboxKBFX!!.visibility = View.VISIBLE
            mCheckboxText!!.visibility = View.VISIBLE
        }else{
            mCheckboxKBFX!!.visibility = View.GONE
            mCheckboxKBFX!!.isChecked = false
            mCheckboxText!!.visibility = View.GONE
            mCheckboxText!!.isChecked = false
        }


        if (mCheckboxText!!.isChecked) {
            if (mTextConfirmationChecked) {
                showHighResolutionAlertDialog()
            } else {
                //mSpinnerFormat.setAdapter(mFormatAdapterSmartphone);
                textOrKBFX(false)
            }
        } else {
            //mSpinnerFormat.setAdapter(mFormatAdapterAll);
            mTextConfirmationChecked = true
        }

        //Check if there is a song to play
        if (mCheckboxSong!!.isChecked && (Workspace.getSongFilename() == "")){
            //you have to have a song to include it!
            Toast.makeText(this,getString(R.string.export_local_song_unrecorded),Toast.LENGTH_SHORT).show()
            mCheckboxSong!!.isChecked = false
        }
    }

    /*
    * Function that makes KBFX and Enabling Text mutually exclusive options
    * Takes a boolean that says whether or not text was just turned on
     */
    private fun textOrKBFX(textJustEnabled: Boolean) {
        if (textJustEnabled && !mTextConfirmationChecked && mCheckboxKBFX!!.isChecked) {
            mCheckboxKBFX!!.isChecked = false
        } else {
            if (mCheckboxKBFX!!.isChecked && mCheckboxText!!.isChecked) {
                mCheckboxText!!.isChecked = false
            }
        }
    }

    /**
     * Creates an alert dialog asking if the user wants to skip registration
     * If they respond yes, finish activity or send them back to MainActivity
     */
    private fun showHighResolutionAlertDialog() {
        val dialog = AlertDialog.Builder(this@CreateActivity)
                .setTitle(getString(R.string.export_include_text_title))
                .setMessage(getString(R.string.export_include_text_message))
                .setNegativeButton(getString(R.string.no)) { _, _ ->
                    mCheckboxText!!.isChecked = false
                    mTextConfirmationChecked = true
                }
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    mTextConfirmationChecked = false
                    textOrKBFX(true)
                }.create()

        dialog.show()
    }

    /*
    **Method for handling the click event for the radio buttons
     */
    fun onRadioButtonClicked(view: View) {
        toggleVisibleElements()
    }
    /**
     * Save current configuration options to shared preferences.
     */
    private fun savePreferences() {
        val editor = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()

        editor.putBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack?.isChecked ?: true)
        editor.putBoolean(PREF_KEY_INCLUDE_PICTURES, mCheckboxPictures?.isChecked ?: true)
        editor.putBoolean(PREF_KEY_INCLUDE_TEXT, mCheckboxText?.isChecked ?: true)
        editor.putBoolean(PREF_KEY_INCLUDE_KBFX, mCheckboxKBFX?.isChecked ?: true)
        editor.putBoolean(PREF_KEY_INCLUDE_SONG, mCheckboxSong?.isChecked ?: true)

        editor.putString("$PREF_KEY_SHORT_NAME ${Workspace.activeStory.shortTitle}", mEditTextTitle!!.text.toString())

        editor.apply()
    }

    /**
     * Load configuration options from shared preferences.
     */
    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

        mCheckboxSoundtrack!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, true)
        mCheckboxPictures!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_PICTURES, true)
        mCheckboxText!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_TEXT, false)
        // Don't show the dialog again when reloading the activity.
        if(mCheckboxText!!.isChecked) {mTextConfirmationChecked = false}
        mCheckboxKBFX!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_KBFX, true)
        mCheckboxSong!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_SONG, true)
        mEditTextTitle!!.setText(prefs.getString("$PREF_KEY_SHORT_NAME ${Workspace.activeStory.shortTitle}", ""))
    }

    private fun tryStartExport() {
        //If the credits are unchanged, don't make the video.
        if(!Workspace.isLocalCreditsChanged(this)){
            Toast.makeText(this,this.resources.getText(
                    R.string.export_local_credits_unchanged),Toast.LENGTH_SHORT).show()
            return
        }

        //If there is no title, don't make video
        if(mEditTextTitle!!.text.toString() == ""){
            Toast.makeText(this,this.resources.getText(
                    R.string.export_no_filename),Toast.LENGTH_SHORT).show()
            return
        }

        //Else, check if the file already exists...
        if (workspaceRelPathExists(this,"$VIDEO_DIR/$mOutputPath")) {
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

            storyMaker!!.mIncludeBackgroundMusic = mCheckboxSoundtrack!!.isChecked
            storyMaker!!.mIncludePictures = mCheckboxPictures!!.isChecked
            storyMaker!!.mIncludeText = mCheckboxText!!.isChecked
            storyMaker!!.mIncludeKBFX = mCheckboxKBFX!!.isChecked
            storyMaker!!.mIncludeSong = mCheckboxSong!!.isChecked


            storyMaker!!.mWidth  = 768
            storyMaker!!.mHeight = 576
            storyMaker!!.mVideoBitRate = 5000000
            storyMaker!!.mCodecString = MediaFormat.MIMETYPE_VIDEO_AVC
            storyMaker!!.mVideoFrameRate = 30
            storyMaker!!.mVideoColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

            storyMaker!!.setOutputFile(mOutputPath)
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
        runOnUiThread { mProgressBar!!.progress = progress }
    }

    /**
     * Lock the start/cancel buttons temporarily to give the StoryMaker some time to get started/stopped.
     */
    private fun lockButtons() {
        buttonLocked = true
        //Unlock button in a short bit.
        Thread(BUTTON_UNLOCKER).start()

        mButtonStart!!.isEnabled = false
        mButtonCancel!!.isEnabled = false
    }

    companion object {
        private val TAG = "CreateActivity"

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

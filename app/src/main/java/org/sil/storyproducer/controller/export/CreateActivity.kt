package org.sil.storyproducer.controller.export

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import org.sil.storyproducer.tools.media.story.AutoStoryMaker
import java.util.*

import java.util.regex.Pattern

class CreateActivity : PhaseBaseActivity() {

    private var mEditTextTitle: EditText? = null
    private var mLayoutConfiguration: View? = null
    private var mCheckboxSoundtrack: CheckBox? = null
    private var mCheckboxPictures: CheckBox? = null
    private var mCheckboxText: CheckBox? = null
    private var mCheckboxKBFX: CheckBox? = null
    private var mLayoutResolution: View? = null
    private var mSpinnerResolution: Spinner? = null
    private var mResolutionAdapterAll: ArrayAdapter<CharSequence>? = null
    private var mResolutionAdapterHigh: ArrayAdapter<CharSequence>? = null
    private var mResolutionAdapterLow: ArrayAdapter<CharSequence>? = null
    private var mButtonStart: Button? = null
    private var mButtonCancel: Button? = null
    private var mProgressBar: ProgressBar? = null

    private var phaseUnlocked = false

    private var mOutputPath: String = ""

    private var mTextConfirmationChecked: Boolean = false

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

            var progress: Double = 0.0
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

        runOnUiThread {
            stopExport()
            Toast.makeText(baseContext, "Video created!", Toast.LENGTH_LONG).show()
        }
    }

    private val formatExtension: String = ".mp4"

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
        phaseUnlocked = StorySharedPreferences.isApproved(Workspace.activeStory.title, this)
        setContentView(R.layout.activity_create)
        setupViews()
        invalidateOptionsMenu()
        if (phaseUnlocked) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
        loadPreferences()
    }

    override fun onResume() {
        super.onResume()
        toggleVisibleElements()

        watchProgress()
    }

    override fun onPause() {
        mProgressUpdater!!.interrupt()

        super.onPause()
    }

    override fun onDestroy() {
        savePreferences()

        super.onDestroy()
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.getItem(0)
        item.setIcon(R.drawable.ic_create)
        return true
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    private fun setupViews() {

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.

        mEditTextTitle = findViewById(R.id.editText_export_title)

        mLayoutConfiguration = findViewById(R.id.layout_export_configuration)

        mCheckboxSoundtrack = findViewById(R.id.checkbox_export_soundtrack)
        mCheckboxPictures = findViewById(R.id.checkbox_export_pictures)
        mCheckboxPictures!!.setOnCheckedChangeListener { compoundButton, newState -> toggleVisibleElements() }
        mCheckboxKBFX = findViewById(R.id.checkbox_export_KBFX)
        mCheckboxKBFX!!.setOnCheckedChangeListener { compoundButton, newState -> toggleVisibleElements() }
        mCheckboxText = findViewById(R.id.checkbox_export_text)
        mCheckboxText!!.setOnCheckedChangeListener { compoundButton, newState -> toggleVisibleElements() }

        val resolutionArray = resources.getStringArray(R.array.export_resolution_options)
        val immutableList = Arrays.asList(*resolutionArray)
        val resolutionList = ArrayList(immutableList)
        val resolutionListLow = ArrayList(immutableList)

        mLayoutResolution = findViewById(R.id.layout_export_resolution)
        mSpinnerResolution = findViewById(R.id.spinner_export_resolution)

        mResolutionAdapterHigh = ArrayAdapter(this,
                R.layout.simple_spinner_dropdown_item, resolutionList.toList())
        mResolutionAdapterHigh!!.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        mResolutionAdapterHigh!!.remove(mResolutionAdapterHigh!!.getItem(0))
        mResolutionAdapterHigh!!.remove(mResolutionAdapterHigh!!.getItem(0))

        mResolutionAdapterLow = ArrayAdapter(this,
                R.layout.simple_spinner_dropdown_item, resolutionListLow.toList())
        mResolutionAdapterLow!!.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        mResolutionAdapterLow!!.remove(mResolutionAdapterLow!!.getItem(1))
        mResolutionAdapterLow!!.remove(mResolutionAdapterLow!!.getItem(1))

        mResolutionAdapterAll = ArrayAdapter.createFromResource(this,
                R.array.export_resolution_options, android.R.layout.simple_spinner_item)
        mResolutionAdapterAll!!.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        mSpinnerResolution!!.adapter = mResolutionAdapterAll

        mButtonStart = findViewById(R.id.button_export_start)
        mButtonCancel = findViewById(R.id.button_export_cancel)
        setOnClickListeners()

        mProgressBar = findViewById(R.id.progress_bar_export)
        mProgressBar!!.max = PROGRESS_MAX
        mProgressBar!!.progress = 0

        mEditTextTitle!!.setText(Workspace.activeStory.getVideoTitle())

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
        mButtonStart!!.visibility = visibilityPreExport
        mButtonCancel!!.visibility = visibilityWhileExport
        mProgressBar!!.visibility = visibilityWhileExport

        mCheckboxKBFX!!.visibility = if (mCheckboxPictures!!.isChecked) View.VISIBLE else View.GONE


        mLayoutResolution!!.visibility = if (mCheckboxPictures!!.isChecked || mCheckboxText!!.isChecked)
            View.VISIBLE
        else
            View.GONE


        if (mCheckboxText!!.isChecked) {
            if (mTextConfirmationChecked) {
                showHighResolutionAlertDialog()
            } else {
                mSpinnerResolution!!.adapter = mResolutionAdapterHigh
                //mSpinnerFormat.setAdapter(mFormatAdapterSmartphone);
                textOrKBFX(false)
            }
        } else {
            mSpinnerResolution!!.adapter = mResolutionAdapterAll
            //mSpinnerFormat.setAdapter(mFormatAdapterAll);
            mTextConfirmationChecked = true
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
                .setNegativeButton(getString(R.string.no)) { dialog, id ->
                    mCheckboxText!!.isChecked = false
                    mTextConfirmationChecked = true
                }
                .setPositiveButton(getString(R.string.yes)) { dialog, id ->
                    mSpinnerResolution!!.adapter = mResolutionAdapterHigh
                    mTextConfirmationChecked = false
                    textOrKBFX(true)
                }.create()

        dialog.show()
    }

    /**
     * Save current configuration options to shared preferences.
     */
    private fun savePreferences() {
        val editor = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()

        editor.putBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack!!.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_PICTURES, mCheckboxPictures!!.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_TEXT, mCheckboxText!!.isChecked)
        editor.putBoolean(PREF_KEY_INCLUDE_KBFX, mCheckboxKBFX!!.isChecked)

        editor.putString(PREF_KEY_RESOLUTION, mSpinnerResolution!!.selectedItem.toString())

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
        mCheckboxKBFX!!.isChecked = prefs.getBoolean(PREF_KEY_INCLUDE_KBFX, true)

        setSpinnerValue(mSpinnerResolution, prefs.getString(PREF_KEY_RESOLUTION, null))
    }

    /**
     * Attempt to set the value of the spinner to the given string value based on options available.
     * @param spinner spinner to update value.
     * @param value new value of spinner.
     */
    private fun setSpinnerValue(spinner: Spinner?, value: String?) {
        if (value == null) {
            return
        }

        for (i in 0 until spinner!!.count) {
            if (value == spinner.getItemAtPosition(i).toString()) {
                spinner.setSelection(i)
            }
        }
    }

    private fun tryStartExport() {
        val ext = formatExtension
        mOutputPath = mEditTextTitle!!.text.toString()
        val outputRelPath = mOutputPath + ext

        if (workspaceRelPathExists(this,"$VIDEO_DIR/$outputRelPath")) {
            val dialog = android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.export_location_exists_title))
                    .setMessage(getString(R.string.export_location_exists_message))
                    .setNegativeButton(getString(R.string.no), null)
                    .setPositiveButton(getString(R.string.yes)) { dialog, id -> startExport(outputRelPath) }.create()

            dialog.show()
        } else {
            //mStory = mOutputPath.split("/")[mOutputPath.split("/").length - 1];
            startExport(outputRelPath)
        }
    }

    private fun startExport(outputRelPath: String) {
        synchronized(storyMakerLock) {
            storyMaker = AutoStoryMaker(this)

            storyMaker!!.mIncludeBackgroundMusic = mCheckboxSoundtrack!!.isChecked
            storyMaker!!.mIncludePictures = mCheckboxPictures!!.isChecked
            storyMaker!!.mIncludeText = mCheckboxText!!.isChecked
            storyMaker!!.mIncludeKBFX = mCheckboxKBFX!!.isChecked


            val resolutionStr = mSpinnerResolution!!.selectedItem.toString()
            //Parse resolution string of "[WIDTH]x[HEIGHT]"
            val p = Pattern.compile("(\\d+)x(\\d+)")
            val m = p.matcher(resolutionStr)
            val found = m.find()
            if (found) {
                storyMaker!!.mWidth = Integer.parseInt(m.group(1))
                storyMaker!!.mHeight = Integer.parseInt(m.group(2))
            } else {
                Log.e(TAG, "Resolution in spinner un-parsable.")
            }


            storyMaker!!.setOutputFile(outputRelPath)
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

        private val LOCATION_MAX_CHAR_DISPLAY = 25

        private val BUTTON_LOCK_DURATION_MS: Long = 1000
        private val PROGRESS_MAX = 1000

        private val PREF_FILE = "Export_Config"

        private val PREF_KEY_TITLE = "title"
        private val PREF_KEY_INCLUDE_BACKGROUND_MUSIC = "include_background_music"
        private val PREF_KEY_INCLUDE_PICTURES = "include_pictures"
        private val PREF_KEY_INCLUDE_TEXT = "include_text"
        private val PREF_KEY_INCLUDE_KBFX = "include_kbfx"
        private val PREF_KEY_RESOLUTION = "resolution"
        private val PREF_KEY_FILE = "file"

        @Volatile
        private var buttonLocked = false
        private val storyMakerLock = Any()
    }
}

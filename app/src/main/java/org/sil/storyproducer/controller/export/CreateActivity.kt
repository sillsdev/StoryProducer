package org.sil.storyproducer.controller.export

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
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
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.storyRelPathExists
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
    private var mRadioButtonSmartPhone: RadioButton? = null
    private var mRadioButtonDumbPhone: RadioButton? = null
    private val mSpinnerFormat: Spinner? = null
    private val mFormatAdapterSmartphone: ArrayAdapter<CharSequence>? = null
    private val mFormatAdapterAll: ArrayAdapter<CharSequence>? = null
    private val mEditTextLocation: EditText? = null
    private val mButtonBrowse: Button? = null
    private var mButtonStart: Button? = null
    private var mButtonCancel: Button? = null
    private var mProgressBar: ProgressBar? = null

    private var phaseUnlocked = false

    private var mOutputPath: String = ""

    private var mTextConfirmationChecked: Boolean = false

    //accordion variables
    private val sectionIds = intArrayOf(R.id.export_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)
    private var mProgressUpdater: Thread? = null

    private val videoRelPath: String
        get() {
            var filename = "HEY"

            val sections = ArrayList<String>()
            var title: String? = mEditTextTitle!!.text.toString().replace(" ".toRegex(), "")
                    .replace("[^a-zA-Z0-9\\-_ ]".toRegex(), "")
            if (title == null || title.isEmpty()) {
                title = Workspace.activeStory.title.replace(" ".toRegex(), "")
                        .replace("[^a-zA-Z0-9\\-_ ]".toRegex(), "")
            }
            sections.add(title)

            val country = RegistrationActivity.getCountry()
            if (country != null && !country.isEmpty()) {
                sections.add(country)
            }

            val languageCode = RegistrationActivity.getLanguageCode()
            if (languageCode != null && !languageCode.isEmpty()) {
                sections.add(languageCode)
            }

            filename = stringJoin(sections, "_")
            return "$VIDEO_DIR/$filename"
        }

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

    private val formatExtension: String
        get() {
            var ext = ".mp4"
            if (mRadioButtonSmartPhone!!.isChecked) {
                ext = resources.getStringArray(R.array.export_format_extensions)[0]
            } else {
                ext = resources.getStringArray(R.array.export_format_extensions)[1]
            }
            return ext
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
     * Listen for callback from FileChooserActivity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // See which child activity is calling us back.
        if (requestCode == FILE_CHOOSER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setLocation(data.getStringExtra(FileChooserActivity.FILE_PATH))
            }
        }
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

        mRadioButtonSmartPhone = findViewById(R.id.radio_smartphone)
        mRadioButtonDumbPhone = findViewById(R.id.radio_dumbphone)


        /*String[] formatArray = getResources().getStringArray(R.array.export_format_options);
        List<String> immutableListFormat = Arrays.asList(formatArray);
        ArrayList<String> formatList = new ArrayList<>(immutableListFormat);

        mFormatAdapterSmartphone = new ArrayAdapter(this,
                R.layout.simple_spinner_dropdown_item, formatList);
        mFormatAdapterSmartphone.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mFormatAdapterSmartphone.remove(mFormatAdapterSmartphone.getItem(1));

        mFormatAdapterAll = ArrayAdapter.createFromResource(this,
                R.array.export_format_options, android.R.layout.simple_spinner_item);
        mFormatAdapterAll.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);


        mSpinnerFormat = (Spinner) findViewById(R.id.spinner_export_format);
        mSpinnerFormat.setAdapter(mFormatAdapterAll);*/

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
        /*mEditTextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });*/

        /*mButtonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });*/

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

        /*mSpinnerFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mSpinnerResolution.setAdapter(mResolutionAdapterAll);
                    mSpinnerResolution.setSelection(1, true);
                } else {
                    mSpinnerResolution.setAdapter(mResolutionAdapterLow);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/

    }


    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     *
     * @param headerView  a variable of type View denoting the field the user will click to open up
     * a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private fun setAccordionListener(headerView: View, sectionView: View) {
        headerView.setOnClickListener {
            if (sectionView.visibility == View.GONE) {
                setSectionsClosedExceptView(sectionView)
            } else {
                sectionView.visibility = View.GONE
                headerView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.gray, null))
            }
        }
    }

    /**
     * sets all the accordion sections closed except for the one passed
     * @param sectionView that is wanted to be made open
     */
    private fun setSectionsClosedExceptView(sectionView: View) {
        for (sectionView1 in sectionViews) {
            if (sectionView1 === sectionView) {
                sectionView1.visibility = View.VISIBLE
            } else {
                sectionView1?.visibility = View.GONE
            }
        }

    }

    /**
     * Ensure the proper elements are visible based on checkbox dependencies and whether export process is going.
     */
    private fun toggleVisibleElements() {
        var isStoryMakerBusy = false

        var visibilityPreExport = View.VISIBLE
        var visibilityWhileExport = View.GONE
        synchronized(storyMakerLock) {
            if (storyMaker != null) {
                isStoryMakerBusy = true

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
                    mRadioButtonDumbPhone!!.isChecked = false
                    mRadioButtonSmartPhone!!.isChecked = true
                }.create()

        dialog.show()
    }

    /*
    **Method for handling the click event for the radio buttons
     */
    fun onRadioButtonClicked(view: View) {
        // Is the button now checked?
        val checked = (view as RadioButton).isChecked

        // Check which radio button was clicked
        when (view.getId()) {
            R.id.radio_dumbphone -> {
                if (checked)
                //Only low resolution on dumbphone and uncheck include text
                    mSpinnerResolution!!.adapter = mResolutionAdapterLow
                mCheckboxText!!.isChecked = false
            }
            R.id.radio_smartphone -> {
                if (checked)
                //Default to medium resolution on smartphone
                    mSpinnerResolution!!.adapter = mResolutionAdapterAll
                mSpinnerResolution!!.setSelection(1, true)
            }
        }
    }

    /**
     * Set the path for export location, including UI.
     * @param path new export location.
     */
    private fun setLocation(path: String?) {
        var path = path
        if (path == null) {
            path = ""
        }
        mOutputPath = path
        var display: String = path
        if (path.length > LOCATION_MAX_CHAR_DISPLAY) {
            display = "..." + path.substring(path.length - LOCATION_MAX_CHAR_DISPLAY + 3)
        }
        //mEditTextLocation.setText(display);
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
        //editor.putString(PREF_KEY_FORMAT, mSpinnerFormat.getSelectedItem().toString());
        editor.putBoolean(PREF_RB_DUMBPHONE, mRadioButtonDumbPhone!!.isChecked)
        editor.putBoolean(PREF_RB_SMARTPHONE, mRadioButtonSmartPhone!!.isChecked)

        editor.putString(Workspace.activeStory.title + PREF_KEY_TITLE, mEditTextTitle!!.text.toString())
        editor.putString(Workspace.activeStory.title + PREF_KEY_FILE, mOutputPath)

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
        //setSpinnerValue(mSpinnerFormat, prefs.getString(PREF_KEY_FORMAT, null));
        mRadioButtonDumbPhone!!.isChecked = prefs.getBoolean(PREF_RB_DUMBPHONE, true)
        mRadioButtonSmartPhone!!.isChecked = prefs.getBoolean(PREF_RB_SMARTPHONE, false)
        mEditTextTitle!!.setText(prefs.getString(Workspace.activeStory.title + PREF_KEY_TITLE, Workspace.activeStory.title))
        setLocation(prefs.getString(Workspace.activeStory.title + PREF_KEY_FILE, null))

        //mTextConfirmationChecked = true;
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
        setLocation(videoRelPath)

        val ext = formatExtension
        val outputRelPath = mOutputPath + ext

        if (storyRelPathExists(this,outputRelPath)) {
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

    private fun stringJoin(list: List<String>, delimeter: String): String {
        val result = StringBuilder()

        var isFirst = true
        for (str in list) {
            if (isFirst) {
                isFirst = false
            } else {
                result.append(delimeter)
            }

            result.append(str)
        }
        return result.toString()
    }

    private fun startExport(outputRelPath: String) {
        synchronized(storyMakerLock) {
            storyMaker = AutoStoryMaker(this)

            storyMaker!!.toggleBackgroundMusic(mCheckboxSoundtrack!!.isChecked)
            storyMaker!!.togglePictures(mCheckboxPictures!!.isChecked)
            storyMaker!!.toggleText(mCheckboxText!!.isChecked)
            storyMaker!!.toggleKenBurns(mCheckboxKBFX!!.isChecked)

            val resolutionStr = mSpinnerResolution!!.selectedItem.toString()
            //Parse resolution string of "[WIDTH]x[HEIGHT]"
            val p = Pattern.compile("(\\d+)x(\\d+)")
            val m = p.matcher(resolutionStr)
            val found = m.find()
            if (found) {
                storyMaker!!.setResolution(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)))
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

        private val FILE_CHOOSER_CODE = 1
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
        private val PREF_KEY_FORMAT = "format"
        private val PREF_RB_SMARTPHONE = "smartphone"
        private val PREF_RB_DUMBPHONE = "dumbphone"
        private val PREF_KEY_FILE = "file"

        @Volatile
        private var buttonLocked = false
        private val storyMakerLock = Any()
    }
}

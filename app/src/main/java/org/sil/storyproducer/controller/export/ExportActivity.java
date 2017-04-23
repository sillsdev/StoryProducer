package org.sil.storyproducer.controller.export;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.VideoFiles;
import org.sil.storyproducer.tools.media.story.AutoStoryMaker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportActivity extends PhaseBaseActivity {
    private static final String TAG = "ExportActivity";

    private static final int FILE_CHOOSER_CODE = 1;
    private static final int LOCATION_MAX_CHAR_DISPLAY = 25;

    private static final long BUTTON_LOCK_DURATION_MS = 1000;
    private static final int PROGRESS_MAX = 1000;

    private static final String PREF_FILE = "Export_Config";

    private static final String PREF_KEY_TITLE = "title";
    private static final String PREF_KEY_INCLUDE_BACKGROUND_MUSIC = "include_background_music";
    private static final String PREF_KEY_INCLUDE_PICTURES = "include_pictures";
    private static final String PREF_KEY_INCLUDE_TEXT = "include_text";
    private static final String PREF_KEY_INCLUDE_KBFX = "include_kbfx";
    private static final String PREF_KEY_RESOLUTION = "resolution";
    private static final String PREF_KEY_FORMAT = "format";
    private static final String PREF_KEY_FILE = "file";

    private EditText mEditTextTitle;
    private View mLayoutConfiguration;
    private CheckBox mCheckboxSoundtrack;
    private CheckBox mCheckboxPictures;
    private CheckBox mCheckboxText;
    private CheckBox mCheckboxKBFX;
    private View mLayoutResolution;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerFormat;
    private EditText mEditTextLocation;
    private Button mButtonBrowse;
    private Button mButtonStart;
    private Button mButtonCancel;
    private ProgressBar mProgressBar;
    private TextView mNoVideosText;
    private ListView mVideosListView;

    private ExportedVideosAdapter videosAdapter;
    private String mStory;

    private String mOutputPath;

    //accordion variables
    private final int [] sectionIds = {R.id.export_section, R.id.share_section};
    private final int [] headerIds = {R.id.export_header, R.id.share_header};
    private View[] sectionViews = new View[sectionIds.length];
    private View[] headerViews = new View[headerIds.length];

    private static volatile boolean buttonLocked = false;
    private Thread mProgressUpdater;
    private static final Object storyMakerLock = new Object();
    private static AutoStoryMaker storyMaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStory = StoryState.getStoryName();     //needs to be set first because some of the views use it

        boolean phaseUnlocked = StorySharedPreferences.isApproved(mStory, this);
        setContentView(R.layout.activity_export);
        mStory = StoryState.getStoryName();
        setupViews();

        if (phaseUnlocked) {
            findViewById(R.id.lock_overlay).setVisibility(View.INVISIBLE);
        } else {
            View mainLayout = findViewById(R.id.main_linear_layout);
            PhaseBaseActivity.disableViewAndChildren(mainLayout);
        }
        setVideoOrShareSectionOpen();
        loadPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        toggleVisibleElements();

        watchProgress();
    }

    @Override
    protected void onPause() {
        mProgressUpdater.interrupt();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        savePreferences();

        super.onDestroy();
    }

    /**
     * Listen for callback from FileChooserActivity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (requestCode == FILE_CHOOSER_CODE) {
            if (resultCode == RESULT_OK) {
                setLocation(data.getStringExtra(FileChooserActivity.FILE_PATH));
            }
        }
    }

    /**
     * Remove focus from EditText when tapping outside. See http://stackoverflow.com/a/28939113/4639640
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    private void setupViews() {

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (int i = 0; i < sectionIds.length; i++) {
            sectionViews[i] = findViewById(sectionIds[i]);
            headerViews[i] = findViewById(headerIds[i]);
            setAccordionListener(findViewById(headerIds[i]), sectionViews[i]);
        }

        mEditTextTitle = (EditText) findViewById(R.id.editText_export_title);

        mLayoutConfiguration = findViewById(R.id.layout_export_configuration);

        mCheckboxSoundtrack = (CheckBox) findViewById(R.id.checkbox_export_soundtrack);
        mCheckboxPictures = (CheckBox) findViewById(R.id.checkbox_export_pictures);
        mCheckboxPictures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean newState) {
                toggleVisibleElements();
            }
        });
        mCheckboxKBFX = (CheckBox) findViewById(R.id.checkbox_export_KBFX);
        mCheckboxText = (CheckBox) findViewById(R.id.checkbox_export_text);
        mCheckboxText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean newState) {
                toggleVisibleElements();
            }
        });

        mLayoutResolution = findViewById(R.id.layout_export_resolution);
        mSpinnerResolution = (Spinner) findViewById(R.id.spinner_export_resolution);
        ArrayAdapter<CharSequence> resolutionAdapter = ArrayAdapter.createFromResource(this,
                R.array.export_resolution_options, android.R.layout.simple_spinner_item);
        resolutionAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mSpinnerResolution.setAdapter(resolutionAdapter);

        mSpinnerFormat = (Spinner) findViewById(R.id.spinner_export_format);
        ArrayAdapter<CharSequence> formatAdapter = ArrayAdapter.createFromResource(this,
                R.array.export_format_options, android.R.layout.simple_spinner_item);
        formatAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mSpinnerFormat.setAdapter(formatAdapter);

        mEditTextLocation = (EditText) findViewById(R.id.editText_export_location);

        mButtonBrowse = (Button) findViewById(R.id.button_export_browse);
        mButtonStart = (Button) findViewById(R.id.button_export_start);
        mButtonCancel = (Button) findViewById(R.id.button_export_cancel);
        setOnClickListeners();

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_export);
        mProgressBar.setMax(PROGRESS_MAX);
        mProgressBar.setProgress(0);

        //share view
        videosAdapter = new ExportedVideosAdapter(this);
        mVideosListView = (ListView) findViewById(R.id.videos_list);
        mVideosListView.setAdapter(videosAdapter);
        mNoVideosText = (TextView)findViewById(R.id.no_videos_text);
        setVideoAdapterPaths();

    }

    /**
     * Setup listeners for start/cancel.
     */
    private void setOnClickListeners() {
        mEditTextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });

        mButtonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });

        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!buttonLocked) {
                    tryStartExport();
                }
                lockButtons();
            }
        });

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!buttonLocked) {
                    stopExport();
                }
                lockButtons();
            }
        });

    }

    /**
     * sets which one of the accordians starts open on the activity start
     */
    private void setVideoOrShareSectionOpen() {
        List<String> actualPaths = getExportedVideosForStory();
        if(actualPaths.size() > 0) {        //open the share view
            setSectionsClosedExceptView(findViewById(R.id.share_section));
        } else {                            //open the video creation view
            setSectionsClosedExceptView(findViewById(R.id.export_section));
        }
    }

    /**
     * This function sets the click listeners to implement the accordion functionality
     * for each section of the registration page
     *
     * @param headerView  a variable of type View denoting the field the user will click to open up
     *                    a section of the registration
     * @param sectionView a variable of type View denoting the section that will open up
     */
    private void setAccordionListener(final View headerView, final View sectionView) {
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sectionView.getVisibility() == View.GONE) {
                    setSectionsClosedExceptView(sectionView);
                } else {
                    sectionView.setVisibility(View.GONE);
                    headerView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.gray, null));
                }
            }
        });
    }

    /**
     * sets all the accordion sections closed except for the one passed
     * @param sectionView that is wanted to be made open
     */
    private void setSectionsClosedExceptView(View sectionView) {
        for(int k = 0; k < sectionViews.length; k++) {
            if(sectionViews[k] == sectionView) {
                sectionViews[k].setVisibility(View.VISIBLE);
                headerViews[k].setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.primary, null));
            } else {
                sectionViews[k].setVisibility(View.GONE);
                headerViews[k].setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.gray, null));
            }
        }

    }

    /**
     * Ensure the proper elements are visible based on checkbox dependencies and whether export process is going.
     */
    private void toggleVisibleElements() {
        int visibilityPreExport = View.VISIBLE;
        int visibilityWhileExport = View.GONE;
        synchronized (storyMakerLock) {
            if (storyMaker != null) {
                visibilityPreExport = View.GONE;
                visibilityWhileExport = View.VISIBLE;
            }
        }

        mLayoutConfiguration.setVisibility(visibilityPreExport);
        mButtonStart.setVisibility(visibilityPreExport);
        mButtonCancel.setVisibility(visibilityWhileExport);
        mProgressBar.setVisibility(visibilityWhileExport);

        mCheckboxKBFX.setVisibility(mCheckboxPictures.isChecked() ? View.VISIBLE : View.GONE);

        mLayoutResolution.setVisibility(mCheckboxPictures.isChecked() || mCheckboxText.isChecked()
                ? View.VISIBLE : View.GONE);
    }

    /**
     * Launch the file explorer.
     */
    private void openFileExplorerToExport() {
        String initialFileExplorerLocation = VideoFiles.getDefaultLocation(mStory).getPath();
        String currentLocation = mOutputPath;
        File currentLocFile = new File(currentLocation);
        File currentParent = currentLocFile.getParentFile();
        if(currentLocFile.isDirectory() || (currentParent != null && currentParent.exists())) {
            initialFileExplorerLocation = currentLocation;
        }

        Intent intent = new Intent(this, FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.ALLOW_OVERWRITE, true);
        intent.putExtra(FileChooserActivity.INITIAL_PATH, initialFileExplorerLocation);
        startActivityForResult(intent, FILE_CHOOSER_CODE);
    }

    /**
     * Set the path for export location, including UI.
     * @param path new export location.
     */
    private void setLocation(String path) {
        if(path == null) {
            path = "";
        }
        mOutputPath = path;
        String display = path;
        if(path.length() > LOCATION_MAX_CHAR_DISPLAY) {
            display = "..." + path.substring(path.length() - LOCATION_MAX_CHAR_DISPLAY + 3);
        }
        mEditTextLocation.setText(display);
    }


    /**
     * sets the videos for the list adapter
     */
    private void setVideoAdapterPaths() {
        List<String> actualPaths = getExportedVideosForStory();
        if(actualPaths.size() > 0) {
            mNoVideosText.setVisibility(View.GONE);
        }
        videosAdapter.setVideoPaths(actualPaths);
    }

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    private List<String> getExportedVideosForStory() {
        List<String> actualPaths = new ArrayList<>();
        List<String> videoPaths = StorySharedPreferences.getExportedVideosForStory(mStory);
        for(String path : videoPaths) {          //make sure the file actually exists
            File file = new File(path);
            if(file.exists()) {
                actualPaths.add(path);
            }
        }
        return actualPaths;
    }

    /**
     * Save current configuration options to shared preferences.
     */
    private void savePreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_FILE, MODE_PRIVATE).edit();

        editor.putBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack.isChecked());
        editor.putBoolean(PREF_KEY_INCLUDE_PICTURES, mCheckboxPictures.isChecked());
        editor.putBoolean(PREF_KEY_INCLUDE_TEXT, mCheckboxText.isChecked());
        editor.putBoolean(PREF_KEY_INCLUDE_KBFX, mCheckboxKBFX.isChecked());

        editor.putString(PREF_KEY_RESOLUTION, mSpinnerResolution.getSelectedItem().toString());
        editor.putString(PREF_KEY_FORMAT, mSpinnerFormat.getSelectedItem().toString());

        editor.putString(mStory + PREF_KEY_TITLE, mEditTextTitle.getText().toString());
        editor.putString(mStory + PREF_KEY_FILE, mOutputPath);

        editor.apply();
    }

    /**
     * Load configuration options from shared preferences.
     */
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        mCheckboxSoundtrack.setChecked(prefs.getBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, true));
        mCheckboxPictures.setChecked(prefs.getBoolean(PREF_KEY_INCLUDE_PICTURES, true));
        mCheckboxText.setChecked(prefs.getBoolean(PREF_KEY_INCLUDE_TEXT, false));
        mCheckboxKBFX.setChecked(prefs.getBoolean(PREF_KEY_INCLUDE_KBFX, true));

        setSpinnerValue(mSpinnerResolution, prefs.getString(PREF_KEY_RESOLUTION, null));
        setSpinnerValue(mSpinnerFormat, prefs.getString(PREF_KEY_FORMAT, null));

        mEditTextTitle.setText(prefs.getString(mStory + PREF_KEY_TITLE, mStory));
        setLocation(prefs.getString(mStory + PREF_KEY_FILE, null));
    }

    /**
     * Attempt to set the value of the spinner to the given string value based on options available.
     * @param spinner spinner to update value.
     * @param value new value of spinner.
     */
    private void setSpinnerValue(Spinner spinner, String value) {
        if(value == null) {
            return;
        }

        for(int i = 0; i < spinner.getCount(); i++) {
            if(value.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
            }
        }
    }

    private void tryStartExport() {
        if(mOutputPath == null || mOutputPath.isEmpty()) {
            Toast.makeText(this, R.string.export_location_missing_message, Toast.LENGTH_LONG).show();
            return;
        }

        String ext = mSpinnerFormat.getSelectedItem().toString();
        final File output = new File(mOutputPath + ext);

        if(output.exists()) {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.export_location_exists_title))
                .setMessage(getString(R.string.export_location_exists_message))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startExport(output);
                    }
                }).create();

            dialog.show();
        }
        else {
            startExport(output);
        }
    }

    private void startExport(File output) {
        synchronized (storyMakerLock) {
            storyMaker = new AutoStoryMaker(mStory);
            storyMaker.setContext(this);

            storyMaker.setTitle(mEditTextTitle.getText().toString());

            storyMaker.toggleBackgroundMusic(mCheckboxSoundtrack.isChecked());
            storyMaker.togglePictures(mCheckboxPictures.isChecked());
            storyMaker.toggleText(mCheckboxText.isChecked());
            storyMaker.toggleKenBurns(mCheckboxKBFX.isChecked());

            String resolutionStr = mSpinnerResolution.getSelectedItem().toString();
            //Parse resolution string of "[WIDTH]x[HEIGHT]"
            Pattern p = Pattern.compile("(\\d+)x(\\d+)");
            Matcher m = p.matcher(resolutionStr);
            boolean found = m.find();
            if(found) {
                storyMaker.setResolution(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            }
            else {
                Log.e(TAG, "Resolution in spinner un-parsable.");
            }


            storyMaker.setOutputFile(output);
        }

        storyMaker.start();
        watchProgress();
    }

    private void stopExport() {
        synchronized (storyMakerLock) {
            if (storyMaker != null) {
                storyMaker.close();
                storyMaker = null;
            }
        }
        //update the list view
        setVideoAdapterPaths();
        toggleVisibleElements();
    }

    private void watchProgress() {
        mProgressUpdater = new Thread(PROGRESS_UPDATER);
        mProgressUpdater.start();
        toggleVisibleElements();
    }

    private void updateProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(progress);
            }
        });
    }

    private final Runnable PROGRESS_UPDATER = new Runnable() {
        @Override
        public void run() {
            boolean isDone = false;
            while(!isDone) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //If progress updater is interrupted, just stop.
                    return;
                }
                double progress;
                synchronized (storyMakerLock) {
                    //Stop if storyMaker was cancelled by someone else.
                    if(storyMaker == null) {
                        updateProgress(0);
                        return;
                    }

                    progress = storyMaker.getProgress();
                    isDone = storyMaker.isDone();
                }
                updateProgress((int) (progress * PROGRESS_MAX));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //save the file only when the video file is actually created
                    String ext = mSpinnerFormat.getSelectedItem().toString();
                    File output = new File(mOutputPath + ext);
                    StorySharedPreferences.addExportedVideoForStory(output.getAbsolutePath(), mStory);
                    stopExport();
                    Toast.makeText(getBaseContext(), "Video created!", Toast.LENGTH_LONG).show();
                    setSectionsClosedExceptView(findViewById(R.id.share_section));

                }
            });
        }
    };

    /**
     * Lock the start/cancel buttons temporarily to give the StoryMaker some time to get started/stopped.
     */
    private void lockButtons() {
        buttonLocked = true;
        //Unlock button in a short bit.
        new Thread(BUTTON_UNLOCKER).start();

        mButtonStart.setEnabled(false);
        mButtonCancel.setEnabled(false);
    }

    /**
     * Unlock the start/cancel buttons after a brief time period.
     */
    private final Runnable BUTTON_UNLOCKER = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(BUTTON_LOCK_DURATION_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                buttonLocked = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mButtonStart.setEnabled(true);
                        mButtonCancel.setEnabled(true);
                    }
                });
            }
        }
    };
}

package org.sil.storyproducer.controller.export;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.VideoFiles;
import org.sil.storyproducer.tools.media.story.AutoStoryMaker;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportActivity extends PhaseBaseActivity {
    private static final String TAG = "ExportActivity";

    private static final int FILE_CHOOSER_CODE = 1;
    private static final int LOCATION_MAX_CHAR_DISPLAY = 25;

    private static final long BUTTON_LOCK_DURATION_MS = 1000;
    private static final int PROGRESS_MAX = 1000;

    private static final String PREF_FILE_BASE = "ProjExportConfig";
    private static final String PREF_FILE_ALL = "AppExportConfig";

    private static final String PREF_KEY_INCLUDE_BACKGROUND_MUSIC = "include_background_music";
    private static final String PREF_KEY_INCLUDE_PICTURES = "include_pictures";
    private static final String PREF_KEY_INCLUDE_TEXT = "include_text";
    private static final String PREF_KEY_INCLUDE_KBFX = "include_kbfx";
    private static final String PREF_KEY_RESOLUTION = "resolution";
    private static final String PREF_KEY_FORMAT = "format";
    private static final String PREF_KEY_FILE = "file";

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

    private String mOutputPath;

    private static volatile boolean buttonLocked = false;
    private Thread mProgressUpdater;
    private static final Object storyMakerLock = new Object();
    private static AutoStoryMaker storyMaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        setupViews();
    }

    @Override
    public void onResume() {
        super.onResume();

        watchProgress();
    }

    @Override
    public void onPause() {
        savePreferences();

        mProgressUpdater.interrupt();

        super.onPause();
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
     * Get handles to all necessary views and add some listeners.
     */
    private void setupViews() {
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
        mEditTextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });

        mButtonBrowse = (Button) findViewById(R.id.button_export_browse);
        mButtonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorerToExport();
            }
        });

        mButtonStart = (Button) findViewById(R.id.button_export_start);
        mButtonCancel = (Button) findViewById(R.id.button_export_cancel);
        setOnClickListeners();

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_export);
        mProgressBar.setMax(PROGRESS_MAX);
        mProgressBar.setProgress(0);

        loadPreferences();

        toggleVisibleElements();
    }

    /**
     * Setup listeners for start/cancel.
     */
    private void setOnClickListeners() {
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
        String initialFileExplorerLocation = VideoFiles.getDefaultLocation(StoryState.getStoryName()).getPath();
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
     * @param path
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
     * Save current configuration options to shared preferences.
     */
    private void savePreferences() {
        //Save preferences universal for app.
        SharedPreferences.Editor prefEditorApp = getSharedPreferences(PREF_FILE_ALL, MODE_PRIVATE).edit();

        prefEditorApp.putBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack.isChecked());
        prefEditorApp.putBoolean(PREF_KEY_INCLUDE_PICTURES, mCheckboxPictures.isChecked());
        prefEditorApp.putBoolean(PREF_KEY_INCLUDE_TEXT, mCheckboxText.isChecked());
        prefEditorApp.putBoolean(PREF_KEY_INCLUDE_KBFX, mCheckboxKBFX.isChecked());

        prefEditorApp.putString(PREF_KEY_RESOLUTION, mSpinnerResolution.getSelectedItem().toString());
        prefEditorApp.putString(PREF_KEY_FORMAT, mSpinnerFormat.getSelectedItem().toString());

        prefEditorApp.apply();

        //Save preferences specific to this project.
        SharedPreferences.Editor prefEditorProject = getSharedPreferences(
                PREF_FILE_BASE + StoryState.getStoryName(), MODE_PRIVATE).edit();
        prefEditorProject.putString(PREF_KEY_FILE, mOutputPath);
        prefEditorProject.apply();
    }

    /**
     * Load configuration options from shared preferences.
     */
    private void loadPreferences() {
        //Get preferences universal for app.
        SharedPreferences prefsApp = getSharedPreferences(PREF_FILE_ALL, MODE_PRIVATE);

        mCheckboxSoundtrack.setChecked(prefsApp.getBoolean(PREF_KEY_INCLUDE_BACKGROUND_MUSIC, true));
        mCheckboxPictures.setChecked(prefsApp.getBoolean(PREF_KEY_INCLUDE_PICTURES, true));
        mCheckboxText.setChecked(prefsApp.getBoolean(PREF_KEY_INCLUDE_TEXT, false));
        mCheckboxKBFX.setChecked(prefsApp.getBoolean(PREF_KEY_INCLUDE_KBFX, true));

        setSpinnerValue(mSpinnerResolution, prefsApp.getString(PREF_KEY_RESOLUTION, null));
        setSpinnerValue(mSpinnerFormat, prefsApp.getString(PREF_KEY_FORMAT, null));

        //Get preferences specific to this project.
        SharedPreferences prefsProject = getSharedPreferences(
                PREF_FILE_BASE + StoryState.getStoryName(), MODE_PRIVATE);
        setLocation(prefsProject.getString(PREF_KEY_FILE, null));
    }

    /**
     * Attempt to set the value of the spinner to the given string value based on options available.
     * @param spinner
     * @param value
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
            storyMaker = new AutoStoryMaker(StoryState.getStoryName());

            storyMaker.toggleBackgroundMusic(mCheckboxSoundtrack.isChecked());
            storyMaker.togglePictures(mCheckboxPictures.isChecked());
            storyMaker.toggleText(mCheckboxText.isChecked());
            storyMaker.toggleKenBurns(mCheckboxKBFX.isChecked());

            String resolutionStr = mSpinnerResolution.getSelectedItem().toString();
            //Parse resolution string of "WIDTHxHEIGHT"
            Pattern p = Pattern.compile("(\\d+)x(\\d+)");
            Matcher m = p.matcher(resolutionStr);
            m.find();
            storyMaker.setResolution(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));


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
                double progress = 0;
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
                    stopExport();
                    Toast.makeText(getBaseContext(), "Video created!", Toast.LENGTH_LONG).show();
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

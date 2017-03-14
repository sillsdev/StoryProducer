package org.sil.storyproducer.controller.export;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.DrawerItemClickListener;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;
import org.sil.storyproducer.tools.file.VideoFiles;
import org.sil.storyproducer.tools.media.story.AutoStoryMaker;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportActivity extends AppCompatActivity {
    private static final String TAG = "ExportActivity";

    private static final int FILE_CHOOSER_CODE = 1;

    private static final String PREFERENCES_BASE = "ProjExportConfig";
    private static final String PREFERENCES_ALL = "AppExportConfig";

    private GestureDetectorCompat mDetector;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private View mLayoutConfiguration;
    private CheckBox mCheckboxSoundtrack;
    private CheckBox mCheckboxPictures;
    private CheckBox mCheckboxText;
    private CheckBox mCheckboxKBFX;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerFormat;
    private EditText mEditTextLocation;
    private Button mButtonBrowse;
    private Button mButtonStart;
    private Button mButtonCancel;

    private static final long BUTTON_LOCK_DURATION_MS = 1000;
    private static volatile boolean buttonLocked = false;

    private ProgressBar mProgressBar;
    private int mCurrentProgress = 0;
    private static final int PROGRESS_MAX = 1000;
    private Thread mProgressUpdater;

    private static final Object storyMakerLock = new Object();
    private static AutoStoryMaker storyMaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        //get the current phase
        Phase phase = StoryState.getCurrentPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources(), phase.getColor(), null)));

        setupDrawer();

        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));

        setupViews();
    }

    private void setupViews() {
        mLayoutConfiguration = findViewById(R.id.layout_export_configuration);

        mCheckboxSoundtrack = (CheckBox) findViewById(R.id.checkbox_export_soundtrack);
        mCheckboxPictures = (CheckBox) findViewById(R.id.checkbox_export_pictures);
        mCheckboxText = (CheckBox) findViewById(R.id.checkbox_export_text);
        mCheckboxKBFX = (CheckBox) findViewById(R.id.checkbox_export_KBFX);

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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * sets the Menu spinner_item object
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_phases, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        spinner.setOnItemSelectedListener(new PhaseMenuItemListener(this));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.phases_menu_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(StoryState.getCurrentPhaseIndex());
        return true;
    }

    /**
     * get the touch event so that it can be passed on to GestureDetector
     * @param event the MotionEvent
     * @return the super version of the function
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void openFileExplorerToExport() {
        Intent intent = new Intent(this, FileChooserActivity.class);
        intent.putExtra(FileChooserActivity.ALLOW_OVERWRITE, true);
        intent.putExtra(FileChooserActivity.PROJECT_DIRECTORY, VideoFiles.getDefaultLocation(StoryState.getStoryName()).getPath());
        startActivityForResult(intent, FILE_CHOOSER_CODE);
    }

    private String mOutputPath;
    private File mOutputFile;

    // Listen for results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (requestCode == FILE_CHOOSER_CODE) {
            if (resultCode == RESULT_OK) {
                mOutputPath = data.getStringExtra(FileChooserActivity.FILE_PATH);
                mEditTextLocation.setText(mOutputPath);
            }
        }
    }
        
    /**
     * initializes the items that the drawer needs
     */
    private void setupDrawer() {
        //TODO maybe take this code off into somewhere so we don't have to duplicate it as much
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        addDrawerItems();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(getApplicationContext()));
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.nav_open, R.string.dummy_content) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getSupportActionBar().setTitle("Navigation!");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getSupportActionBar().setTitle("blah");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    /**
     * adds the items to the drawer from the array resources
     */
    private void addDrawerItems() {
        String[] menuArray = getResources().getStringArray(R.array.global_menu_array);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menuArray);
        mDrawerList.setAdapter(mAdapter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();                                  //needed to make the drawer synced
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);            //needed to make the drawer synced
    }

    private void toggleVisibleElements() {
        synchronized (storyMakerLock) {
            if (storyMaker == null) {
                mLayoutConfiguration.setVisibility(View.VISIBLE);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonCancel.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
            } else {
                mLayoutConfiguration.setVisibility(View.GONE);
                mButtonStart.setVisibility(View.GONE);
                mButtonCancel.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    private void watchProgress() {
        mProgressUpdater = new Thread(new ProgressUpdater());
        mProgressUpdater.start();
        toggleVisibleElements();
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

    private void lockButtons() {
        buttonLocked = true;
        new Thread(BUTTON_UNLOCKER).start();

        mButtonStart.setEnabled(false);
        mButtonCancel.setEnabled(false);
    }

    private static final String PREF_INCLUDE_BACKGROUND_MUSIC   = "include_background_music";
    private static final String PREF_INCLUDE_PICTURES           = "include_pictures";
    private static final String PREF_INCLUDE_TEXT               = "include_text";
    private static final String PREF_INCLUDE_KBFX               = "include_kbfx";
    private static final String PREF_RESOLUTION                 = "resolution";
    private static final String PREF_FORMAT                     = "format";
    private static final String PREF_FILE                       = "file";

    private void savePreferences() {
        SharedPreferences.Editor prefEditorAll = getSharedPreferences(PREFERENCES_ALL, MODE_PRIVATE).edit();
        SharedPreferences.Editor prefEditorMe = getSharedPreferences(
                PREFERENCES_BASE + StoryState.getStoryName(), MODE_PRIVATE).edit();

        prefEditorAll.putBoolean(PREF_INCLUDE_BACKGROUND_MUSIC, mCheckboxSoundtrack.isChecked());
        prefEditorAll.putBoolean(PREF_INCLUDE_PICTURES, mCheckboxPictures.isChecked());
        prefEditorAll.putBoolean(PREF_INCLUDE_TEXT, mCheckboxText.isChecked());
        prefEditorAll.putBoolean(PREF_INCLUDE_KBFX, mCheckboxKBFX.isChecked());

        prefEditorAll.putString(PREF_RESOLUTION, mSpinnerResolution.getSelectedItem().toString());
        prefEditorAll.putString(PREF_FORMAT, mSpinnerFormat.getSelectedItem().toString());

        prefEditorMe.putString(PREF_FILE, mOutputPath);

        prefEditorAll.apply();
        prefEditorMe.apply();
    }

    private void loadPreferences() {
        SharedPreferences prefAll = getSharedPreferences(PREFERENCES_ALL, MODE_PRIVATE);
        SharedPreferences prefMe = getSharedPreferences(
                PREFERENCES_BASE + StoryState.getStoryName(), MODE_PRIVATE);

        mCheckboxSoundtrack.setChecked(prefAll.getBoolean(PREF_INCLUDE_BACKGROUND_MUSIC, true));
        mCheckboxPictures.setChecked(prefAll.getBoolean(PREF_INCLUDE_PICTURES, true));
        mCheckboxText.setChecked(prefAll.getBoolean(PREF_INCLUDE_TEXT, false));
        mCheckboxKBFX.setChecked(prefAll.getBoolean(PREF_INCLUDE_KBFX, true));

        setSpinnerValue(mSpinnerResolution, prefAll.getString(PREF_RESOLUTION, null));
        setSpinnerValue(mSpinnerFormat, prefAll.getString(PREF_FORMAT, null));

        mOutputPath = prefMe.getString(PREF_FILE, null);
        mEditTextLocation.setText(mOutputPath);
    }

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

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

    private void updateProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(progress);
            }
        });
    }

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

    private final class ProgressUpdater implements Runnable {
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
}

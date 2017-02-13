package org.sil.storyproducer.controller.export;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.DrawerItemClickListener;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;
import org.sil.storyproducer.tools.media.story.AutoStoryMaker;

public class ExportActivity extends AppCompatActivity {

    private GestureDetectorCompat mDetector;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private Button mButtonStart;
    private Button mButtonCancel;

    private ProgressBar mProgressBar;
    private int mCurrentProgress = 0;
    private static final int PROGRESS_MAX = 1000;

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

        mButtonStart = (Button) findViewById(R.id.button_export_start);
        mButtonCancel = (Button) findViewById(R.id.button_export_cancel);
        toggleButtons();

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_export);
        mProgressBar.setMax(PROGRESS_MAX);
        mProgressBar.setProgress(0);
//        new Thread() {
//            @Override
//            public void run() {
//                while(mCurrentProgress < 100) {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d("ExportActivity", "progress: " + mCurrentProgress);
//                    mProgressBar.setProgress(++mCurrentProgress);
//                }
////                mProgressBar.setVisibility(View.INVISIBLE);
//            }
//        }.start();
    }

    @Override
    public void onDestroy() {

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
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuArray);
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

    private void toggleButtons() {
        synchronized (storyMakerLock) {
            if (storyMaker == null) {
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonCancel.setVisibility(View.GONE);
            } else {
                mButtonCancel.setVisibility(View.VISIBLE);
                mButtonStart.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    public void onStartExport(View view) {
        synchronized (storyMakerLock) {
            storyMaker = new AutoStoryMaker(StoryState.getStoryName());
            storyMaker.start();

            new Thread() {
                @Override
                public void run() {
                    boolean isDone = false;
                    while(!isDone) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        double progress = 0;
                        synchronized (storyMakerLock) {
                            progress = storyMaker.getProgress();
                            isDone = storyMaker.isDone();
                        }
                        mProgressBar.setProgress((int) (progress * PROGRESS_MAX));
                    }
                    mProgressBar.setProgress(PROGRESS_MAX);

                    onCancelExport(null);
                }
            }.start();
        }
        toggleButtons();
    }

    public void onCancelExport(View view) {
        synchronized (storyMakerLock) {
            storyMaker.close();
            storyMaker = null;
        }
        toggleButtons();
    }
}

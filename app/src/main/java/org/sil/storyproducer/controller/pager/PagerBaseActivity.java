package org.sil.storyproducer.controller.pager;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.DrawerItemClickListener;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;


/**
 * The activty that is the base of the paging views
 */
public class PagerBaseActivity extends AppCompatActivity {

    private PagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private GestureDetectorCompat mDetector;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager_base);

        //keeps the screen from going to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new CircularViewPagerHandler(mViewPager));       //sets the change listener to be the circular handler
        mViewPager.setCurrentItem(StoryState.getCurrentStorySlide());           //sets view pager to the current slide from the story state

        //get the current phase
        Phase phase = StoryState.getCurrentPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources()
                                                                                    , phase.getColor(), null)));
        //setup the menu drawer
        setupDrawer();

        // set up the gesture detector
        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));
    }

    @Override
    public void onPause() {
        super.onPause();
        StoryState.setCurrentStorySlide( mViewPager.getCurrentItem());      //sets the story state to the current item before the activity is left
    }

    /**
     * initializes the items that the drawer needs
     */
    private void setupDrawer() {
        //TODO maybe take this code off into somewhere so we don't have to duplicate it as much
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerList.bringToFront();
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        addDrawerItems();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this));
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.nav_open, R.string.dummy_content) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
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

}

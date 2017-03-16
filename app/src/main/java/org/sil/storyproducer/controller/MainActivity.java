package org.sil.storyproducer.controller;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Looper;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import org.sil.storyproducer.R;
import org.sil.storyproducer.model.NavItem;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.FileSystem;

import java.io.Serializable;

import org.sil.storyproducer.tools.media.story.AutoStoryMaker;
import org.sil.storyproducer.tools.media.story.SampleStory;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Serializable {
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileSystem.init(getApplicationContext());
        StoryState.init(getApplicationContext());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new StoryFrag()).commit();
//        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_bg_trans, getTheme()));
        setupNavDrawer();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        boolean skipRegistration = checkRegistrationSkip();
        if (!skipRegistration) {
            // Checks registration file to see if email has been sent and launches registration if it hasn't
            SharedPreferences prefs = getSharedPreferences(getString(R.string.registration_filename), MODE_PRIVATE);
            Map<String, ?> preferences = prefs.getAll();
            Object registrationComplete = preferences.get(RegistrationActivity.EMAIL_SENT);
            if (registrationComplete == null || !(Boolean)registrationComplete) {
                Intent intent = new Intent(this, RegistrationActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        if(drawerOpen) {
            menu.findItem(R.id.menu_lang).setVisible(false);
            menu.findItem(R.id.menu_play).setVisible(true);
        } else {
            menu.findItem(R.id.menu_lang).setVisible(true);
            menu.findItem(R.id.menu_play).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_story_templates, menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        if(item.getItemId() == R.id.menu_lang){
            Snackbar.make(getCurrentFocus(), "Languages", Snackbar.LENGTH_LONG).show();
        }
            return super.onOptionsItemSelected(item);
    }

    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ArrayList<NavItem> mNavItems = new ArrayList<>();
    private String mActivityTitle;

    private void setupNavDrawer(){
        mActivityTitle = getTitle().toString();
        String[] aNavTitles = getResources().getStringArray(R.array.nav_labels);
        TypedArray aNavIcons = getResources().obtainTypedArray(R.array.nav_icons);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.nav_layout);
        mDrawerList = (ListView) findViewById(R.id.nav_list);

        for (int i = 0; i < aNavTitles.length; i++) {
            mNavItems.add(new NavItem(aNavTitles[i], aNavIcons.getResourceId(i, -1)));
        }
        aNavIcons.recycle();

        NavItemAdapter adapter = new NavItemAdapter(getApplicationContext(), mNavItems);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Pages");
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(id == 5) {
                AutoStoryMaker encodeThread = new AutoStoryMaker("Joshua Sends Spies Rahab");
                encodeThread.toggleLogProgress(true);
                encodeThread.toggleText(true);
                encodeThread.start();
            }
            mDrawerLayout.closeDrawer(mDrawerList);
            if(id == 5) {
                Toast.makeText(getBaseContext(), "Starting video creation. Please hold.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * move to the chosen story
     */
    public void switchToStory(String storyName) {
        //TODO change the Story State that is stored for each story
        StoryState.setStoryName(storyName);
        Phase currPhase = StoryState.getCurrentPhase();
        Intent intent = new Intent(this.getApplicationContext(), currPhase.getTheClass());
        startActivity(intent);
    }

    /**
     * Checks the bundle variables to see if the user has bypassed registration
     * @return true if they want to bypass registration, false if not
     */
    private boolean checkRegistrationSkip() {
        // Check to see if registration was skipped by the user
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(RegistrationActivity.SKIP_KEY)) {
            return true;
        } else {
            return false;
        }
    }
}


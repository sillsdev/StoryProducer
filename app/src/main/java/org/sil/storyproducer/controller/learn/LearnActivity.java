package org.sil.storyproducer.controller.learn;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.DrawerItemClickListener;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;
import org.sil.storyproducer.tools.file.ImageFiles;

public class LearnActivity extends AppCompatActivity {

    private RelativeLayout rootView;
    private ImageView learnImageView;
    private ImageButton playButton;
    private SeekBar videoSeekBar;
    private AudioPlayer narrationPlayer;
    private AudioPlayer backgroundPlayer;
    private int slideNum = 0;
    private String storyName;
    private boolean isVolumeOn = true;
    private boolean isWatchedOnce = false;
    private float backgroundVolume = 0.4f;
    private GestureDetectorCompat mDetector;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);
        rootView = (RelativeLayout) findViewById(R.id.activity_learn);

        //get the story name
        storyName = StoryState.getStoryName();

        //get the ui
        learnImageView = (ImageView) findViewById(R.id.learnImageView);
        playButton = (ImageButton) findViewById(R.id.playButton);
        videoSeekBar = (SeekBar) findViewById(R.id.videoSeekBar);

        //get the current phase
        Phase phase = StoryState.getCurrentPhase();

        Toolbar mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ResourcesCompat.getColor(getResources(),
                                                                                    phase.getColor(), null)));
        setupDrawer();

        setSeekBarListener();
        playVideo();
        setBackgroundMusic();

        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));

    }

    /**
     * Sets up the background music player
     */
    private void setBackgroundMusic() {
        //turn on the background music
        backgroundPlayer = new AudioPlayer();
        backgroundPlayer.playWithPath(AudioFiles.getSoundtrack(storyName).getPath());
        backgroundPlayer.setVolume(backgroundVolume);
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

    @Override
    public void onStop() {
        super.onStop();
        narrationPlayer.releaseAudio();
        backgroundPlayer.releaseAudio();
    }

    @Override
    public void onPause() {
        super.onPause();
        narrationPlayer.pauseAudio();
        backgroundPlayer.pauseAudio();
    }

    @Override
    public void onResume() {
        super.onResume();
        narrationPlayer.resumeAudio();
        backgroundPlayer.resumeAudio();
    }


    /**
     * Plays the video and runs everytime the audio is completed
     */
    void playVideo() {
        //TODO: sync background audio with image
        setPic(learnImageView);                                                             //set the next image
        narrationPlayer = new AudioPlayer();                                                //set the next audio
        narrationPlayer.playWithPath(AudioFiles.getLWC(storyName, slideNum).getPath());
        if(isVolumeOn) {
            narrationPlayer.setVolume(1.0f);
        } else {
            narrationPlayer.setVolume(0.0f);
        }
        videoSeekBar.setProgress(slideNum);
        narrationPlayer.audioCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(slideNum < ImageFiles.getAmount(storyName)) {
                    playVideo();
                } else {
                    videoSeekBar.setProgress(ImageFiles.getAmount(storyName) - 1);
                    backgroundPlayer.releaseAudio();
                    showStartPracticeSnackBar();
                }
            }
        });
        slideNum++;         //move to the next slide
    }

    /**
     * Button actin for playing/pausing the audio
     * @param view
     */
    public void onClickPlayPauseButton(View view) {
        if(narrationPlayer.isAudioPlaying()) {
            narrationPlayer.pauseAudio();
            backgroundPlayer.pauseAudio();
            playButton.setImageResource(R.drawable.ic_play_gray);
        } else {
            narrationPlayer.resumeAudio();
            backgroundPlayer.resumeAudio();
            playButton.setImageResource(R.drawable.ic_pause_gray);
        }
    }

    /**
     * Sets the seekBar listener for the video seek bar
     */
    private void setSeekBarListener() {
        videoSeekBar.setMax(ImageFiles.getAmount(storyName) - 1);      //set the bar to have as many markers as images
        videoSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar sBar){
            }
            @Override
            public void onStartTrackingTouch(SeekBar sBar){
            }
            @Override
            public void onProgressChanged(SeekBar sBar, int progress, boolean fromUser) {
                if(fromUser) {
                    boolean notPlayingAudio = false;
                    notPlayingAudio = !narrationPlayer.isAudioPlaying();
                    narrationPlayer.releaseAudio();
                    slideNum = progress;
                    playVideo();
                    if(notPlayingAudio) narrationPlayer.pauseAudio();
                }
            }
        });
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user that they should practice saying the story
     */
    private void showStartPracticeSnackBar() {
        if(!isWatchedOnce) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout_learn),
                    R.string.learn_phase_practice, Snackbar.LENGTH_INDEFINITE);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.lightWhite, null));
            TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.darkGray, null));
            snackbar.setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //reset the story with the volume off
                    videoSeekBar.setProgress(0);
                    slideNum = 0;
                    narrationPlayer.setVolume(0.0f);
                    setBackgroundMusic();
                    backgroundPlayer.setVolume(0.0f);
                    isVolumeOn = false;
                    playVideo();
                    setVolumeSwitchVisible();
                }
            });
            snackbar.show();
        }
        isWatchedOnce = true;
    }

    /**
     * Makes the volume switch visible so it can be used
     */
    private void setVolumeSwitchVisible() {
        ImageView soundOff = (ImageView) findViewById(R.id.soundOff);
        ImageView soundOn = (ImageView) findViewById(R.id.soundOn);
        Switch volumeSwitch = (Switch) findViewById(R.id.volumeSwitch);
        soundOff.setVisibility(View.VISIBLE);
        soundOn.setVisibility(View.VISIBLE);
        volumeSwitch.setVisibility(View.VISIBLE);
        volumeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    narrationPlayer.setVolume(1.0f);
                    backgroundPlayer.setVolume(backgroundVolume);
                    isVolumeOn = true;
                } else {
                    narrationPlayer.setVolume(0.0f);
                    backgroundPlayer.setVolume(0.0f);
                    isVolumeOn = false;
                }
            }
        });
    }

    /**
     * initializes the items that the drawer needs
     */
    private void setupDrawer() {
        //TODO maybe take this code off into somewhere so we don't have to duplicate it as much
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerList = (ListView)findViewById(R.id.navList_learn);
        mDrawerList.bringToFront();
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout_learn);
        addDrawerItems();
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener(getApplicationContext()));
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
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     */
    private void setPic(View aView) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.4;
        height = (int)(height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

}

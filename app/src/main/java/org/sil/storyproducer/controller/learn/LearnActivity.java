package org.sil.storyproducer.controller.learn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.Phase;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AnimationToolbar;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.DrawerItemClickListener;
import org.sil.storyproducer.tools.PhaseGestureListener;
import org.sil.storyproducer.tools.PhaseMenuItemListener;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LearnActivity extends AppCompatActivity {

    private final static float BACKGROUND_VOLUME = 0.0f;        //makes for no background music but still keeps the functionality in there if we decide to change it later

    private RelativeLayout rootView;
    private ImageView learnImageView;
    private ImageButton playButton;
    private SeekBar videoSeekBar;
    private AudioPlayer narrationPlayer;
    private AudioPlayer backgroundPlayer;
    private int slideNum = 0;
    private int CONTENT_SLIDE_COUNT = 0;
    private String storyName;
    private boolean isVolumeOn = true;
    private boolean isWatchedOnce = false;
    private GestureDetectorCompat mDetector;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ArrayList<Integer> backgroundAudioJumps;

    //recording and playback
    private AudioPlayer voiceAudioPlayer;
    private String recordFilePath;
    private MediaRecorder voiceRecorder;
    private boolean isRecording = false;
    private boolean isFirstTime = true;         //used to know if it is the first time the activity is started up for playing the vid

    //recording animation bar
    private AnimationToolbar myToolbar = null;
    private TransitionDrawable transitionDrawable;
    private Handler colorHandler;
    private Runnable colorHandlerRunnable;
    private boolean isRed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);
        rootView = (RelativeLayout) findViewById(R.id.activity_learn);

        //get the story name
        storyName = StoryState.getStoryName();
        CONTENT_SLIDE_COUNT = FileSystem.getContentSlideAmount(storyName);

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
        setBackgroundAudioJumps();

        setSeekBarListener();

        //create audio players
        narrationPlayer = new AudioPlayer();
        backgroundPlayer = new AudioPlayer();

        setPic(learnImageView);     //set the first image to show

        //setup the recording animation bar
        setupToolbarAndRecordAnim(rootView.findViewById(R.id.fab),
                rootView.findViewById(R.id.fragment_draft_animated_toolbar),
                rootView.findViewById(R.id.fragment_draft_mic_toolbar_button));
        setRecordNPlayback(rootView.findViewById(R.id.fragment_draft_mic_toolbar_button),
                rootView.findViewById(R.id.fragment_draft_play_toolbar_button));

        mDetector = new GestureDetectorCompat(this, new PhaseGestureListener(this));

    }

    /**
     * Sets up the background music player
     */
    private void setBackgroundMusic() {
        //turn on the background music
        backgroundPlayer = new AudioPlayer();
        backgroundPlayer.playWithPath(AudioFiles.getSoundtrack(storyName).getPath());
        backgroundPlayer.setVolume(BACKGROUND_VOLUME);
    }

    /**
     * Sets the array list for all the jump points that the background music has to make
     */
    private void setBackgroundAudioJumps() {
        int audioStartValue = 0;
        backgroundAudioJumps = new ArrayList<Integer>();
        backgroundAudioJumps.add(0, audioStartValue);
        for(int k = 0; k < CONTENT_SLIDE_COUNT; k++) {
            String lwcPath = AudioFiles.getLWC(storyName, k).getPath();
            audioStartValue += MediaHelper.getAudioDuration(lwcPath) / 1000;
            backgroundAudioJumps.add(k, audioStartValue);
        }
        backgroundAudioJumps.add(audioStartValue);        //this last one is just added for the copyrights slide
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
        pauseVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * Plays the video and runs everytime the audio is completed
     */
    void playVideo() {
        setPic(learnImageView);                                                             //set the next image

        //Clear old narrationPlayer
        if(narrationPlayer != null) {
            narrationPlayer.releaseAudio();
        }
        narrationPlayer = new AudioPlayer();                                                //set the next audio
        narrationPlayer.playWithPath(AudioFiles.getLWC(storyName, slideNum).getPath());
        narrationPlayer.setVolume((isVolumeOn)? 1.0f : 0.0f);       //set the volume on or off based on the boolean
        videoSeekBar.setProgress(slideNum);
        narrationPlayer.audioCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(slideNum < CONTENT_SLIDE_COUNT) {     //not at the end of video
                    playVideo();
                } else {                            //at the end of video so special case
                    videoSeekBar.setProgress(CONTENT_SLIDE_COUNT);
                    backgroundPlayer.releaseAudio();
                    narrationPlayer.releaseAudio();
                    playButton.setImageResource(R.drawable.ic_play_gray);
                    setPic(learnImageView);     //sets the pic to the end image
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
            pauseVideo();
        } else {
            playButton.setImageResource(R.drawable.ic_pause_gray);
            if(slideNum >= CONTENT_SLIDE_COUNT) {        //reset the video to the beginning because they already finished it
                videoSeekBar.setProgress(0);
                slideNum = 0;
                setBackgroundMusic();
                playVideo();
            } else {
               resumeVideo();
            }
        }
    }

    /**
     * helper function for pausing the video
     */
    private void pauseVideo() {
        narrationPlayer.pauseAudio();
        backgroundPlayer.pauseAudio();
        playButton.setImageResource(R.drawable.ic_play_gray);
    }

    /**
     * helper function for resuming the video
     */
    private void resumeVideo() {
        if(isFirstTime) {           //actually start playing the video if playVideo() has never been called
            playVideo();
            isFirstTime = false;
        } else {
            narrationPlayer.resumeAudio();
            backgroundPlayer.resumeAudio();
        }
    }

    /**
     * Sets the seekBar listener for the video seek bar
     */
    private void setSeekBarListener() {
        videoSeekBar.setMax(CONTENT_SLIDE_COUNT);      //set the progress bar to have as many markers as images
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
                    narrationPlayer.releaseAudio();             //clear the two audios because they have to be restarted
                    backgroundPlayer.releaseAudio();
                    slideNum = progress;
                    setBackgroundMusic();       //have to reset the background music because it could have been completed
                    if (notPlayingAudio) backgroundPlayer.pauseAudio();
                    backgroundPlayer.seekTo(backgroundAudioJumps.get(slideNum));
                    if(slideNum == CONTENT_SLIDE_COUNT) {
                        backgroundPlayer.releaseAudio();
                        playButton.setImageResource(R.drawable.ic_play_gray);
                        setPic(learnImageView);     //sets the pic to the end image
                        showStartPracticeSnackBar();
                        narrationPlayer = new AudioPlayer();    //create new player so there is one that exists
                    } else {
                        playVideo();
                    }
                    if (notPlayingAudio) narrationPlayer.pauseAudio();
                }
            }
        });
    }

    /**
     * helper function that resets the vidio to the beginning and turns off the sound
     */
    private void resetVideoWithSoundOff() {
        playButton.setImageResource(R.drawable.ic_pause_gray);
        videoSeekBar.setProgress(0);
        slideNum = 0;
        narrationPlayer = new AudioPlayer();
        narrationPlayer.setVolume(0.0f);
        setBackgroundMusic();
        backgroundPlayer.setVolume(0.0f);
        isVolumeOn = false;
        playVideo();
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
                    resetVideoWithSoundOff();
                    setVolumeSwitchAndFloatingButtonVisible();
                }
            });
            snackbar.show();
        }
        isWatchedOnce = true;
    }

    /**
     * Makes the volume switch visible so it can be used
     */
    private void setVolumeSwitchAndFloatingButtonVisible() {
        //make the floating button visible
        FloatingActionButton floatingActionButton = (FloatingActionButton)rootView.findViewById(R.id.fab);
        floatingActionButton.setVisibility(View.VISIBLE);
        //make the sounds stuff visible
        ImageView soundOff = (ImageView) findViewById(R.id.soundOff);
        ImageView soundOn = (ImageView) findViewById(R.id.soundOn);
        Switch volumeSwitch = (Switch) findViewById(R.id.volumeSwitch);
        soundOff.setVisibility(View.VISIBLE);
        soundOn.setVisibility(View.VISIBLE);
        volumeSwitch.setVisibility(View.VISIBLE);
        //set the volume switch change listener
        volumeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    narrationPlayer.setVolume(1.0f);
                    backgroundPlayer.setVolume(BACKGROUND_VOLUME);
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
        if(slideNum == CONTENT_SLIDE_COUNT) {                //gets the end image if we are at the end of the story
            slidePicture = ImageFiles.getBitmap(storyName, ImageFiles.COPYRIGHT);
        }

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


    //all the recording stuff is below
    /**
     * This function sets the recording and playback buttons (The mic and play button) with their
     * respective functionalities.
     */
    private void setRecordNPlayback(View recordButt, View playRecordingButt) {
        if (recordButt == null || playRecordingButt == null) {
            return;
        }

        recordFilePath = AudioFiles.getLearnPractice(StoryState.getStoryName()).getPath();
        setVoicePlayBackButton(new File(recordFilePath).exists());

        final ImageButton recordButton = (ImageButton) recordButt;
        final ImageButton playRecordingButton = (ImageButton) playRecordingButt;

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop all other playback streams.
                if (voiceAudioPlayer != null) {
                    voiceAudioPlayer.stopAudio();
                    voiceAudioPlayer.releaseAudio();
                }
                if (isRecording) {
                    stopAudioRecorder();
                    stopRecordingAnimation();
                    //set playback button visible
                    playRecordingButton.setVisibility(View.VISIBLE);

                } else {
                    //setupRecordinganimationHandler() should be called first
                    //to initialize the colorHandler and colorHandlerRunnable().
                    startRecordingAnimation(false, 0);
                    startAudioRecorder();
                    //set the switch off and turn off volume
                    Switch volumeSwitch = (Switch) findViewById(R.id.volumeSwitch);
                    volumeSwitch.setChecked(false);
                    //start the video at the beginning
                    resetVideoWithSoundOff();
                }
            }
        });
    }

    /**
     * The function that aids in starting an audio recorder.
     */
    private void startAudioRecorder() {
        setVoiceRecorder(recordFilePath, voiceRecorder != null);
        try {
            voiceRecorder.prepare();
            voiceRecorder.start();
            isRecording = true;
            Toast.makeText(getApplicationContext(), "Recording voice!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The function that aids in stopping an audio recorder.
     */
    private void stopAudioRecorder() {
        try {
            voiceRecorder.stop();
            isRecording = false;
            Toast.makeText(getApplicationContext(), "Stopped recording!", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException stopException) {
            Toast.makeText(getApplicationContext(), "Please record again!", Toast.LENGTH_SHORT).show();
        }
        voiceRecorder.reset();
    }


    /**
     * This function sets the voice recorder with either a new voicerecorder or reuses the
     * voicerecorder.
     *
     * @param fileName               The file to output the voice recordings.
     * @param createNewMediaRecorder The boolean that dictates an new instantiation of a
     *                               voice recorder.
     */
    private void setVoiceRecorder(String fileName, boolean createNewMediaRecorder) {
        if (createNewMediaRecorder || voiceRecorder == null) {
            voiceRecorder = new MediaRecorder();
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }
        voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        voiceRecorder.setOutputFile(fileName);
    }

    /**
     * This function sets the voice play back function. This function is called
     * in private void setRecordNPlayback(). This serves to set the visibility if the audio file
     * already exists.
     *
     * @param audioFileExists The boolean to check if the recording file exists.
     */
    private void setVoicePlayBackButton(boolean audioFileExists) {
        ImageButton playbackButton = (ImageButton) rootView.findViewById(R.id.fragment_draft_play_toolbar_button);
        if (audioFileExists) {
            playbackButton.setVisibility(View.VISIBLE);
        }
        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stops all other playback streams.
                if (voiceAudioPlayer != null) {
                    voiceAudioPlayer.stopAudio();
                    voiceAudioPlayer.releaseAudio();
                }
                if(isRecording){
                    stopAudioRecorder();
                    if(myToolbar != null){
                        stopRecordingAnimation();
                    }
                }
                voiceAudioPlayer = new AudioPlayer();
                voiceAudioPlayer.playWithPath(recordFilePath);
                //set the switch off and turn off volume
                Switch volumeSwitch = (Switch) findViewById(R.id.volumeSwitch);
                volumeSwitch.setChecked(false);
                //start the video at the beginning
                resetVideoWithSoundOff();
                Toast.makeText(getApplicationContext(), "Playing back recording!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * This function initializes all of the buttons associates with the toolbar.
     */
    private void setupToolbarAndRecordAnim(View fab, View relativeLayout, View micToolbarButton) {
        if (fab == null || relativeLayout == null || micToolbarButton == null) {
            return;
        }
        try {
            myToolbar = new AnimationToolbar(fab, relativeLayout, this);
        } catch (ClassCastException ex) {
            System.out.println(ex.toString());
        }

        //The following allows for a touch from user to close the toolbar and make the fab visible.
        //This also stops the recording animation.
        RelativeLayout clickView = (RelativeLayout) rootView.findViewById(R.id.click_view_layout);
        clickView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myToolbar != null && myToolbar.isOpen() && !isRecording) {
                    stopRecordingAnimation();
                    myToolbar.close();
                }
            }
        });

        /*This function must be called before using record animations i.e. calling
        * setRecordNPlayback();*/
        setupRecordingAnimationHandler();
    }


    /**
     * <a href="https://developer.android.com/reference/android/os/Handler.html">See for handler</a>
     * <br/>
     * <a href="https://developer.android.com/reference/java/lang/Runnable.html">See for runnable</a>
     * <br/>
     * <a href="https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable.html">See for transition Drawable</a>
     * <br/>
     * <br/>
     * Call this function prior to setting the button listener of the record button. E.g.: <br/>
     * setupRecordAnimationHandler();<br/>
     * button.Handler(){}
     * <br/>
     * Essentially the function utilizes a Transition Drawable to interpolate between the red and
     * the toolbar color. (The colors are defined in an array and used in the transition drawable)
     * To schedule the running of the transition drawable a handler and runnable are used.
     * The handler takes a runnable which schedules the transitiondrawable. The handler function
     * called postDelayed will delay the running of the next Runnable by the passed in value e.g.:
     * colorHandler.postDelayed(runnable goes here, time delay in MS).
     * <br/>
     * Still confused about handlers, runnables, and the MessageQueue?
     * <br/>
     * <a href="http://stackoverflow.com/questions/12877944/what-is-the-relationship-between-looper-handler-and-messagequeue-in-android">See this excellent SA post for more info.</a>
     */
    private void setupRecordingAnimationHandler() {
        int red = Color.rgb(255, 0, 0);
        int colorOfToolbar = Color.rgb(255, 0, 0); /*Arbitrary color value of red used initially*/

        RelativeLayout rel = (RelativeLayout) rootView.findViewById(R.id.fragment_draft_animated_toolbar);
        Drawable relBackgroundColor = rel.getBackground();
        if (relBackgroundColor instanceof ColorDrawable) {
            colorOfToolbar = ((ColorDrawable) relBackgroundColor).getColor();
        }
        transitionDrawable = new TransitionDrawable(new ColorDrawable[]{
                new ColorDrawable(colorOfToolbar),
                new ColorDrawable(red)
        });
        rel.setBackground(transitionDrawable);

        colorHandler = new Handler();
        colorHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                //Animation to change the toolbar
                //wait 1.5 seconds between color transitions
                if (isRed) {
                    transitionDrawable.startTransition(1500);
                    isRed = false;

                } else {
                    transitionDrawable.reverseTransition(1500);
                    isRed = true;
                }
                startRecordingAnimation(true, 1500);
            }
        };
    }

    /**
     * This function is used to start the handler to run the runnable.
     *
     * @param isDelayed Used to signify that the runnable will be delayed in running.
     * @param delay The time that will be delayed in ms if isDelayed is true.
     */
    private void startRecordingAnimation(boolean isDelayed, long delay) {
        if (colorHandler != null && colorHandlerRunnable != null) {
            if (isDelayed) {
                colorHandler.postDelayed(colorHandlerRunnable, delay);
            } else {
                colorHandler.post(colorHandlerRunnable);
            }
        }
    }

    /**
     * Stops the animation from continuing. The removeCallbacks function removes all
     * colorHandlerRunnable from the MessageQueue and also resets the toolbar to its original color.
     * (transitionDrawable.resetTransition();)
     */
    private void stopRecordingAnimation() {
        if (colorHandler != null && colorHandlerRunnable != null) {
            colorHandler.removeCallbacks(colorHandlerRunnable);
            transitionDrawable.resetTransition();
        }
    }

}

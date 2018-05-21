package org.sil.storyproducer.controller.remote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.FileSystem;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.media.MediaHelper;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by annmcostantino on 1/14/2018.
 */

public class WholeStoryBackTranslationActivity extends PhaseBaseActivity {

    private final static float BACKGROUND_VOLUME = 0.0f;        //makes for no background music but still keeps the functionality in there if we decide to change it later

    private RelativeLayout rootView;
    private ImageView wStoryImageView;
    private ImageButton playButton;
    private SeekBar videoSeekBar;
    private AudioPlayer narrationPlayer;
    private AudioPlayer backgroundPlayer;
    private boolean backgroundAudioExists;

    private int slideNumber = 0;
    private int CONTENT_SLIDE_COUNT = 0;
    private String storyName;
    private boolean isVolumeOn = true;
    private List<Integer> backgroundAudioJumps;

    //recording toolbar vars
    private String recordFilePath;
    private RecordingToolbar recordingToolbar;

    private boolean isFirstTime = true;         //used to know if it is the first time the activity is started up for playing the vid
    private int startPos = -1;
    private long startTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whole_story);
        rootView = findViewById(R.id.phase_frame);

        //get the story name
        storyName = StoryState.getStoryName();
        CONTENT_SLIDE_COUNT = FileSystem.getContentSlideAmount(storyName);

        //get the ui
        wStoryImageView = findViewById(R.id.wholeStoryImageView);
        playButton = findViewById(R.id.playButton);
        videoSeekBar = findViewById(R.id.videoSeekBar);

        setBackgroundAudioJumps();

        setSeekBarListener();

        setPic(wStoryImageView);     //set the first image to show

        //set the recording toolbar stuffs
        recordFilePath = AudioFiles.getWholeStory(StoryState.getStoryName()).getPath();
        View rootViewToolbar = getLayoutInflater().inflate(R.layout.toolbar_for_recording, rootView, false);
        setToolbar(rootViewToolbar);
        invalidateOptionsMenu();
        setVolumeSwitchAndFloatingButtonVisible();
        recordingToolbar.keepToolbarVisible();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_wholestory);
        return true;
    }

    /**
     * Starts the background music player
     */
    private void playBackgroundMusic() {
        if (backgroundAudioExists) {
            backgroundPlayer.playAudio();
        }
    }

    /**
     * Sets the array list for all the jump points that the background music has to make
     */
    private void setBackgroundAudioJumps() {
        int audioStartValue = 0;
        backgroundAudioJumps = new ArrayList<>();
        backgroundAudioJumps.add(0, audioStartValue);
        for(int k = 0; k < CONTENT_SLIDE_COUNT; k++) {
            String lwcPath = AudioFiles.getLWC(storyName, k).getPath();
            audioStartValue += MediaHelper.getAudioDuration(lwcPath) / 1000;
            backgroundAudioJumps.add(k, audioStartValue);
        }
        backgroundAudioJumps.add(audioStartValue);        //this last one is just added for the copyrights slide
    }

    public void onStart() {
        super.onStart();
        //create audio players
        narrationPlayer = new AudioPlayer();
        narrationPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                slideNumber++;         //move to the next slide
                if(slideNumber < CONTENT_SLIDE_COUNT) {     //not at the end of video
                    playVideo();
                } else {                            //at the end of video so special case
                    //makeLogIfNecessary(true);

                    videoSeekBar.setProgress(CONTENT_SLIDE_COUNT);
                    playButton.setImageResource(R.drawable.ic_play_gray);
                    setPic(wStoryImageView);     //sets the pic to the end image
                }
            }
        });
        //recordingToolbar.hideFloatingActionButton();

        backgroundPlayer = new AudioPlayer();
        backgroundPlayer.setVolume(BACKGROUND_VOLUME);
        File backgroundAudioFile = AudioFiles.getSoundtrack(StoryState.getStoryName());
        if (backgroundAudioFile.exists()) {
            backgroundAudioExists = true;
            backgroundPlayer.setPath(backgroundAudioFile.getPath());
        } else {
            backgroundAudioExists = false;
        }
    }

    /*private void markLogStart() {
        startPos = slideNumber;
        startTime = System.currentTimeMillis();
    }

    private void makeLogIfNecessary(){
        makeLogIfNecessary(false);
    }

    private void makeLogIfNecessary(boolean request){
        if(narrationPlayer.isAudioPlaying() || backgroundPlayer.isAudioPlaying()
                || request){
            if(startPos!=-1) {
                WholeStoryEntry.saveFilteredLogEntry(startPos, slideNumber,
                        System.currentTimeMillis() - startTime);
                startPos=-1;
            }
        }
    }*/

    @Override
    public void onPause() {
        super.onPause();
        pauseVideo();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
            recordingToolbar.closeToolbar();
        }
    }

    @Override
    public void onResume() {
            super.onResume();
            //recordingToolbar.hideFloatingActionButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        narrationPlayer.release();
        backgroundPlayer.release();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
            recordingToolbar.closeToolbar();
            recordingToolbar.releaseToolbarAudio();
        }

    }

    /**
     * Plays the video and runs every time the audio is completed
     */
    void playVideo() {
        setPic(wStoryImageView);                                                             //set the next image
        File audioFile = AudioFiles.getDraft(storyName, slideNumber);
        //set the next audio
        if (audioFile.exists()) {
            narrationPlayer.setVolume((isVolumeOn)? 1.0f : 0.0f); //set the volume on or off based on the boolean
            narrationPlayer.setPath(audioFile.getPath());
            narrationPlayer.playAudio();
        }

        videoSeekBar.setProgress(slideNumber);
    }

    /**
     * Button action for playing/pausing the audio
     * @param view button to set listeners for
     */
    public void onClickPlayPauseButton(View view) {
        if(narrationPlayer.isAudioPlaying()) {
            pauseVideo();

        }
        //if no draft audio exists
        else if(!AudioFiles.allDraftsComplete(storyName,FileSystem.getContentSlideAmount(storyName))){
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(this.getString(R.string.wsbt_alert_title))
                    .setMessage(this.getString(R.string.wsbt_alert_text))
                    .setPositiveButton(this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    }).create();
            dialog.show();
        } else {
            //markLogStart();

            playButton.setImageResource(R.drawable.ic_pause_gray);

            if(slideNumber >= CONTENT_SLIDE_COUNT) {        //reset the video to the beginning because they already finished it
                videoSeekBar.setProgress(0);
                slideNumber = 0;
                playBackgroundMusic();
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
        //makeLogIfNecessary();
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
            if (backgroundAudioExists) {
                backgroundPlayer.resumeAudio();
            }
        }
    }

    /**
     * Sets the seekBar listener for the video seek bar
     */
    private void setSeekBarListener() {
        videoSeekBar.setMax(CONTENT_SLIDE_COUNT);      //set the progress bar to have as many markers as images
        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar sBar){
            }
            @Override
            public void onStartTrackingTouch(SeekBar sBar){
            }
            @Override
            public void onProgressChanged(SeekBar sBar, int progress, boolean fromUser) {
                if(fromUser) {
                    //makeLogIfNecessary();

                    slideNumber = progress;
                    narrationPlayer.stopAudio();
                    if(backgroundAudioExists) {
                        backgroundPlayer.seekTo(backgroundAudioJumps.get(slideNumber));
                        if (!backgroundPlayer.isAudioPlaying()) {
                            backgroundPlayer.resumeAudio();
                        }
                    }
                    if(slideNumber == CONTENT_SLIDE_COUNT) {
                        playButton.setImageResource(R.drawable.ic_play_gray);
                        setPic(wStoryImageView);     //sets the pic to the end image
                    } else {
                        //markLogStart();
                        playVideo();
                        playButton.setImageResource(R.drawable.ic_pause_gray);
                    }

                }
            }
        });
    }

    /**
     * helper function that resets the video to the beginning and turns off the sound
     */
    private void resetVideoWithSoundOff() {

        videoSeekBar.setProgress(0);
        slideNumber = 0;
        narrationPlayer.setVolume(0.0f);
        Switch volumeSwitch = findViewById(R.id.volumeSwitch);
        backgroundPlayer.stopAudio();
        volumeSwitch.setChecked(false);
        backgroundPlayer.stopAudio();
        backgroundPlayer.setVolume(0.0f);
        playBackgroundMusic();
        isVolumeOn = false;

       //markLogStart();
        if(AudioFiles.allDraftsComplete(storyName,FileSystem.getContentSlideAmount(storyName))){
            playButton.setImageResource(R.drawable.ic_pause_gray);
            playVideo();
        }
    }

    /**
     * Makes the volume switch visible so it can be used
     */
    private void setVolumeSwitchAndFloatingButtonVisible() {
        //make the floating button visible
        //recordingToolbar.showFloatingActionButton();
        //make the sounds stuff visible
        ImageView soundOff = findViewById(R.id.soundOff);
        ImageView soundOn = findViewById(R.id.soundOn);
        Switch volumeSwitch = findViewById(R.id.volumeSwitch);
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
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     */
    private void setPic(View aView) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNumber);
        if(slideNumber == CONTENT_SLIDE_COUNT) {                //gets the end image if we are at the end of the story
            slidePicture = ImageFiles.getBitmap(storyName, ImageFiles.COPYRIGHT);
        }

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture", Snackbar.LENGTH_SHORT).show();
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

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar){
        recordingToolbar = new RecordingToolbar(this, toolbar, rootView, true, false, false, true, recordFilePath, recordFilePath, null, new RecordingToolbar.RecordingListener() {
            @Override
            public void onStoppedRecording() {

            }
            @Override
            public void onStartedRecordingOrPlayback(boolean isRecording) {
                resetVideoWithSoundOff();
            }
        });
        //recordingToolbar.hideFloatingActionButton();
        //The following allows for a touch from user to close the toolbar and make the fab visible.
        //This does not stop the recording
        /*RelativeLayout dummyView = (RelativeLayout) rootView.findViewById(R.id.activity_wholestorybacktranslation);
        dummyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recordingToolbar != null && recordingToolbar.isOpen() && !recordingToolbar.isRecording()) {
                    recordingToolbar.keepToolbarVisible();
                    recordingToolbar.hideFloatingActionButton();
                }
            }
        });*/
    }



}


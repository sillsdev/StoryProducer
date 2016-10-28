package org.sil.storyproducer.controller.learn;

import android.media.MediaPlayer;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.View;
import android.view.KeyEvent;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.content.Intent;
import android.widget.SeekBar;
import android.support.design.widget.Snackbar;

import org.sil.storyproducer.R;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.FileSystem;

public class LearnActivity extends AppCompatActivity {

    private ImageView learnImageView;
    private ImageButton playButton;
    private PopupWindow pWindow;
    private SeekBar videoSeekBar;
    private AudioPlayer aPlayer;
    private AudioPlayer backgroundPlayer;
    private int slideNum = 0;
    private String storyName;
    private boolean isVolumeOn = true;
    private boolean isWatchedOnce = false;
    private float backgroundVolume = 0.4f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        //get the story name
        Intent intent = getIntent();
        storyName = intent.getStringExtra("storyname");

        //get the ui
        learnImageView = (ImageView) findViewById(R.id.learnImageView);
        playButton = (ImageButton) findViewById(R.id.playButton);
        videoSeekBar = (SeekBar) findViewById(R.id.videoSeekBar);

        setSeekBarListener();
        playVideo();
        setBackgroundMusic();

    }

    /**
     * Sets up the background music player
     */
    private void setBackgroundMusic() {
        //turn on the background music
        backgroundPlayer = new AudioPlayer();
        backgroundPlayer.playWithPath(FileSystem.getStoryPath(storyName) + "/SoundTrack0.mp3");
        backgroundPlayer.setVolume(backgroundVolume);
    }

    /**
     * Back button event for API's greater than 5
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        aPlayer.stopAudio();
        backgroundPlayer.stopAudio();
    }

    /**
     * Back button event for API's less than 5
     * @param keyCode
     * @param event
     * @return returns the event
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            aPlayer.stopAudio();
            backgroundPlayer.stopAudio();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Plays the video and runs everytime the audio is completed
     */
    void playVideo() {
        learnImageView.setImageBitmap(FileSystem.getImage(storyName, slideNum));          //set the next image
        aPlayer = new AudioPlayer();                                                //set the next audio
        aPlayer.playWithPath(FileSystem.getStoryPath(storyName) + "/narration" + slideNum + ".wav");
        if(isVolumeOn) {
            aPlayer.setVolume(1.0f);
        } else {
            aPlayer.setVolume(0.0f);
        }
        videoSeekBar.setProgress(slideNum);
        aPlayer.audioCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(slideNum < FileSystem.getImageAmount(storyName)) {
                    playVideo();
                } else {
                    videoSeekBar.setProgress(FileSystem.getImageAmount(storyName) - 1);
                    backgroundPlayer.stopAudio();
                    showSnackBar();
                }
            }
        });
        slideNum++;         //move to the next slide
    }

    /**
     * Button actin for playing/pausing the audio
     * @param view
     */
    public void clickPlayPauseButton(View view) {
        if(aPlayer.isAudioPlaying()) {
            aPlayer.pauseAudio();
            backgroundPlayer.pauseAudio();
            playButton.setImageResource(R.drawable.ic_play_gray);
        } else {
            aPlayer.resumeAudio();
            backgroundPlayer.resumeAudio();
            playButton.setImageResource(R.drawable.ic_pause_gray);
        }
    }

    /**
     * Sets the seekBar listener for the video seek bar
     */
    private void setSeekBarListener() {
        videoSeekBar.setMax(FileSystem.getImageAmount(storyName) - 1);      //set the bar to have as many markers as images
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
                    if(!aPlayer.isAudioPlaying()) notPlayingAudio = true;
                    aPlayer.stopAudio();
                    slideNum = progress;
                    playVideo();
                    if(notPlayingAudio) aPlayer.pauseAudio();
                }
            }
        });
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user
     */
    private void showSnackBar() {
        if(!isWatchedOnce) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_learn),
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
                    aPlayer.setVolume(0.0f);
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
                    aPlayer.setVolume(1.0f);
                    backgroundPlayer.setVolume(backgroundVolume);
                    isVolumeOn = true;
                } else {
                    aPlayer.setVolume(0.0f);
                    backgroundPlayer.setVolume(0.0f);
                    isVolumeOn = false;
                }
            }
        });
    }

}

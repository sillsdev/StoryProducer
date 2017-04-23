package org.sil.storyproducer.tools.toolbar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.Space;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.draft.Modal;

import java.io.File;


/**
 *
 */
public class PausingRecordToolbar extends RecordingToolbar {
    private boolean enablePlayListCheckButton = true;
    private ImageButton playListCheckButton;

    public PausingRecordToolbar(Activity activity, View rootViewToolbarLayout, RelativeLayout rootViewLayout,
                                boolean enablePlaybackButton, boolean enableDeleteButton, boolean enableMultiRecordButton,
                                String playbackRecordFilePath, String recordFilePath, Modal multiRecordModal, RecordingListener recordingListener) throws ClassCastException {
        super(activity, rootViewToolbarLayout, rootViewLayout, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, playbackRecordFilePath, recordFilePath, multiRecordModal, recordingListener);
    }

    @Override
    protected void setupToolbarButtons() {
        rootViewToolbarLayout.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(0, 0, 1f);
        spaceLayoutParams.width = 0;
        int[] drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_record_voice_over_white_48dp, R.drawable.ic_playlist_add_white_48dp};
        ImageButton[] imageButtons = new ImageButton[]{new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext)};
        boolean[] buttonToDisplay = new boolean[]{true/*enable mic*/, enablePlaybackButton, enableMultiRecordButton, enablePlayListCheckButton};

        Space buttonSpacing = new Space(appContext);
        buttonSpacing.setLayoutParams(spaceLayoutParams);
        toolbar.addView(buttonSpacing); //Add a space to the left of the first button.
        for (int i = 0; i < drawables.length; i++) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i]);
                imageButtons[i].setVisibility(View.VISIBLE);
                imageButtons[i].setLayoutParams(layoutParams);
                toolbar.addView(imageButtons[i]);

                buttonSpacing = new Space(appContext);
                buttonSpacing.setLayoutParams(spaceLayoutParams);
                toolbar.addView(buttonSpacing);
                if (i == 0) {
                    micButton = imageButtons[i];
                } else if (i == 1) {
                    playButton = imageButtons[i];
                } else if (i == 2) {
                    multiRecordButton = imageButtons[i];
                } else if (i == 3) {
                    playListCheckButton = imageButtons[i];
                }
            }
        }

        if (playButton != null) {
            playButton.setVisibility((enablePlaybackButton && new File(playbackRecordFilePath).exists()) ? View.VISIBLE : View.INVISIBLE);
            multiRecordButton.setVisibility((enablePlaybackButton && new File(playbackRecordFilePath).exists()) ? View.VISIBLE : View.INVISIBLE);
        }
        if (playListCheckButton != null) {
            //TODO do something with default visibility check for writing to file if should be visible
        }

        setOnClickListeners();
    }

    @Override
    protected void startAudioRecorder() {

    }

    @Override
    protected void stopAudioRecorder() {

    }

    @Override
    protected void setOnClickListeners() {
        super.setOnClickListeners();

        View.OnClickListener micListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    stopRecording();
                    micButton.setBackgroundResource(R.drawable.ic_mic_white);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.VISIBLE);
                    }
                    if (enableMultiRecordButton) {
                        multiRecordButton.setVisibility(View.VISIBLE);
                    }
                    if (enablePlayListCheckButton) {
                        playListCheckButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    stopPlayBackAndRecording();
                    startRecording();
                    micButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.INVISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.INVISIBLE);
                    }
                    if (enableMultiRecordButton) {
                        multiRecordButton.setVisibility(View.INVISIBLE);
                    }
                    if (enablePlayListCheckButton) {
                        playListCheckButton.setVisibility(View.INVISIBLE);
                    }
                }
            }
        };
        micButton.setOnClickListener(micListener);

        View.OnClickListener checkListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //write final to phone and add to playlist

                //Delete the temp file and the wav file and add to playlist

                //make the button invisible till after the next new recording
                playListCheckButton.setVisibility(View.INVISIBLE);
            }
        };
        playListCheckButton.setOnClickListener(checkListener);
    }

    private void createNewAudioRecord() {
        if (ContextCompat.checkSelfPermission(currentActivity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(currentActivity,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }


    }


}

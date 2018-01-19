package org.sil.storyproducer.tools.toolbar;

import android.app.Activity;
import android.app.Application;
import android.support.v4.widget.Space;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.Network.BackTranslationUpload;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.wavaudio.WavAudioRecorder;
import org.sil.storyproducer.tools.media.wavaudio.WavFileConcatenator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * This class is used to extend the capabilities of the RecordingToolbar {@link RecordingToolbar}. <br/>
 * The purpose of this class is to add a pausing capability to the the recording toolbar. So, a
 * recording can have multiple appended recordings.
 */
public class PausingRecordingToolbar extends RecordingToolbar {
    private static final String TAG = "PauseRecordToolbar";
    private boolean enableCheckButton;
    private boolean isAppendingOn;
    private ImageButton checkButton;
    private WavAudioRecorder wavAudioRecorder;


    /**
     * Ctor
     *
     * @param activity                The activity from the calling class.
     * @param rootViewToolbarLayout   The rootViewToEmbedToolbarIn of the Toolbar layout called toolbar_for_recording.
     *                                must be of type LinearLayout so that buttons can be
     *                                evenly spaced.
     * @param rootViewLayout          The rootViewToEmbedToolbarIn of the layout that you want to embed the toolbar in.
     * @param enablePlaybackButton    Enable playback of recording.
     * @param enableDeleteButton      Enable the delete button, does not work as of now.
     * @param enableMultiRecordButton Enabled the play list button on the toolbar.
     * @param playbackRecordFilePath  The file path where the toolbar will play from.
     * @param recordFilePath          The file path that the recording will be saved under.
     * @param multiRecordModal        The modal that houses the multiple recordings.
     * @param recordingListener       The listener responsible for interactions between toolbar
     *                                and corresponding class. Used on stop and start of recording.
     */
    public PausingRecordingToolbar(Activity activity, View rootViewToolbarLayout, RelativeLayout rootViewLayout,
                                   boolean enablePlaybackButton, boolean enableDeleteButton, boolean enableMultiRecordButton,
                                   String playbackRecordFilePath, String recordFilePath, Modal multiRecordModal, RecordingListener recordingListener) throws ClassCastException {
        super(activity, rootViewToolbarLayout, rootViewLayout, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, playbackRecordFilePath, recordFilePath, multiRecordModal, recordingListener);

        wavAudioRecorder = new WavAudioRecorder(activity, new File(recordFilePath));
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    @Override
    public void stopToolbarMedia() {
        if (isRecording) {
            stopRecording();
            micButton.setBackgroundResource(R.drawable.ic_mic_black_append);
            //set playback button visible
            if (enableDeleteButton) {
                deleteButton.setVisibility(View.VISIBLE);
            }
            if (enablePlaybackButton) {
                playButton.setVisibility(View.VISIBLE);
            }
            if (enableMultiRecordButton) {
                multiRecordButton.setVisibility(View.VISIBLE);
            }
            if (enableCheckButton) {
                multiRecordButton.setVisibility(View.VISIBLE);
            }
        }
        if (audioPlayer != null && audioPlayer.isAudioPlaying()) {
            playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            audioPlayer.stopAudio();
        }
    }

    /*
    Stop the appending session for the toolbar.
     */
    public void stopAppendingSession() {
        if (isAppendingOn) {
            //simulate a finish recording session and set isAppendingOn to false
            checkButton.callOnClick();
        }
    }

    /**
     * Calling class should be responsible for all other media
     * so {@link #stopPlayBackAndRecording()} is not being used here.
     */
    @Override
    public void onClose() {
        if (isRecording) {
            //simulate a stop of recording.
            micButton.callOnClick();
        }
        //else stop other media from playing.
        stopToolbarMedia();
        stopAppendingSession();
    }

    /**
     * set the recording file path
     *
     * @param path to set the recording path to
     */
    public void setRecordFilePath(String path) {
        recordFilePath = path;
        wavAudioRecorder.setNewPath(new File(recordFilePath));
    }

    /**
     * Used to hide any buttons that need to be hidden
     */
    @Override
    public void hideButtons() {
        super.hideButtons();
        if (enableCheckButton) {
            checkButton.setVisibility(View.INVISIBLE);
        }
        if (isAppendingOn) {
            //simulate a finish recording session and set isAppendingOn to false
            checkButton.callOnClick();
        }
    }

    /**
     * Used to add buttons to the toolbar
     */
    @Override
    protected void setupToolbarButtons() {
        rootViewToolbarLayout.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(0, 0, 1f);
        spaceLayoutParams.width = 0;
        int[] drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_done_all_black_48dp};
        ImageButton[] imageButtons = new ImageButton[]{new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext), new ImageButton(appContext)};
        boolean[] buttonToDisplay = new boolean[]{true/*enable mic*/, enablePlaybackButton, enableMultiRecordButton, (enableCheckButton = true)};

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
                    checkButton = imageButtons[i];
                }
            }
        }

        boolean playBackFileExist = new File(playbackRecordFilePath).exists();
        if(enablePlaybackButton){
            playButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(enableMultiRecordButton){
            multiRecordButton.setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(enableCheckButton){
            checkButton.setVisibility((playBackFileExist && isAppendingOn) ? View.VISIBLE : View.INVISIBLE);
        }

        setOnClickListeners();
    }

    /**
     * Stop the wav audio recorder.
     */
    @Override
    protected void stopRecording() {
        isRecording = false;
        stopRecordingAnimation();
        wavAudioRecorder.stopRecording();
        if (isAppendingOn) {
            try {
                WavFileConcatenator.ConcatenateAudioFiles(new File(recordFilePath), AudioFiles.getDramatizationTemp(StoryState.getStoryName()));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Did not concatenate audio files", e);
            }
        } else {
            wavAudioRecorder.setNewPath(AudioFiles.getDramatizationTemp(StoryState.getStoryName()));
        }
    }

    /**
     * Start the audio recorder.
     */
    @Override
    protected void startAudioRecorder() {
        isRecording = true;
        wavAudioRecorder.startRecording();
    }

    /**
     * Add listeners to the buttons on the toolbar. This child class does change the
     * mic button and adds the check button listeners on top of calling the parent's class
     * onClickListeners.
     */
    @Override
    protected void setOnClickListeners() {
        super.setOnClickListeners();
        View.OnClickListener micListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    stopRecording();
                    if (!isAppendingOn) {
                        isAppendingOn = true;
                        checkButton.setVisibility(View.VISIBLE);
                    }
                    micButton.setBackgroundResource(R.drawable.ic_mic_black_append);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.VISIBLE);
                    }
                    if (enableMultiRecordButton) {
                        multiRecordButton.setVisibility(View.VISIBLE);
                    }
                    if (enableCheckButton) {
                        checkButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    stopPlayBackAndRecording();
                    startRecording();
                    if (!isAppendingOn) {
                        recordingListener.onStartedRecordingOrPlayback(true);
                    }
                    micButton.setBackgroundResource(R.drawable.ic_pause_white_48dp);
                    checkButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    if (enableDeleteButton) {
                        deleteButton.setVisibility(View.INVISIBLE);
                    }
                    if (enablePlaybackButton) {
                        playButton.setVisibility(View.INVISIBLE);
                    }
                    if (enableMultiRecordButton) {
                        multiRecordButton.setVisibility(View.INVISIBLE);
                    }
                    //if (enableCheckButton) {
                      //  checkButton.setVisibility(View.INVISIBLE);
                    //}
                }
            }
        };
        micButton.setOnClickListener(micListener);

        if (enableCheckButton) {
            View.OnClickListener checkListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Delete the temp file wav file
                    stopPlayBackAndRecording();
                    File tempFile = AudioFiles.getDramatizationTemp(StoryState.getStoryName());
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.delete();
                    }
                    recordingListener.onStoppedRecording();
                    //make the button invisible till after the next new recording
                    isAppendingOn = false;
                    checkButton.setVisibility(View.INVISIBLE);
                    micButton.setBackgroundResource(R.drawable.ic_mic_white);
                    //TODO: remove the commented stuff. left just incase
                  /*  if(StoryState.getCurrentPhase().getTitle().equals("Back Translation" )){
                       File playback = AudioFiles.getBackTranslation(StoryState.getStoryName(), StoryState.getCurrentStorySlide());
                      //  String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), StoryState.getCurrentStorySlide()).getName();
                        try {
                            BackTranslationUpload.Upload(playback, appContext.getApplicationContext());
                        }
                        catch(IOException e){
                            e.printStackTrace();
                        }
                    }*/
                }
            };

            checkButton.setOnClickListener(checkListener);
        }

    }
}

package org.sil.storyproducer.tools.toolbar;

import android.app.Activity;
import android.support.v4.widget.Space;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.Modal;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.media.wavaudio.WavAudioRecorder;
import org.sil.storyproducer.tools.media.wavaudio.WavFileConcatenator;

import java.io.File;
import java.io.FileNotFoundException;


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
     * @param enableSendAudioButton   Enable sending audio to server
     * @param playbackRecordFilePath  The file path where the toolbar will play from.
     * @param recordFilePath          The file path that the recording will be saved under.
     * @param multiRecordModal        The modal that houses the multiple recordings.
     * @param recordingListener       The listener responsible for interactions between toolbar
     *                                and corresponding class. Used on stop and start of recording.
     */
    public PausingRecordingToolbar(Activity activity, View rootViewToolbarLayout, RelativeLayout rootViewLayout,
                                   boolean enablePlaybackButton, boolean enableDeleteButton, boolean enableMultiRecordButton,boolean enableSendAudioButton,
                                   String playbackRecordFilePath, String recordFilePath, Modal multiRecordModal, RecordingListener recordingListener) throws ClassCastException {
        super(activity, rootViewToolbarLayout, rootViewLayout, enablePlaybackButton, enableDeleteButton, enableMultiRecordButton, enableSendAudioButton, multiRecordModal, recordingListener);

        wavAudioRecorder = new WavAudioRecorder(activity, new File(recordFilePath));
    }

    /**
     * This function is used to stop all the media sources on the toolbar from playing or recording.
     * The auxiliary medias are not stopped because the calling class should be responsible for
     * those.
     */
    @Override
    public void stopToolbarMedia() {
        if (isRecording()) {
            stopRecording();
            getMicButton().setBackgroundResource(R.drawable.ic_mic_black_append);
            //set playback button visible
            if (getEnableDeleteButton()) {
                getDeleteButton().setVisibility(View.VISIBLE);
            }
            if (getEnablePlaybackButton()) {
                getPlayButton().setVisibility(View.VISIBLE);
            }
            if (getEnableMultiRecordButton()) {
                getMultiRecordButton().setVisibility(View.VISIBLE);
            }
            if (enableCheckButton) {
                getMultiRecordButton().setVisibility(View.VISIBLE);
            }
            if(getEnableSendAudioButton()) {
                getSendAudioButton().setVisibility(View.VISIBLE);
            }
        }
        if (getAudioPlayer() != null && getAudioPlayer().isAudioPlaying()) {
            getPlayButton().setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            getAudioPlayer().stopAudio();
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
    public void onPause() {
        if (isRecording()) {
            //simulate a stop of recording.
            getMicButton().callOnClick();
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
        setRecordFilePath(path);
        //FIXME
//        wavAudioRecorder.setNewPath(new File(getRecordFilePath()));
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
        getRootViewToolbarLayout().removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams spaceLayoutParams = new LinearLayout.LayoutParams(0, 0, 1f);
        spaceLayoutParams.width = 0;
        int[] drawables = new int[]{R.drawable.ic_mic_white, R.drawable.ic_play_arrow_white_48dp, R.drawable.ic_playlist_play_white_48dp, R.drawable.ic_stop_white_48dp, R.drawable.ic_send_audio_48dp};
        ImageButton[] imageButtons = new ImageButton[]{new ImageButton(getAppContext()), new ImageButton(getAppContext()), new ImageButton(getAppContext()), new ImageButton(getAppContext()), new ImageButton(getAppContext())};
        boolean[] buttonToDisplay = new boolean[]{true/*enable mic*/, getEnablePlaybackButton(), getEnableMultiRecordButton(), (enableCheckButton = true), getEnableSendAudioButton()};

        Space buttonSpacing = new Space(getAppContext());
        buttonSpacing.setLayoutParams(spaceLayoutParams);
        getToolbar().addView(buttonSpacing); //Add a space to the left of the first button.
        for (int i = 0; i < drawables.length; i++) {
            if (buttonToDisplay[i]) {
                imageButtons[i].setBackgroundResource(drawables[i]);
                imageButtons[i].setVisibility(View.VISIBLE);
                imageButtons[i].setLayoutParams(layoutParams);
                getToolbar().addView(imageButtons[i]);

                buttonSpacing = new Space(getAppContext());
                buttonSpacing.setLayoutParams(spaceLayoutParams);
                getToolbar().addView(buttonSpacing);
                switch (i) {
                    case 0:
                        setMicButton(imageButtons[i]);
                        break;
                    case 1:
                        setPlayButton(imageButtons[i]);
                        break;
                    case 2:
                        setMultiRecordButton(imageButtons[i]);
                        break;
                    case 3:
                        checkButton = imageButtons[i];
                        break;
                    case 4:
                        setSendAudioButton(imageButtons[i]);
                        break;
                }
            }
        }

        //FIXME
        //boolean playBackFileExist = new File(getPlaybackRecordFilePath()).exists();
        boolean playBackFileExist = false;
        if(getEnablePlaybackButton()){
            getPlayButton().setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(getEnableMultiRecordButton()){
            getMultiRecordButton().setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }
        if(enableCheckButton){
            checkButton.setVisibility((playBackFileExist && isAppendingOn) ? View.VISIBLE : View.INVISIBLE);
        }
        if(getEnableSendAudioButton()){
            getSendAudioButton().setVisibility((playBackFileExist) ? View.VISIBLE : View.INVISIBLE);
        }

        setOnClickListeners();
    }

    /**
     * Stop the wav audio recorder.
     */
    @Override
    protected void stopRecording() {
        //setIsRecording(false);
        stopRecordingAnimation();
        wavAudioRecorder.stopRecording();
        if (isAppendingOn) {
            //FIXME
/*
            try {
                WavFileConcatenator.ConcatenateAudioFiles(new File(getRecordFilePath()), AudioFiles.INSTANCE.getDramatizationTemp(StoryState.getStoryName()));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Did not concatenate audio files", e);
            }
*/
        } else {
            wavAudioRecorder.setNewPath(AudioFiles.INSTANCE.getDramatizationTemp(StoryState.getStoryName()));
        }
    }

    /**
     * Start the audio recorder.
     */
    //FIXME
    //@Override
    protected void startAudioRecorder() {
        //FIXME
        //setIsRecording(true);
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
                if (isRecording()) {
                    stopRecording();
                    if (!isAppendingOn) {
                        isAppendingOn = true;
                        checkButton.setVisibility(View.VISIBLE);
                    }
                    getMicButton().setBackgroundResource(R.drawable.ic_mic_black_append);
                    if (getEnableDeleteButton()) {
                        getDeleteButton().setVisibility(View.VISIBLE);
                    }
                    if (getEnablePlaybackButton()) {
                        getPlayButton().setVisibility(View.VISIBLE);
                    }
                    if (getEnableMultiRecordButton()) {
                        getMultiRecordButton().setVisibility(View.VISIBLE);
                    }
                    if (enableCheckButton) {
                        checkButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    stopPlayBackAndRecording();
                    //FIXME
                    //startRecording();
                    if (!isAppendingOn) {
                        getRecordingListener().onStartedRecordingOrPlayback(true);
                    }
                    getMicButton().setBackgroundResource(R.drawable.ic_pause_white_48dp);
                    //checkButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    if (getEnableDeleteButton()) {
                        getDeleteButton().setVisibility(View.INVISIBLE);
                    }
                    if (getEnablePlaybackButton()) {
                        getPlayButton().setVisibility(View.INVISIBLE);
                    }
                    if (getEnableMultiRecordButton()) {
                        getMultiRecordButton().setVisibility(View.INVISIBLE);
                    }
                    if(getEnableSendAudioButton()){
                        getSendAudioButton().setVisibility(View.INVISIBLE);
                    }
                    //if (enableCheckButton) {
                      //  checkButton.setVisibility(View.INVISIBLE);
                    //}
                }
            }
        };
        getMicButton().setOnClickListener(micListener);

        if (enableCheckButton) {
            View.OnClickListener checkListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Delete the temp file wav file
                    stopPlayBackAndRecording();
                    File tempFile = AudioFiles.INSTANCE.getDramatizationTemp(StoryState.getStoryName());
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.delete();
                    }
                    getRecordingListener().onStoppedRecording();
                    //make the button invisible till after the next new recording
                    isAppendingOn = false;
                    checkButton.setVisibility(View.INVISIBLE);
                    getMicButton().setBackgroundResource(R.drawable.ic_mic_white);
                    if(getEnableSendAudioButton()){
                        getSendAudioButton().setVisibility(View.VISIBLE);
                    }
                }
            };

            checkButton.setOnClickListener(checkListener);
        }

    }
}

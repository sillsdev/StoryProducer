package org.sil.storyproducer.controller.dramatization;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener;

import java.io.File;


public class DramatizationFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    private View rootView;
    private View rootViewToolbar;
    private int slideNumber;
    private EditText slideText;
    private String storyName;
    private boolean phaseUnlocked;
    private AudioPlayer draftPlayer;
    private boolean draftAudioExists;
    private File dramatizationRecordingFile = null;
    private ImageButton draftPlayButton;


    private PausingRecordingToolbar recordingToolbar;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        storyName = StoryState.getStoryName();
        phaseUnlocked = StorySharedPreferences.isApproved(storyName, getContext());
        setRecordFilePath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false);
        draftPlayButton = (ImageButton)rootView.findViewById(R.id.fragment_dramatization_play_draft_button);
        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_dramatization_image_view), slideNumber);
        TextView slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + 1 + "");
        slideText = (EditText)rootView.findViewById(R.id.fragment_dramatization_edit_text);
        slideText.setText(TextFiles.getDramatizationText(StoryState.getStoryName(), slideNumber), TextView.BufferType.EDITABLE);

        if (phaseUnlocked) {
            rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);
            closeKeyboardOnTouch(rootView);
            rootView.findViewById(R.id.lock_overlay).setVisibility(View.INVISIBLE);
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView);
        }
        return rootView;
    }

    public void onStart() {
        super.onStart();

        if (phaseUnlocked) {
            setToolbar(rootViewToolbar);
        }

        draftPlayer = new AudioPlayer();
        File draftAudioFile = AudioFiles.getDraft(storyName, slideNumber);
        if (draftAudioFile.exists()) {
            draftAudioExists = true;
            draftPlayer.setPath(draftAudioFile.getPath());
        } else {
            draftAudioExists = false;
        }
        draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                draftPlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
            }
        });

        setPlayStopDraftButton((ImageButton)rootView.findViewById(R.id.fragment_dramatization_play_draft_button));
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
        }
        closeKeyboard(rootView);
        TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText.getText().toString());
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        draftPlayer.release();
        if(recordingToolbar != null){
            recordingToolbar.onClose();
            recordingToolbar.releaseToolbarAudio();
        }

        closeKeyboard(rootView);
        TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText.getText().toString());
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar.onClose();
                }
                closeKeyboard(rootView);
                TextFiles.setDramatizationText(StoryState.getStoryName(), slideNumber, slideText.getText().toString());
            }
        }
    }

    /**
     * Used to stop playing and recording any media. The calling class should be responsible for
     * stopping its own media. Used in {@link DramaListRecordingsModal}.
     */
    public void stopPlayBackAndRecording() {
        recordingToolbar.stopToolbarMedia();
    }

    /**
     * Used to hide the play and multiple recordings button.
     */
    public void hideButtonsToolbar(){
        recordingToolbar.hideButtons();
    }

    /**
     * Stop the toolbar from continuing the appending session.
     */
    public void stopAppendingRecordingFile(){
        recordingToolbar.stopAppendingSession();
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_dramatization_root_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function is used to the set the picture per slide.
     *
     * @param slideImage    The view that will have the picture rendered on it.
     * @param slideNum The respective slide number for the dramatization slide.
     */
    private void setPic(final ImageView slideImage, int slideNum) {

        Bitmap slidePicture = ImageFiles.getBitmap(storyName, slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, R.string.dramatization_draft_no_picture, Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.4;
        height = (int) (height * scalingFactor);

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * sets the playback path
     */
    public void setPlayBackPath() {
        String playBackFilePath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * This function serves to set the play and stop button for the draft playback button.
     */

    private void setPlayStopDraftButton(final ImageButton playPauseDraftButton) {

        if (!draftAudioExists) {
            //draft recording does not exist
            playPauseDraftButton.setAlpha(0.8f);
            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200));
        } else {
            //remove x mark from ImageButton play
            playPauseDraftButton.setImageResource(0);
        }
        playPauseDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!draftAudioExists){
                    Toast.makeText(getContext(), R.string.dramatization_no_draft_recording_available, Toast.LENGTH_SHORT).show();
                }
                else if (draftPlayer.isAudioPlaying()) {
                    draftPlayer.stopAudio();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                } else {
                    recordingToolbar.stopToolbarMedia();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_stop_white_48dp);
                    draftPlayer.playAudio();

                    if(draftPlayer != null){ //if there is a draft available to play
                        recordingToolbar.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_arrow_white_48dp, draftPlayer);
                    }
                    Toast.makeText(getContext(), R.string.dramatization_playback_draft_recording, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setRecordFilePath() {
        int nextDraftIndex = AudioFiles.getDramatizationTitles(StoryState.getStoryName(), slideNumber).length + 1;
        File recordFile = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber,getString(R.string.dramatization_record_file_drama_name, nextDraftIndex));
        while (recordFile.exists()) {
            nextDraftIndex++;
            recordFile = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber, getString(R.string.dramatization_record_file_drama_name, nextDraftIndex));
        }
        dramatizationRecordingFile = recordFile;
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar) {
        if (rootView instanceof RelativeLayout) {
            String playBackFilePath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).getPath();
            RecordingListener recordingListener = new RecordingListener() {
                @Override
                public void onStoppedRecording() {
//                    String[] splitPath = dramatizationRecordingPath.split("dramatization" + "\\d+" + "_");    //get just the title from the path
//                    String title = splitPath[1].replace(".wav", "");
//                    StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
//                    //update to new recording path
//                    setRecordFilePath();
//                    recordingToolbar.setRecordFilePath(dramatizationRecordingPath);
//                    //update to old recording or whatever was set by StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
//                    setPlayBackPath();
                    //update to new recording path
                    setRecordFilePath();
                    recordingToolbar.setRecordFilePath(dramatizationRecordingFile.getAbsolutePath());
                }
                @Override
                public void onStartedRecordingOrPlayback(boolean isRecording) {
                    if(isRecording) {
                        String title = AudioFiles.getDramatizationTitle(dramatizationRecordingFile);
                        StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                        //update to old recording or whatever was set by StorySharedPreferences.setDramatizationForSlideAndStory(title, slideNumber, StoryState.getStoryName());
                        setPlayBackPath();
                    }
                }
            };
            DramaListRecordingsModal modal = new DramaListRecordingsModal(getContext(), slideNumber, this);

            recordingToolbar = new PausingRecordingToolbar(getActivity(), toolbar, (RelativeLayout)rootView,
                    true, false, true, playBackFilePath, dramatizationRecordingFile.getAbsolutePath(), modal,recordingListener);
            recordingToolbar.keepToolbarVisible();
        }
    }

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: {@link #closeKeyboard(View)}.
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     *                    the view will close the softkeyboard.
     */
    private void closeKeyboardOnTouch(final View touchedView) {
        if (touchedView != null) {
            touchedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeKeyboard(touchedView);
                }
            });
        }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     *
     */
    private void closeKeyboard(View viewToFocus) {
        if(viewToFocus != null){
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(viewToFocus.getWindowToken(), 0);
            viewToFocus.requestFocus();
        }
    }

}

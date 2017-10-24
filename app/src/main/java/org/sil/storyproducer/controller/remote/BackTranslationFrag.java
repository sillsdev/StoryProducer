package org.sil.storyproducer.controller.remote;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import org.sil.storyproducer.controller.dramatization.DramaListRecordingsModal;
import org.sil.storyproducer.controller.phase.PhaseBaseActivity;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;

import java.io.File;

/**
 * Created by alexamhepner on 10/23/17.
 */

public class BackTranslationFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    private View rootView;
    private View rootViewToolbar;
    private int slideNumber;
    private EditText slideText;
    private String storyName;
    private boolean phaseUnlocked;
    private AudioPlayer draftPlayer;
    private boolean draftAudioExists;
    private File backTranslationRecordingFile = null;
    private ImageButton draftPlayButton;

    private RecordingToolbar recordingToolbar;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        storyName = StoryState.getStoryName();
        //phaseUnlocked = StorySharedPreferences.isApproved(storyName, getContext());
        setRecordFilePath();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_backtranslation, container, false);

        draftPlayButton = (ImageButton)rootView.findViewById(R.id.fragment_backtranslation_play_draft_button);
        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_backtranslation_image_view), slideNumber);
        TextView slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + "");

        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);
        closeKeyboardOnTouch(rootView);


        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item =  menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
        item.setIcon(R.drawable.ic_dramatize);
    }

    public void onStart() {
        super.onStart();

        setToolbar(rootViewToolbar);

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

        setPlayStopDraftButton((ImageButton)rootView.findViewById(R.id.fragment_backtranslation_play_draft_button));
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
     * sets the playback path
     */
    public void updatePlayBackPath() {
        String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * Stop the toolbar from continuing the appending session.
     */
  //  public void stopAppendingRecordingFile(){
   //     recordingToolbar.stopAppendingSession();
   // }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_backtranslation_root_layout);
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
            Snackbar.make(rootView, R.string.backTranslation_draft_no_picture, Snackbar.LENGTH_SHORT).show();
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
        String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
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
                    Toast.makeText(getContext(), R.string.backTranslation_no_draft_recording_available, Toast.LENGTH_SHORT).show();
                }
                else if (draftPlayer.isAudioPlaying()) {
                    draftPlayer.stopAudio();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp);
                } else {
                    recordingToolbar.stopToolbarMedia();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_pause_white_48dp);
                    draftPlayer.playAudio();

                    if(draftPlayer != null){ //if there is a draft available to play
                        recordingToolbar.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_arrow_white_48dp, draftPlayer);
                    }
                    Toast.makeText(getContext(), R.string.backTranslation_playback_draft_recording, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setRecordFilePath() {
        int nextDraftIndex = AudioFiles.getBackTranslationTitles(StoryState.getStoryName(), slideNumber).length + 1;
        File recordFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber,getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
        while (recordFile.exists()) {
            nextDraftIndex++;
            recordFile = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber, getString(R.string.backTranslation_record_file_backT_name, nextDraftIndex));
        }
        backTranslationRecordingFile = recordFile;
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar) {
        if (rootView instanceof RelativeLayout) {
            String playBackFilePath = AudioFiles.getBackTranslation(StoryState.getStoryName(), slideNumber).getPath();
            RecordingToolbar.RecordingListener recordingListener = new RecordingToolbar.RecordingListener() {
                @Override
                public void onStoppedRecording() {
                    String title = AudioFiles.getBackTranslationTitle(backTranslationRecordingFile);
                    StorySharedPreferences.setBackTranslationForSlideAndStory(title, slideNumber, StoryState.getStoryName());     //save the draft  title for the recording
                    setRecordFilePath();
                    recordingToolbar.setRecordFilePath(backTranslationRecordingFile.getAbsolutePath());
                    updatePlayBackPath();
                }

                @Override
                public void onStartedRecordingOrPlayback(boolean isRecording) {
                    //not used here
                }
            };
            BackTranslationListRecordingsModal modal = new BackTranslationListRecordingsModal(getContext(), slideNumber, this);

            recordingToolbar = new RecordingToolbar(getActivity(), toolbar, (RelativeLayout) rootView,
                    true, false, true, playBackFilePath, backTranslationRecordingFile.getAbsolutePath(), modal , recordingListener);
            recordingToolbar.keepToolbarVisible();
            recordingToolbar.stopToolbarMedia();
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
    
    


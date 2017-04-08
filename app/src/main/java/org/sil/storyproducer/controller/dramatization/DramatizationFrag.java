package org.sil.storyproducer.controller.dramatization;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;

public class DramatizationFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    private View rootView;
    private int slideNumber;
    private ImageButton playPauseDraftButton;
    private TextView slideNumberText;
    private AudioPlayer draftPlayer;
    private String draftPlayerPath = null;
    private String dramatizationRecordingPath = null;

    private RecordingToolbar recordingToolbar;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        if (AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).exists()) {
            draftPlayerPath =  AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        }
        dramatizationRecordingPath = AudioFiles.getDramatization(StoryState.getStoryName(), slideNumber).getPath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false);
        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_dramatization_image_view), slideNumber);
        setPlayStopDraftButton(rootView.findViewById(R.id.fragment_dramatization_play_draft_button));
        View rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);
        setToolbar(rootViewToolbar);
        slideNumberText = (TextView) rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + 1 + "");

        return rootView;
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.closeToolbar();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        if(recordingToolbar != null){
            recordingToolbar.closeToolbar();
        }
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (recordingToolbar != null) {
                    recordingToolbar.closeToolbar();
                }
            }
        }
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
     * @param aView    The view that will have the picture rendered on it.
     * @param slideNum The respective slide number for the dramatization slide.
     */
    private void setPic(View aView, int slideNum) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum);

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
     * This function serves to set the play and stop button for the draft playback button.
     */
    private void setPlayStopDraftButton(View playPauseDraftBut) {
        View button = playPauseDraftBut;
        if (button != null && button instanceof ImageButton) {
            playPauseDraftButton = (ImageButton) button;
        }
        if (draftPlayerPath == null) {
            //draft recording does not exist
            playPauseDraftButton.setAlpha(0.8f);
            playPauseDraftButton.setColorFilter(Color.argb(200, 200, 200, 200));
        }else{
            //remove x mark from Imagebutton play
            playPauseDraftButton.setImageResource(0);
        }

        draftPlayer = new AudioPlayer();

        playPauseDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(draftPlayerPath == null){
                    Toast.makeText(getContext(), R.string.dramatization_no_draft_recording_available, Toast.LENGTH_SHORT).show();
                }
                else if (draftPlayer.isAudioPlaying()) {
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_gray);
                    draftPlayer.stopAudio();
                    draftPlayer.releaseAudio();
                } else {
                    recordingToolbar.stopToolbarMedia();
                    playPauseDraftButton.setBackgroundResource(R.drawable.ic_stop_gray);
                    draftPlayer = new AudioPlayer();
                    draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            playPauseDraftButton.setBackgroundResource(R.drawable.ic_play_gray);
                            draftPlayer.releaseAudio();
                        }
                    });
                    if(draftPlayer != null){ //if there is a draft available to play
                        recordingToolbar.onToolbarTouchStopAudio(playPauseDraftButton, R.drawable.ic_play_gray, draftPlayer);
                    }
                    draftPlayer.playWithPath(draftPlayerPath);
                    Toast.makeText(getContext(), R.string.dramatization_playback_draft_recording, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar){
        if(rootView instanceof RelativeLayout){
            recordingToolbar = new RecordingToolbar(getActivity(), toolbar, (RelativeLayout)rootView, true, false, dramatizationRecordingPath);
            recordingToolbar.keepToolbarVisible();
        }
    }
}

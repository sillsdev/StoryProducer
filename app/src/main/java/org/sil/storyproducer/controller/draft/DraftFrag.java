package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.logging.DraftEntry;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.StorySharedPreferences;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.LogFiles;
import org.sil.storyproducer.tools.file.TextFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar;
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener;

import java.io.File;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public class DraftFrag extends Fragment {
    private View rootView;
    private View rootViewToolbar;
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private String storyName;
    private int slideNumber;
    private SlideText slideText;

    private AudioPlayer LWCAudioPlayer;
    private File recordFile;
    private boolean LWCAudioExists;
    private ImageButton LWCPlayButton;


    private RecordingToolbar recordingToolbar;

    public DraftFrag() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        storyName = StoryState.getStoryName();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        slideText = TextFiles.getSlideText(storyName, slideNumber);
        setRecordFilePath();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);
        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false);

        LWCPlayButton = rootView.findViewById(R.id.fragment_draft_lwc_audio_button);

        LWCPlayButton = rootView.findViewById(R.id.fragment_draft_lwc_audio_button);

        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_draft_image_view), slideNumber);
        setScriptureText((TextView)rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText((TextView)rootView.findViewById(R.id.fragment_draft_reference_text));
        setLWCAudioButton(LWCPlayButton);
        TextView slideNumberText = rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + "");

        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        MenuItem item =  menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
        item.setIcon(R.drawable.ic_draft);
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
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
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setToolbar(rootViewToolbar);

        LWCAudioPlayer = new AudioPlayer();
        File LWCFile = AudioFiles.INSTANCE.getNarration(storyName, slideNumber);
        if (LWCFile.exists()) {
            LWCAudioExists = true;
            LWCAudioPlayer.setSource(LWCFile.getPath());
        } else {
            LWCAudioExists = false;
        }

        LWCAudioPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                LWCPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
            }
        });
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();

        recordingToolbar.stopToolbarMedia();
        LWCAudioPlayer.release();
     
        if (recordingToolbar != null) {
            recordingToolbar.onClose();
            recordingToolbar.releaseToolbarAudio();
        }

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
        String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
        recordingToolbar.setPlaybackRecordFilePath(playBackFilePath);
    }

    /**
     * Stops the toolbar from recording or playing back media.
     * Used in {@link DraftListRecordingsModal}
     */
    public void stopPlayBackAndRecording() {
        recordingToolbar.stopToolbarMedia();
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slideNumber == 0) {
            RelativeLayout rl = rootView.findViewById(R.id.fragment_draft_root_relayout_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = rootView.findViewById(R.id.fragment_draft_envelope);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = rootView.findViewById(R.id.fragment_draft_text_envelope);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));


            TextView tv = rootView.findViewById(R.id.fragment_draft_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = rootView.findViewById(R.id.fragment_draft_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
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
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(final TextView textView) {
        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private void setReferenceText(final TextView textView) {
        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }
        textView.setText(R.string.draft_bible_story);
    }

    /**
     * This function sets the LWC playback to the correct audio file. Also, the LWC narration
     * button will have a listener added to it in order to detect playback when pressed.
     *
     * @param LWCPlayButton the button to set the listeners for
     */
    private void setLWCAudioButton(final ImageButton LWCPlayButton) {
        LWCPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (!LWCAudioExists) {
                Snackbar.make(rootView, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show();
            } else {
                if(LWCAudioPlayer.isAudioPlaying()){
                    LWCAudioPlayer.stopAudio();
                    LWCPlayButton.setBackgroundResource(R.drawable.ic_menu_play);
                } else {
                    //stop other playback streams.
                    recordingToolbar.stopToolbarMedia();
                    LWCAudioPlayer.playAudio();
                    if (recordingToolbar != null) {
                        recordingToolbar.onToolbarTouchStopAudio(LWCPlayButton, R.drawable.ic_menu_play, LWCAudioPlayer);
                    }

                    LWCPlayButton.setBackgroundResource(R.drawable.ic_stop_white_36dp);
                    Toast.makeText(getContext(), R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show();
                    LogFiles.saveLogEntry(DraftEntry.Type.LWC_PLAYBACK.makeEntry());
                }
            }
            }
        });
    }

    private void setRecordFilePath() {
        int nextDraftIndex = AudioFiles.INSTANCE.getDraftTitles(StoryState.getStoryName(), slideNumber).length + 1;
        File recordFile = AudioFiles.INSTANCE.getDraft(StoryState.getStoryName(), slideNumber, getString(R.string.draft_record_file_draft_name, nextDraftIndex));
        while (recordFile.exists()) {
            nextDraftIndex++;
            recordFile = AudioFiles.INSTANCE.getDraft(StoryState.getStoryName(), slideNumber, getString(R.string.draft_record_file_draft_name, nextDraftIndex));
        }
        this.recordFile = recordFile;
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(View toolbar) {
        if (rootView instanceof RelativeLayout) {
            String playBackFilePath = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber).getPath();
            RecordingListener recordingListener = new RecordingListener() {
                @Override
                public void onStoppedRecording() {
                    String title = AudioFiles.INSTANCE.getDraftTitle(recordFile);
                    StorySharedPreferences.setDraftForSlideAndStory(title, slideNumber, StoryState.getStoryName());     //save the draft  title for the recording
                    setRecordFilePath();
                    recordingToolbar.setRecordFilePath(recordFile.getAbsolutePath());
                    updatePlayBackPath();
                }

                @Override
                public void onStartedRecordingOrPlayback(boolean isRecording) {
                    //not used here
                }
            };
            DraftListRecordingsModal modal = new DraftListRecordingsModal(getContext(), slideNumber, this);

            recordingToolbar = new RecordingToolbar(getActivity(), toolbar, (RelativeLayout) rootView,
                    true, false, true, false, playBackFilePath, recordFile.getAbsolutePath(), modal , recordingListener);
            recordingToolbar.keepToolbarVisible();
            recordingToolbar.stopToolbarMedia();
        }
    }
}

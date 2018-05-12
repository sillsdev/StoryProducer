package org.sil.storyproducer.controller.community;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.model.logging.ComChkEntry;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.file.AudioFiles;
import org.sil.storyproducer.tools.file.ImageFiles;
import org.sil.storyproducer.tools.file.LogFiles;
import org.sil.storyproducer.tools.media.AudioPlayer;
import org.sil.storyproducer.tools.media.AudioRecorder;

import java.io.File;
import java.io.IOException;

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
public class CommunityCheckFrag extends Fragment implements RecordingsListAdapter.ClickListeners {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private final static String LOG_TAG = "CommunityCheckFrag";
    private int slideNumber;
    private AudioPlayer draftPlayer;
    private ImageButton draftPlaybackButton;
    private AudioPlayer commentPlayer;
    private ImageButton commentButtonClicked;
    private MediaRecorder commentRecorder;
    private ImageButton commentRecordButton;
    private View rootView;
    private String[] comments;
    private boolean isRecording;
    private boolean draftAudioExists;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        slideNumber = passedArgs.getInt(SLIDE_NUM);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_community_check, container, false);
        draftPlaybackButton = rootView.findViewById(R.id.fragment_draft_playback_button);
        commentRecordButton = rootView.findViewById(R.id.fragment_commcheck_add_comment_button);

        updateCommentList();
        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_commcheck_image_view), slideNumber);
        setDraftPlaybackButton(draftPlaybackButton);
        setRecordCommentButton(commentRecordButton);
        TextView slideNumberText = rootView.findViewById(R.id.slide_number_text);
        slideNumberText.setText(slideNumber + "");

        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item =  menu.getItem(0);
        item.setIcon(R.drawable.ic_comcheck);
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     * @param isVisibleToUser whether fragment is visible to user anymore
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                stopAllMedia();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        commentPlayer = new AudioPlayer();
        draftPlayer = new AudioPlayer();
        final File draftFile = AudioFiles.getDraft(StoryState.getStoryName(), slideNumber);
        if (draftFile.exists()) {
            draftAudioExists = true;
            draftPlayer.setPath(draftFile.getPath());
        } else {
            draftAudioExists = false;
        }
        draftPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //TODO: use non-deprecated method; currently used to support older devices
                draftPlaybackButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
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
        stopAllMedia();
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        stopAllMedia();
        draftPlayer.release();
        commentPlayer.release();
    }

    /**
     * Updates the list of comments at beginning of fragment creation and after any list change
     */
    public void updateCommentList() {
        ListView listView = rootView.findViewById(R.id.audio_comment_list_view);
        listView.setScrollbarFadingEnabled(false);
        comments = AudioFiles.getCommentTitles(StoryState.getStoryName(), slideNumber);
        RecordingsListAdapter adapter = new RecordingsListAdapter(getContext(), comments, slideNumber, this);
        adapter.setDeleteTitle(getResources().getString(R.string.delete_comment_title));
        adapter.setDeleteMessage(getResources().getString(R.string.delete_comment_message));
        listView.setAdapter(adapter);
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slideNumber == 0){
            RelativeLayout rl = rootView.findViewById(R.id.fragment_commcheck_Relative_Layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(ImageView slideImage, int slideNum) {
        Bitmap slidePicture = ImageFiles.getBitmap(StoryState.getStoryName(), slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
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
     * This function sets the draft playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param button the ImageButton view handler to set the onclicklistener to
     */
    private void setDraftPlaybackButton(final ImageButton button) {
        //TODO: use non-deprecated method; currently used to support older devices
        button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop other playback streams

                boolean wasPlaying = draftPlayer.isAudioPlaying();
                stopAllMedia();
                if (draftAudioExists && !wasPlaying) {
                    draftPlayer.playAudio();
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_stop_red, null));
                    Toast.makeText(getContext(), "Playing Draft Audio", Toast.LENGTH_SHORT).show();
                    LogFiles.saveLogEntry(ComChkEntry.Type.DRAFT_PLAYBACK.makeEntry());
                } else if (wasPlaying) {
                    //TODO: use non-deprecated method; currently used to support older devices
                    button.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
                } else {
                    Toast.makeText(getContext(), "No Draft Audio Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRowClick(String recordingTitle) {
        //empty because Community check doesn't use this feature
    }

    /**
     * Plays the audio comment designated by the title
     * @param commentTitle the title of the comment to play
     */
    @Override
    public void onPlayClick(String commentTitle, final ImageButton buttonClickedNow) {
        final File commentFile = AudioFiles.getComment(StoryState.getStoryName(), slideNumber, commentTitle);

        boolean wasPlaying = commentPlayer.isAudioPlaying();

        // Different play button clicked while other still playing
        // Sets old button back to play image and sets was playing so new comment will still play
        if (wasPlaying && !buttonClickedNow.equals(commentButtonClicked)) {
            commentButtonClicked.setImageResource(R.drawable.ic_green_play);
            wasPlaying = false;
        }
        stopAllMedia();
        commentButtonClicked = buttonClickedNow;
        if (commentFile.exists() && !wasPlaying) {
            commentPlayer.setPath(commentFile.getPath());
            commentPlayer.playAudio();
            buttonClickedNow.setImageResource(R.drawable.ic_stop_red);
            commentPlayer.onPlayBackStop(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    buttonClickedNow.setImageResource(R.drawable.ic_green_play);
                }
            });
            Toast.makeText(getContext(), "Playing Comment", Toast.LENGTH_SHORT).show();
            LogFiles.saveLogEntry(ComChkEntry.Type.COMMENT_PLAYBACK.makeEntry());
        } else if (wasPlaying) {
            commentPlayer.stopAudio();
            buttonClickedNow.setImageResource(R.drawable.ic_green_play);
        } else {
            Toast.makeText(getContext(), "No Comment Found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(String commentTitle) {
        AudioFiles.deleteComment(StoryState.getStoryName(), slideNumber, commentTitle);
        updateCommentList();
    }

    @Override
    public AudioFiles.RenameCode onRenameClick(String name, String newName) {
        return AudioFiles.renameComment(StoryState.getStoryName(), slideNumber, name, newName);
    }

    @Override
    public void onRenameSuccess() {
        updateCommentList();
    }

    /**
     * This function sets the recording button with its functionality
     */
    private void setRecordCommentButton(final ImageButton recordButton){
        //TODO: use non-deprecated method; currently used to support older devices
        recordButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_mic_blue, null));
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Comment index for user starts at 1 so we increment 1 from the 0 based index
                int nextCommentIndex = comments.length + 1;
                File recordFile = AudioFiles.getComment(StoryState.getStoryName(), slideNumber,
                        "Comment " + nextCommentIndex);
                while (recordFile.exists()) {
                    nextCommentIndex++;
                    recordFile = AudioFiles.getComment(StoryState.getStoryName(), slideNumber,
                            "Comment " + nextCommentIndex);
                }

                String recordFilePath = recordFile.getPath();

                //stop all playback streams.
                if(draftPlayer != null && draftPlayer.isAudioPlaying()){
                    draftPlayer.stopAudio();
                    //TODO: use non-deprecated method; currently used to support older devices
                    draftPlaybackButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
                }
                if(commentPlayer != null && commentPlayer.isAudioPlaying()){
                    commentPlayer.stopAudio();
                    if (commentButtonClicked != null) {
                        commentButtonClicked.setImageResource(R.drawable.ic_green_play);
                    }
                }
                if(isRecording){
                    stopAudioRecorder();
                    LogFiles.saveLogEntry(ComChkEntry.Type.COMMENT_RECORDING.makeEntry());
                    //TODO: use non-deprecated method; currently used to support older devices
                    recordButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_mic_blue, null));
                    updateCommentList();
                }else{
                    startAudioRecorder(recordFilePath);
                    //TODO: use non-deprecated method; currently used to support older devices
                    recordButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_stop_red, null));
                }
            }
        });
    }

    /**
     * The function that aids in starting an audio recorder.
     */
    private void startAudioRecorder(String recordFilePath){
        setVoiceRecorder(recordFilePath);
        try {
            commentRecorder.prepare();
            commentRecorder.start();
            isRecording = true;
            Toast.makeText(getContext(), "Recording comment!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e){
            Log.e(LOG_TAG, "Error recording comment");
            Toast.makeText(getContext(), "Error recording comment", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * The function that aids in stopping an audio recorder.
     * According to documentation, it should have been enough to reset the comment recorder
     * but it was not working, so we go ahead and release it and make another for any subsequent
     * comments
     */
    private void stopAudioRecorder(){
        try{
            commentRecorder.stop();
            isRecording = false;
            Toast.makeText(getContext(), "Stopped recording!", Toast.LENGTH_SHORT).show();
        }catch(RuntimeException stopException){
            Toast.makeText(getContext(), "Please record again!", Toast.LENGTH_SHORT).show();
        }
        commentRecorder.release();
        commentRecorder = null;
    }


    /**
     * This function sets the voice recorder with either a new voicerecorder or reuses the
     * voicerecorder.
     * @param fileName The file to output the voice recordings.
     */
    private void setVoiceRecorder(String fileName){
        commentRecorder = new AudioRecorder(fileName, getActivity());
    }

    /**
     * Stops all media including audio playbacks and active audio recordings
     */
    private void stopAllMedia() {
        if(draftPlayer != null){
            draftPlayer.stopAudio();
            //TODO: use non-deprecated method; currently used to support older devices
            draftPlaybackButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_play_blue, null));
        }
        if(commentPlayer != null){
            commentPlayer.stopAudio();
            if (commentButtonClicked != null) {
                commentButtonClicked.setImageResource(R.drawable.ic_green_play);
            }
        }
        if(commentRecorder != null) {
            stopAudioRecorder();
            //TODO: use non-deprecated method; currently used to support older devices
            commentRecordButton.setBackgroundDrawable(VectorDrawableCompat.create(getResources(), R.drawable.ic_mic_blue, null));
            updateCommentList();
        }

    }
}

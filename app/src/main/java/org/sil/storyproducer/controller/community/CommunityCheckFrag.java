package org.sil.storyproducer.controller.community;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.CustomAdapter;
import org.sil.storyproducer.model.ListFiles;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * the fragment for the community check view. The community can make sure the draft is ok
 */
public class CommunityCheckFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private int slidePosition;
    private static AudioPlayer draftPlayer;
    private static AudioPlayer commentPlayer;
    private MediaRecorder commentRecorder;
    private View rootView;
    private ListView listView;
    private ArrayList<String> comments;
    private boolean isRecording;
    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
        context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_com_check, container, false);

        updateCommentList();
        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_commcheck_image_view), slidePosition);
        setDraftPlayback(rootView.findViewById(R.id.fragment_draft_playback_button));
        setRecordComment(rootView.findViewById(R.id.fragment_commcheck_add_comment_button));

        return rootView;
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if(draftPlayer != null){
                    draftPlayer.stopAudio();
                }
                if(commentPlayer != null){
                    commentPlayer.stopAudio();
                }
            }
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    @Override
    public void onPause() {
        super.onPause();
        if(draftPlayer != null){
            draftPlayer.stopAudio();
        }
        if(commentPlayer != null){
            commentPlayer.stopAudio();
        }
    }

    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on stop.
     */
    @Override
    public void onStop() {
        super.onStop();
        if(draftPlayer != null){
            draftPlayer.stopAudio();
            draftPlayer.releaseAudio();
        }
        if(commentPlayer != null){
            commentPlayer.stopAudio();
            commentPlayer.releaseAudio();
        }
    }

    public void updateCommentList() {
        listView = (ListView)rootView.findViewById(R.id.audio_comment_list_view);
        comments = FileSystem.getCommentTitles(StoryState.getStoryName(), slidePosition);
        String [] commentTitles = new String[comments.size()];
        commentTitles = comments.toArray(commentTitles);
        ListAdapter adapter = new CommentListAdapter(getContext(), commentTitles, slidePosition, this);
        listView.setAdapter(adapter);
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slidePosition == 0){
            RelativeLayout rl =  (RelativeLayout)rootView.findViewById(R.id.fragment_commcheck_Relative_Layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(View aView, int slideNum) {
        if (aView == null || !(aView instanceof ImageView)) {
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(), slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
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
     * @param aView
     */
    private void setDraftPlayback(View aView) {
        if (aView == null || !(aView instanceof ImageButton)) {
            return;
        }

        final File draftFile = FileSystem.getTranslationAudio(StoryState.getStoryName(), slidePosition);
        ImageView imageView = (ImageView) aView;
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //stop other playback streams.
            if(commentPlayer != null && commentPlayer.isAudioPlaying()){
                commentPlayer.stopAudio();
            }
            if (draftFile.exists()) {
                draftPlayer = new AudioPlayer();
                draftPlayer.playWithPath(draftFile.getPath());
                Toast.makeText(getContext(), "Playing Draft Audio...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No Draft Audio Found...", Toast.LENGTH_SHORT).show();
            }
            }
        });
    }

    public void playComment(int commentIndex) {
        final File commentFile = FileSystem.getAudioComment(StoryState.getStoryName(), slidePosition, commentIndex);
        if (draftPlayer != null && draftPlayer.isAudioPlaying()) {
            draftPlayer.stopAudio();
        }
        if (commentFile.exists()) {
            commentPlayer = new AudioPlayer();
            commentPlayer.playWithPath(commentFile.getPath());
            Toast.makeText(context, "Playing Comment...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No Comment Found...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This function sets the recording and playback buttons (The mic and play button) with their
     * respective functionalities.
     */
    private void setRecordComment(View aView){
        FloatingActionButton recordButton =
                (FloatingActionButton) aView;
        int numComments = comments.size();
        final String recordFilePath = FileSystem.getAudioComment(StoryState.getStoryName(), slidePosition,
                numComments).getPath();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop all other playback streams.
                if(commentPlayer != null && commentPlayer.isAudioPlaying()){
                    commentPlayer.stopAudio();
                }
                if(draftPlayer != null && draftPlayer.isAudioPlaying()){
                    draftPlayer.stopAudio();
                }
                if(isRecording){
                    stopAudioRecorder();
                    updateCommentList();
                }else{
                    startAudioRecorder(recordFilePath);
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
            e.printStackTrace();
        }
    }

    /**
     * The function that aids in stopping an audio recorder.
     */
    private void stopAudioRecorder(){
        try{
            commentRecorder.stop();
            isRecording = false;
            Toast.makeText(getContext(), "Stopped recording!", Toast.LENGTH_SHORT).show();
        }catch(RuntimeException stopException){
            Toast.makeText(getContext(), "Please record again!", Toast.LENGTH_SHORT).show();
        }
        commentRecorder.reset();
    }


    /**
     * This function sets the voice recorder with either a new voicerecorder or reuses the
     * voicerecorder.
     * @param fileName The file to output the voice recordings.
     */
    private void setVoiceRecorder(String fileName){
        if(commentRecorder == null){
            commentRecorder = new MediaRecorder();
        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        commentRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        commentRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        commentRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        commentRecorder.setOutputFile(fileName);
    }
}

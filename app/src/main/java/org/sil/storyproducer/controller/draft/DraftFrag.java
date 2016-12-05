package org.sil.storyproducer.controller.draft;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.FileSystem;

import java.io.IOException;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public class DraftFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private int slidePosition;
    private MediaPlayer narrationMediaPlayer;
    private View rootView;
    private String filePath;
    String recordFilePath;
    private MediaRecorder voiceRecorder;
    private boolean isRecording = false;
    private AudioPlayer voicePlayer;



    public DraftFrag() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
        FileSystem.loadSlideContent(StoryState.getStoryName(), slidePosition/*StoryState.getCurrentStorySlide()*/);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);


        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_draft_image_view), slidePosition/*StoryState.getCurrentStorySlide()*/);
        setScriptureText(rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText(rootView.findViewById(R.id.fragment_draft_reference_text));
        setNarration(rootView.findViewById(R.id.fragment_draft_narration_button));
        setRecordNPlayback();
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                if (narrationMediaPlayer != null) {
                    if (narrationMediaPlayer.isPlaying()) narrationMediaPlayer.stop();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (narrationMediaPlayer != null) {
            if (narrationMediaPlayer.isPlaying()) narrationMediaPlayer.stop();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (narrationMediaPlayer != null) {
            if (narrationMediaPlayer.isPlaying()) narrationMediaPlayer.stop();

            narrationMediaPlayer.release();
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
        DisplayMetrics metric = getContext().getResources().getDisplayMetrics();
        int height = metric.heightPixels;
        double scalingFactor = 0.4;
        height = (int)(height * scalingFactor);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * Sets the main text of the layout.
     *
     * @param aView The text view that will be filled with the verse's text.
     */
    private void setScriptureText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;
        textView.setText(FileSystem.getSlideContent());
    }

    /**
     * This function sets the reference text.
     * @param aView The view that will be populated with the reference text.
     */
    private void setReferenceText(View aView) {
        if (aView == null || !(aView instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) aView;

        String[] titleNamePriority = new String[]{FileSystem.getSlideVerse(),
                FileSystem.getSubTitle(), FileSystem.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }

        //TODO Reference a string constant
        textView.setText("Bible Story!");
    }

    /**
     * This function sets the narration playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param aView
     */
    private void setNarration(View aView) {
        if (aView == null || !(aView instanceof ImageButton)) {
            return;
        }

        filePath = FileSystem.getAudioPath(StoryState.getStoryName(), slidePosition);
        if (filePath != null) {
            narrationMediaPlayer = new MediaPlayer();

            ImageView imageView = (ImageView) aView;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (narrationMediaPlayer != null) {
                        if (narrationMediaPlayer.isPlaying()) narrationMediaPlayer.stop();

                        narrationMediaPlayer.release();
                        narrationMediaPlayer = new MediaPlayer();

                        try {
                            narrationMediaPlayer.setDataSource(filePath.toString());
                            narrationMediaPlayer.prepare();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                        Snackbar.make(rootView, "Playing Narration Audio...", Snackbar.LENGTH_SHORT).show();
                        narrationMediaPlayer.start();
                    } else {
                        Snackbar.make(rootView, "Could Not Find Narration Audio...", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slidePosition == 0){
            RelativeLayout rl = (RelativeLayout)rootView.findViewById(R.id.trans_layout);
            rl.setBackgroundColor(getResources().getColor(R.color.primaryDark));
            rl =  (RelativeLayout)rootView.findViewById(R.id.fragment_draft_Relative_Layout);
            rl.setBackgroundColor(getResources().getColor(R.color.primaryDark));

            TextView tv = (TextView)rootView.findViewById(R.id.fragment_draft_scripture_text);
            tv.setBackgroundColor(getResources().getColor(R.color.primaryDark));
            tv = (TextView)rootView.findViewById(R.id.fragment_draft_reference_text);
            tv.setBackgroundColor(getResources().getColor(R.color.primaryDark));

            ImageButton ib = (ImageButton)rootView.findViewById(R.id.fragment_draft_narration_button);
            ib.setBackgroundColor(getResources().getColor(R.color.primaryDark));
        }
    }


    private void setRecordNPlayback(){
        FloatingActionButton recordButton = (FloatingActionButton)rootView.findViewById(R.id.fragment_draft_record_button);
        recordFilePath = FileSystem.getStoryPath(StoryState.getStoryName());
        recordFilePath += "/recordedVoice" + slidePosition + ".mp3";

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording){
                    stopAudioRecorder();
                    setVoicePlayBackButton();
                }else{
                    startAudioRecorder();
                }
            }
        });
    }

    private void startAudioRecorder(){
        if(voiceRecorder != null){
            setVoiceRecorder(recordFilePath, false);
        }else{
            setVoiceRecorder(recordFilePath, true);
        }
        try {
            voiceRecorder.prepare();
            voiceRecorder.start();
            isRecording = true;
            Toast.makeText(getContext(), "Recording voice!", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException | IOException e){
            e.printStackTrace();
        }
    }

    private void stopAudioRecorder(){
        try{
            voiceRecorder.stop();
            isRecording = false;
            Toast.makeText(getContext(), "Stopped recording!", Toast.LENGTH_SHORT).show();
        }catch(RuntimeException stopException){
            Toast.makeText(getContext(), "Please record again!", Toast.LENGTH_SHORT).show();
        }
        voiceRecorder.reset();
    }



    private void setVoiceRecorder(String fileName, boolean createNewMediaRecorder){
        if(createNewMediaRecorder || voiceRecorder == null){
            voiceRecorder = new MediaRecorder();
        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        voiceRecorder.setOutputFile(fileName);
    }

    private void setVoicePlayBackButton() {
        FloatingActionButton playbackButton = (FloatingActionButton) rootView.findViewById(R.id.fragment_draft_playback_button);
        playbackButton.setVisibility(View.VISIBLE);
        voicePlayer = new AudioPlayer();

        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voicePlayer.isAudioPlaying()) {
                    voicePlayer.stopAudio();
                }
                voicePlayer.playWithPath(recordFilePath);
            }
        });
    }
}

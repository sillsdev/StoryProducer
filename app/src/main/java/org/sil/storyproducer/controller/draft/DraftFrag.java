package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
    private AudioPlayer narrationAudioPlayer;
    private View rootView;
    private String filePath;

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
        //setRecordNPlayback();
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                narrationAudioPlayer.stopAudio();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        narrationAudioPlayer.stopAudio();
    }

    @Override
    public void onStop() {
        super.onStop();
        narrationAudioPlayer.releaseAudio();
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
            ImageView imageView = (ImageView) aView;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(filePath == null) {
                        Snackbar.make(rootView, "Could Not Find Narration Audio...", Snackbar.LENGTH_SHORT).show();
                    } else {
                        narrationAudioPlayer = new AudioPlayer();
                        narrationAudioPlayer.playWithPath(filePath.toString());
                        Snackbar.make(rootView, "Playing Narration Audio...", Snackbar.LENGTH_SHORT).show();
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

    private void startAudioRecorder(MediaRecorder recorder){
        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException | IOException e){
            e.printStackTrace();
        }
    }

    private void stopAudioRecorder(MediaRecorder recorder){
        try{
            recorder.stop();
        }catch(RuntimeException stopException){
            Toast.makeText(getContext(), "Please record again", Toast.LENGTH_SHORT).show();
        }
        recorder.reset();
        recorder.release();
    }

//    private void setRecordNPlayback(){
//
//
//        floatingActionButton1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//            }
//        });
//
//        floatingActionButton1.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                v.setPressed(true);
//                outputFile = fileName + getArguments().getInt(SLIDE_NUM) + ".mp3";
//                audioRecorder = createAudioRecorder(output.getAbsolutePath() + "/" + outputFile);
//                startAudioRecorder(audioRecorder);
//                Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
//                isSpeakButtonLongPressed = true;
//                return true;
//            }
//        });
//
//        //TODO handle an event when you simply click -> it crashes when you do this
//        //hopefully the click function above this does that.
//        floatingActionButton1.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                v.onTouchEvent(event);
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    if (isSpeakButtonLongPressed) {
//                        Toast.makeText(getContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
//                        failure = 1;
//                        stopAudioRecorder(audioRecorder);
//                        //keep track of the number of records
//                        if (record_count == 2 & failure == 1) {
//                            record_count--;
//                            floatingActionButton1.setColorNormalResId(R.color.yellow);
//                            floatingActionButton2.setVisibility(View.VISIBLE);
//                        } else if (record_count == 1 & failure == 1) {
//                            record_count--;
//                            floatingActionButton1.setColorNormalResId(R.color.green);
//                            File greenColor = new File(output.getAbsolutePath() + "/" + outputFile + "green");
//                            if(!greenColor.exists()){ greenColor.mkdir();}
//                        } else if (record_count == 0 & failure == 0) {
//                            record_count++;
//                            floatingActionButton1.setColorNormalResId(R.color.yellow);
//                        }
//                        v.setPressed(false);
//                        isSpeakButtonLongPressed = false;
//                    }
//                }
//                return true;
//            }
//        });
//    }

}

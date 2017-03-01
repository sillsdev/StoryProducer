package org.sil.storyproducer.controller.consultant;

import android.graphics.Bitmap;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.SlideText;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AudioPlayer;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.FileSystem;

import java.io.File;

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
public class ConsultantCheckFrag extends Fragment {

    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";
    private int slidePosition;
    private View rootView;
    private boolean isChecked;
    AudioPlayer draftPlayer;
    SlideText slideText;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_con_check, container, false);
        slideText = FileSystem.getSlideText(StoryState.getStoryName(), slidePosition);

        setUiColors();
        setPic((ImageView)rootView.findViewById(R.id.fragment_concheck_image_view), slidePosition);
        setScriptureText((TextView)rootView.findViewById(R.id.fragment_concheck_scripture_text));
        setReferenceText((TextView)rootView.findViewById(R.id.fragment_concheck_reference_text));
        setDraftPlaybackButton((ImageButton)rootView.findViewById(R.id.concheck_draft_playback_button));
        setCheckmarkButton((ImageButton)rootView.findViewById(R.id.concheck_checkmark_button));
        setLogsButton((ImageButton)rootView.findViewById(R.id.concheck_logs_button));

        return rootView;
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors(){
        if(slidePosition == 0){
            RelativeLayout rl =  (RelativeLayout)rootView.findViewById(R.id.concheck_relative_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            rl = (RelativeLayout)rootView.findViewById(R.id.concheck_button_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            TextView tv = (TextView) rootView.findViewById(R.id.fragment_concheck_scripture_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            tv = (TextView) rootView.findViewById(R.id.fragment_concheck_reference_text);
            tv.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
            ImageButton ib = (ImageButton) rootView.findViewById(R.id.fragment_concheck_narration_button);
            ib.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(ImageView slideImage, int slideNum) {
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(), slideNum);

        if(slidePicture == null){
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
        }

        //Get the height of the phone.
        DisplayMetrics phoneProperties = getContext().getResources().getDisplayMetrics();
        int height = phoneProperties.heightPixels;
        double scalingFactor = 0.3;
        height = (int)(height * scalingFactor);

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
    private void setScriptureText(TextView textView) {

        textView.setText(slideText.getContent());
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
    private void setReferenceText(TextView textView) {
        String[] titleNamePriority = new String[]{slideText.getReference(),
                slideText.getSubtitle(), slideText.getTitle()};

        for (String title : titleNamePriority) {
            if (title != null && !title.equals("")) {
                textView.setText(title);
                return;
            }
        }
    }

    /**
     * This function sets the draft playback to the correct audio file. Also, the narration
     * button will have a listener added to it in order to detect playback when pressed.
     * @param button the ImageButton view handler to set the onclicklistener to
     */
    private void setDraftPlaybackButton(ImageButton button) {

        final File draftFile = FileSystem.getTranslationAudio(StoryState.getStoryName(), slidePosition);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stop other playback streams.
                if (draftPlayer != null && draftPlayer.isAudioPlaying()) {
                    draftPlayer.stopAudio();
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

    private void setCheckmarkButton(final ImageButton button) {
        //TODO: persist isChecked in some file and set button accordingly
        isChecked = false;
        button.setBackgroundResource(R.drawable.ic_checkmark_red);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isChecked) {
                    button.setBackgroundResource(R.drawable.ic_checkmark_red);
                    isChecked = false;
                } else {
                    button.setBackgroundResource(R.drawable.ic_checkmark_green);
                    isChecked = true;
                }
            }
            //TODO: check all of stories isChecked to see if story is completely checked
            // if story is checked, launch password to unlock dramatization
        });
    }

    private void setLogsButton(ImageButton button) {
        button.setBackgroundResource(R.drawable.ic_logs_blue);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Log interface yet to be implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

package org.sil.storyproducer.controller.dramatization;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.AnimationToolbar;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.FileSystem;

import java.io.File;


public class DramatizationFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    private View rootView;
    private int slidePosition;
    private AnimationToolbar myToolbar;
    private boolean isRecording = false;

    @Override
    public void onCreate(Bundle savedState){
        super.onCreate(savedState);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false);
        setUiColors();
        setPic(rootView.findViewById(R.id.fragment_dramatization_image_view), slidePosition);
        setDraftPlayback(rootView.findViewById(R.id.fragment_dramatization_play_draft_button), slidePosition);
        setToolbar();
        return rootView;
    }

    public boolean isToolBarOpen(){
        if(myToolbar != null){
            return myToolbar.isOpen();
        }
        return false;
    }

    public void closeToolbar(){
        if(myToolbar!= null && myToolbar.isOpen()){
            myToolbar.close();
        }
    }

    /**
     * This function sets the first slide of each story to the blue color in order to prevent
     * clashing of the grey starting picture.
     */
    private void setUiColors() {
        if (slidePosition == 0) {
            RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.fragment_dramatization_root_relayout_layout);
            rl.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryDark));
        }
    }

    private void setPic(View aView, int slideNum){
        if(aView == null || !(aView instanceof ImageView)){
            return;
        }

        ImageView slideImage = (ImageView) aView;
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(), slideNum);

        if (slidePicture == null) {
            Snackbar.make(rootView, "Could Not Find Picture...", Snackbar.LENGTH_SHORT).show();
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

    void setDraftPlayback(View aView, int slideNum){
        if(aView == null || !(aView instanceof ImageButton)){
            return;
        }



        ImageButton playButton = (ImageButton)aView;
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private void setToolbar(){
        setupToolbarAndRecordAnim(rootView.findViewById(R.id.fragment_dramatization_fab),
                rootView.findViewById(R.id.fragment_dramatization_animated_toolbar));
      //  setRecordNPlayback();
       // setToolbarDeleteButton(new File(recordFilePath).exists());
    }

    /**
     * This function initializes the animated toobar and click of dummyView to close toolbar.
     */
    private void setupToolbarAndRecordAnim(View fab, View relativeLayout) {
        if (fab == null || relativeLayout == null) {
            return;
        }
        try {
            myToolbar = new AnimationToolbar(fab, relativeLayout, this.getActivity());
        } catch (ClassCastException ex) {
            Log.e(getActivity().toString(), ex.getMessage());
        }

        //The following allows for a touch from user to close the toolbar and make the fab visible.
        //This also stops the recording animation
        LinearLayout dummyView = (LinearLayout) rootView.findViewById(R.id.fragment_dramatization_dummyView);
        dummyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myToolbar != null && myToolbar.isOpen() && !isRecording) {
                    myToolbar.close();
                }
            }
        });

        //This function must be called before using record animations i.e. calling
        //setRecordNPlayback()
        //setupRecordingAnimationHandler();
    }



}

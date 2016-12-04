package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
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

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.FileSystem;
import org.w3c.dom.Text;

import java.io.File;
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

    public DraftFrag() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_draft, container, false);
        Bundle passedArgs = this.getArguments();
        slidePosition = passedArgs.getInt(SLIDE_NUM);
        FileSystem.loadSlideContent(StoryState.getStoryName(), slidePosition/*StoryState.getCurrentStorySlide()*/);

        setPic(rootView.findViewById(R.id.fragment_draft_image_view), slidePosition/*StoryState.getCurrentStorySlide()*/);
        setScriptureText(rootView.findViewById(R.id.fragment_draft_scripture_text));
        setReferenceText(rootView.findViewById(R.id.fragment_draft_reference_text));
        setNarration(rootView.findViewById(R.id.fragment_draft_narration_button));
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                //TODO release playing audio or other fragment instance related objects
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
        //TODO make sure to dsiplay an error when a picture is not found for the story and slidenum

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

}

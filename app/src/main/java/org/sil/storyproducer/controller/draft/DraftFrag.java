package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sil.storyproducer.R;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.BitmapScaler;
import org.sil.storyproducer.tools.FileSystem;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public class DraftFrag extends Fragment {
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    public DraftFrag() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(R.layout.fragment_draft, container, false);

        FileSystem.loadSlideContent(StoryState.getStoryName(), StoryState.getCurrentStorySlide());

        setPic(rootView.findViewById(R.id.fragment_draft_image_view), StoryState.getCurrentStorySlide());
        setText(rootView.findViewById(R.id.fragment_draft_text_view));
        return rootView;
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     * @param aView The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    private void setPic(View aView,int slideNum){
        if(! (aView instanceof ImageView)){
            return;
        }
        ImageView slideImage = (ImageView)aView;
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(),slideNum);
        //TODO make sure to dsiplay an error when a picture is not found for the story and slidenum

        //Get the height of the phone.
        DisplayMetrics metric = getContext().getResources().getDisplayMetrics();
        int height = metric.heightPixels;
        double scalingFactor = 0.5;
        height = (int)(height * scalingFactor);

        //Set the height of the image view
        slideImage.getLayoutParams().height = height;
        slideImage.requestLayout();

        slideImage.setImageBitmap(slidePicture);
    }

    /**
     * Sets the main text of the layout.
     * @param aView The text view that will be filled with the verse's text.
     */
    private void setText(View aView){
        if(! (aView instanceof TextView)){
            return;
        }
        TextView textView = (TextView)aView;
        textView.setText(FileSystem.getSlideContent());
    }


}

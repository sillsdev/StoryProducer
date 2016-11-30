package org.sil.storyproducer.controller.draft;

import android.graphics.Bitmap;
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
        Bundle args = getArguments();

        FileSystem.loadSlideContent(StoryState.getStoryName(), args.getInt(SLIDE_NUM));

        setPic(rootView.findViewById(R.id.fragment_draft_image_view), args.getInt(SLIDE_NUM));
        setText(rootView.findViewById(R.id.fragment_draft_text_view));
        return rootView;
    }

    private void setPic(View aView,int slideNum){
        ImageView slideImage = (ImageView)aView;
        Bitmap slidePicture = FileSystem.getImage(StoryState.getStoryName(),slideNum);
//        slideImage.setImageBitmap();
        DisplayMetrics metric = getContext().getResources().getDisplayMetrics();
        int width = metric.widthPixels;
        int height = metric.heightPixels;

        System.out.format("The width:%d and height:%d of picture before transformation. %n", slidePicture.getWidth(), slidePicture.getHeight());

        slidePicture = BitmapScaler.scaleToFitWidth(slidePicture, width);

        System.out.format("The width:%d and height:%d of picture after transformation. %n", slidePicture.getWidth(), slidePicture.getHeight());


        slideImage.setImageBitmap(slidePicture);
    }

    private void setText(View aView){
        TextView textView = (TextView)aView;
        textView.setText(FileSystem.getSlideContent());
    }


}

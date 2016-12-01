package org.sil.storyproducer.controller.draft;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sil.storyproducer.R;

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
public class DraftFrag extends Fragment {
    public static final String ARG_OBJECT = "object";

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
        ((TextView) rootView.findViewById(R.id.textView)).setText("Object " + args.getInt(ARG_OBJECT));

        return rootView;
    }

}

package org.sil.storyproducer.controller.community;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sil.storyproducer.R;

/**
 * the fragment for the community check view. The community can make sure the draft is ok
 */
public class CommunityCheckFrag extends Fragment {
    private static final String ARG_OBJECT = "object";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_com_check, container, false);
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.textView)).setText("Object " + args.getInt(ARG_OBJECT));

        return rootView;
    }
}

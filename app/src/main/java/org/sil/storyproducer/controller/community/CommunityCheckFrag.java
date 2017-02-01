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
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_com_check, container, false);
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.textView)).setText("");

        return rootView;
    }
}

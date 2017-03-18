package org.sil.storyproducer.controller.consultant;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sil.storyproducer.R;

/**
 * The fragment for the Consultant check view. The consultant can check that the draft is ok
 */
public class ConsultantCheckFrag extends Fragment {
    private static final String ARG_OBJECT = "object";
    public static final String SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_con_check, container, false);
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.textView)).setText("");

        return rootView;
    }
}

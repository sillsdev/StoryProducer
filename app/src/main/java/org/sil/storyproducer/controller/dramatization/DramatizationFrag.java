package org.sil.storyproducer.controller.dramatization;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sil.storyproducer.R;


public class DramatizationFrag extends Fragment {
    private static final String ARG_OBJECT = "object";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_dramatization, container, false);
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.textView)).setText("");

        return rootView;
    }
}

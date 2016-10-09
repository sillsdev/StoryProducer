package org.sil.storyproducer.controller.community;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sil.storyproducer.R;


public class CommunityCheckFrag extends Fragment {
    private static final String SLIDE_NUM = "slidenum";

    public static CommunityCheckFrag newInstance(int position){
        CommunityCheckFrag frag = new CommunityCheckFrag();
        Bundle args = new Bundle();
        args.putInt(SLIDE_NUM, position);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_com_check, container, false);
    }
}

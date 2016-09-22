package org.sil.storyproducer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ComCheckFrag extends Fragment {
    private static final String SLIDE_NUM = "slidenum";

    public static ComCheckFrag newInstance(int position){
        ComCheckFrag frag = new ComCheckFrag();
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

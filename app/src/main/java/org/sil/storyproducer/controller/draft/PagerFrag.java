package org.sil.storyproducer.controller.draft;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sil.storyproducer.tools.PagerAnimation;
import org.sil.storyproducer.R;

public class PagerFrag extends Fragment{
    public static final String NUM_OF_FRAG = "fragnum";
    public static final String TYPE_OF_FRAG = "fragtype";
    public static final String STORY_NAME = "storyname";
    public static PagerFrag newInstance(int numOfFrags, int typeOfFrag, String storyName){
        PagerFrag frag = new PagerFrag();
        Bundle bundle = new Bundle();
        bundle.putInt(NUM_OF_FRAG, numOfFrags);
        bundle.putInt(TYPE_OF_FRAG, typeOfFrag);
        bundle.putString(STORY_NAME, storyName);
        frag.setArguments(bundle);
        return frag;
    }
    private ViewPager mPager;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pager, container, false);
        mPager = (ViewPager)view.findViewById(R.id.pager_viewpager);
        mPager.setAdapter(new PagerAdapter(getActivity(), getChildFragmentManager(), getArguments().getInt(NUM_OF_FRAG), getArguments().getInt(TYPE_OF_FRAG), getArguments().getString(STORY_NAME)));
        mPager.setPageTransformer(true, new PagerAnimation());
        return view;
    }

    public void changeView(int position){
        mPager.setCurrentItem(position, true);
    }

}

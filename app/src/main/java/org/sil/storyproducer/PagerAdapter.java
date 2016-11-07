package org.sil.storyproducer;

import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jordan Skomer on 10/22/2015.
 */
public class PagerAdapter extends FragmentPagerAdapter {
    Context context;
    private static int NUM_OF_FRAGS = 5;
    private static int FRAG_TYPE = 0;
    private static String FRAG_STORY = "";
    private Map<Integer, TransFrag> mapOfTransFrags;

    public PagerAdapter(Context context, FragmentManager fm, int fragNum, int fragType, String fragStory){
        super(fm);
        this.context = context;
        NUM_OF_FRAGS = fragNum;
        FRAG_TYPE = fragType;
        FRAG_STORY = fragStory;
        mapOfTransFrags = new HashMap<>();
    }

    @Override
    public int getCount() {
        return NUM_OF_FRAGS;
    }

    @Override
    public Fragment getItem(int position) {
        switch (FRAG_TYPE){
            //Translate
            case 1:
                TransFrag toReturn = TransFrag.newInstance(position, NUM_OF_FRAGS, FRAG_STORY);
                mapOfTransFrags.put(position, toReturn);
                return toReturn;
            //Community
            case 2:
                return new ComCheckFrag();
            //Consultant
            case 3:
                return new ConCheckFrag();
        }
        return null;
    }

    public TransFrag getTransFrag(int position){
        return mapOfTransFrags.get(position);
    }
}

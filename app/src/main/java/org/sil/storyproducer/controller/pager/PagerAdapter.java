package org.sil.storyproducer.controller.pager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.sil.storyproducer.controller.community.CommunityCheckFrag;
import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag;
import org.sil.storyproducer.controller.draft.DraftFrag;
import org.sil.storyproducer.controller.dramatization.DramatizationFrag;
import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.FileSystem;

public class PagerAdapter extends FragmentStatePagerAdapter {

    private int numOfSlides = 0;
    private static int previousSlide = 0;
    private static boolean getInitialPosition = false;

    public PagerAdapter(FragmentManager fm) {
        super(fm);
        numOfSlides = FileSystem.getTotalSlideNum(StoryState.getStoryName());
    }

    /**
     * getItem is called every time the user moves on to the next page to get the next fragment
     *
     * @param i
     * @return the fragment
     */
    @Override
    public Fragment getItem(int i) {
        Fragment fragment;
        switch (StoryState.getCurrentPhase().getTitle()) {
            case "Draft":
                fragment = new DraftFrag();
                break;
            case "Community Check":
                fragment = new CommunityCheckFrag();
                break;
            case "Consultant Check":
                fragment = new ConsultantCheckFrag();
                break;
            case "Dramatization":
                fragment = new DramatizationFrag();
                break;
            default:
                fragment = new DraftFrag();
        }
        Bundle passedArgs = new Bundle();
        passedArgs.putInt(DraftFrag.SLIDE_NUM, i);
        fragment.setArguments(passedArgs);
        //mapOfTransFrags.put(i, (DraftFrag)fragment);

        return fragment;
    }

    /**
     * Returns the count of how many pages are in the pager
     *
     * @return page count
     */
    @Override
    public int getCount() {
        return numOfSlides;
    }

    /**
     * returns the page title for a specific page
     *
     * @param position
     * @return the title
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return "Page " + (position + 1);
    }

    /**
     * @param slidePosition
     * @return
     */
    public DraftFrag getDraftFrag(int slidePosition) {
        return null;
    }
}

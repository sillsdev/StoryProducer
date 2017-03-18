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

    public PagerAdapter(FragmentManager fm) {
        super(fm);
        numOfSlides = FileSystem.getContentSlideAmount(StoryState.getStoryName());
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
        Bundle passedArgs = new Bundle();
        switch (StoryState.getCurrentPhase().getTitle()) {
            case StoryState.DRAFT_PHASE:
                fragment = new DraftFrag();
                passedArgs.putInt(DraftFrag.SLIDE_NUM, i);
                break;
            case StoryState.COMMUNITY_PHASE:
                fragment = new CommunityCheckFrag();
                passedArgs.putInt(CommunityCheckFrag.SLIDE_NUM, i);
                break;
            case StoryState.CONSULTANT_PHASE:
                fragment = new ConsultantCheckFrag();
                passedArgs.putInt(ConsultantCheckFrag.SLIDE_NUM, i);
                break;
            case StoryState.DRAMATIZATION_PHASE:
                fragment = new DramatizationFrag();
                passedArgs.putInt(DramatizationFrag.SLIDE_NUM, i);
                break;
            default:
                fragment = new DraftFrag();
                passedArgs.putInt(DraftFrag.SLIDE_NUM, i);
        }
        fragment.setArguments(passedArgs);

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
}

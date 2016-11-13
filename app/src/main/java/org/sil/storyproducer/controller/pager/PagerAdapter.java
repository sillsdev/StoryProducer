package org.sil.storyproducer.controller.pager;

import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;

import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag;
import org.sil.storyproducer.controller.draft.DraftFrag;
import org.sil.storyproducer.controller.dramatization.DramatizationFrag;

public class PagerAdapter extends FragmentStatePagerAdapter {

    public PagerAdapter(FragmentManager fm) {
        super(fm);
    }

    /**
     * getItem is called every time the user moves on to the next page to get the next fragment
     * @param i
     * @return the fragment
     */
    @Override
    public Fragment getItem(int i) {
        Fragment fragment = new DraftFrag();
        Bundle args = new Bundle();
        // Our object is just an integer :-P
        args.putInt(DraftFrag.ARG_OBJECT, i + 1);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns the count of how many pages are in the pager
     * @return page count
     */
    @Override
    public int getCount() {
        return 5;
    }

    /**
     * returns the page title for a specific page
     * @param position
     * @return the title
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return "OBJECT " + (position + 1);
    }
}

package org.sil.storyproducer.controller.pager;

import android.support.v4.view.ViewPager;

/**
 * Class that implements the ViewPager.OnPageChangeListener to give the view pager circular functionality
 */
public class CircularViewPagerHandler implements ViewPager.OnPageChangeListener {
    private ViewPager   mViewPager;
    private int         mCurrentPosition;
    private int         mScrollState;

    public CircularViewPagerHandler(final ViewPager viewPager) {
        mViewPager = viewPager;
    }

    @Override
    public void onPageSelected(final int position) {
        mCurrentPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE && mScrollState != ViewPager.SCROLL_STATE_SETTLING) {
            handleSetNextItem();
        }
        mScrollState = state;
    }

    private void handleSetNextItem() {
        final int lastPosition = mViewPager.getAdapter().getCount() - 1;
        if(mCurrentPosition == 0) {
            mViewPager.setCurrentItem(lastPosition, false);
        } else if(mCurrentPosition == lastPosition) {
            mViewPager.setCurrentItem(0, false);
        }
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }
}

package org.sil.storyproducer.controller.pager

import android.support.v4.view.ViewPager

import org.sil.storyproducer.model.StoryState

/**
 * Class that implements the ViewPager.OnPageChangeListener to give the view pager circular functionality
 */
class CircularViewPagerHandler(private val mViewPager: ViewPager) : ViewPager.OnPageChangeListener {
    private var mCurrentPosition: Int = 0
    private var mScrollState: Int = 0

    override fun onPageSelected(position: Int) {
        mCurrentPosition = position
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE && mScrollState != ViewPager.SCROLL_STATE_SETTLING) {
            handleSetNextItem()
        }
        mScrollState = state
    }

    private fun handleSetNextItem() {
        val lastPosition = mViewPager.adapter.count - 1
        if (mCurrentPosition == 0) {
            mViewPager.setCurrentItem(lastPosition, false)
        } else if (mCurrentPosition == lastPosition) {
            mViewPager.setCurrentItem(0, false)
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        StoryState.setCurrentStorySlide(mViewPager.currentItem)
    }
}

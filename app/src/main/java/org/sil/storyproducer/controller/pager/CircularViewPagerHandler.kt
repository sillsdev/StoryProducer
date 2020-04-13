package org.sil.storyproducer.controller.pager

import android.support.v4.view.ViewPager
import android.util.Log

import org.sil.storyproducer.model.Workspace

/**
 * Class that implements the ViewPager.OnPageChangeListener to give the view pager circular functionality
 */
class CircularViewPagerHandler(private val mViewPager: ViewPager) : ViewPager.OnPageChangeListener {
    private var mScrollState: Int = 0

    override fun onPageSelected(position: Int) {
        Workspace.activeStory.lastSlideNum = position
        Log.e("@pwhite", "switched sides $position")
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE && mScrollState != ViewPager.SCROLL_STATE_SETTLING) {
            handleSetNextItem()
        }
        mScrollState = state
    }

    private fun handleSetNextItem() {
        val lastPosition = mViewPager.adapter!!.count - 1
        if (Workspace.activeStory.lastSlideNum == 0) {
            mViewPager.setCurrentItem(lastPosition, false)
        } else if (Workspace.activeStory.lastSlideNum == lastPosition) {
            mViewPager.setCurrentItem(0, false)
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }
}

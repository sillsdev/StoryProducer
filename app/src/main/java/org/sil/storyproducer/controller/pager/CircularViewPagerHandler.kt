package org.sil.storyproducer.controller.pager

import android.support.v4.view.ViewPager
import android.util.Log

import org.sil.storyproducer.model.Workspace

/**
 * Class that implements the ViewPager.OnPageChangeListener to give the view pager circular functionality
 */
class CircularViewPagerHandler(private val mViewPager: ViewPager) : ViewPager.OnPageChangeListener {
    private var userWasScrolling = false
    private var lastScrollState = ViewPager.SCROLL_STATE_IDLE

    override fun onPageSelected(position: Int) {
        if (userWasScrolling) {
            Workspace.activeStory.lastSlideNum = position
            Log.e("@pwhite", "switched slides $position")
        }
    }

    override fun onPageScrollStateChanged(scrollState: Int) {
        if (scrollState == ViewPager.SCROLL_STATE_DRAGGING) {
            userWasScrolling = true
        } else if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
            // Whether the page was scrolled to programmatically or by the
            // user, SCROLL_STATE_SETTLING always comes immediately before
            // SCROLL_STATE_IDLE. The only time it does not is when the end
            // or beginning of the ViewPager is reached, in which case, the
            // dragging occurs, but no scrolling happens, which means the scroll
            // state moves back to SCROLL_STATE_IDLE immediately
            if (lastScrollState != ViewPager.SCROLL_STATE_SETTLING) {
                val lastPosition = mViewPager.adapter!!.count - 1
                if (Workspace.activeStory.lastSlideNum == 0) {
                    mViewPager.setCurrentItem(lastPosition, false)
                } else if (Workspace.activeStory.lastSlideNum == lastPosition) {
                    mViewPager.setCurrentItem(0, false)
                }            
            }
            userWasScrolling = false
        }
        lastScrollState = scrollState
    }

    private fun handleSetNextItem() {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }
}

package org.sil.storyproducer.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class FilterPageAdapter(var numOfTabs: Int, fm: FragmentManager): FragmentPagerAdapter(fm,numOfTabs) {

    override fun getCount(): Int {
        return numOfTabs
    }

    override fun getItem(tabPosition: Int): StoryListFrag {
        return StoryListFrag(tabPosition)
    }
}
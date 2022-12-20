package org.tyndalebt.storyproduceradv.controller.storylist

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.tyndalebt.storyproduceradv.activities.AppCompatActivityMTT

class StoryPageAdapter(activity: AppCompatActivityMTT, private val itemsCount: Int) :
        FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
        return StoryPageFragment.getInstance(position)
    }
}
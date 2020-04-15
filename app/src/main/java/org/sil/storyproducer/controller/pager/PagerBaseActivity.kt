package org.sil.storyproducer.controller.pager

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.PHASE_TYPE
import org.sil.storyproducer.model.PhaseType

class PagerBaseFragment : Fragment() {

    private var viewIsPrepared = false

    private lateinit var phaseType: PhaseType
    private lateinit var mPagerAdapter: PagerAdapter
    private lateinit var mViewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phaseType = PhaseType.ofInt(arguments!!.getInt(PHASE_TYPE, 0))

        if (!phaseType.checkValidDisplaySlideNum(Workspace.activeStory.lastSlideNum)) {
            Workspace.activeStory.lastSlideNum = 0
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.activity_pager_base, container, false)
        mPagerAdapter = PagerAdapter(childFragmentManager, phaseType)
        mViewPager = rootView.findViewById<ViewPager>(R.id.pager)
        mViewPager.adapter = mPagerAdapter
        mViewPager.currentItem = Workspace.activeStory.lastSlideNum
        mViewPager.addOnPageChangeListener(CircularViewPagerHandler(mViewPager))
        viewIsPrepared = true
        return rootView
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (viewIsPrepared) {
            val itemIndex = Workspace.activeStory.lastSlideNum
            mViewPager.currentItem = itemIndex
            val page = mPagerAdapter.instantiateItem(mViewPager, itemIndex) as Fragment
            page.setUserVisibleHint(isVisibleToUser)
        }
    }
}

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

    lateinit var phaseType: PhaseType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phaseType = PhaseType.ofInt(arguments!!.getInt(PHASE_TYPE, 0))

        if (!phaseType.checkValidDisplaySlideNum(Workspace.activeSlideNum)) {
            Workspace.activeSlideNum = 0
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.activity_pager_base, container, false)
        val mPagerAdapter = PagerAdapter(childFragmentManager, phaseType)
        val mViewPager = rootView.findViewById<ViewPager>(R.id.pager)
        mViewPager.adapter = mPagerAdapter
        mViewPager.currentItem = Workspace.activeSlideNum
        mViewPager.addOnPageChangeListener(CircularViewPagerHandler(mViewPager))
        return rootView
    }
}

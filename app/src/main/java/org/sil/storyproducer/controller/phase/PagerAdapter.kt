package org.sil.storyproducer.controller.phase

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import org.sil.storyproducer.controller.export.CreateFragment
import org.sil.storyproducer.controller.export.ShareFragment
import org.sil.storyproducer.controller.learn.LearnFragment
import org.sil.storyproducer.controller.pager.PagerBaseFragment
import org.sil.storyproducer.controller.remote.WholeStoryBackTranslationFragment
import org.sil.storyproducer.model.PHASE_TYPE
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace

class PagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        val phaseType = Workspace.phases[i]
        val fragment = when (phaseType) {
            PhaseType.LEARN -> LearnFragment()
            PhaseType.CREATE -> CreateFragment()
            PhaseType.WHOLE_STORY -> WholeStoryBackTranslationFragment()
            PhaseType.SHARE -> ShareFragment()
            PhaseType.DRAFT -> PagerBaseFragment()
            PhaseType.COMMUNITY_CHECK -> PagerBaseFragment()
            PhaseType.CONSULTANT_CHECK -> PagerBaseFragment()
            PhaseType.REMOTE_CHECK -> PagerBaseFragment()
            PhaseType.BACKT -> PagerBaseFragment()
            PhaseType.DRAMATIZATION -> PagerBaseFragment()
        }
        val passedArgs = Bundle()
        passedArgs.putInt(PHASE_TYPE, phaseType.ordinal)
        fragment.arguments = passedArgs

        return fragment
    }

    override fun getCount(): Int {
        return Workspace.phases.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return Workspace.phases[position].getDisplayName()
    }
}

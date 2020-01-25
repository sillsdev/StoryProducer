package org.sil.storyproducer.controller.pager

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import org.sil.storyproducer.controller.backtranslation.BackTranslationFrag
import org.sil.storyproducer.controller.community.CommunityCheckFrag
import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.dramatization.DramatizationFrag
import org.sil.storyproducer.controller.remote.RemoteCheckFrag
import org.sil.storyproducer.model.PHASE_TYPE
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM

class PagerAdapter(fm: FragmentManager, val phaseType: PhaseType) : FragmentStatePagerAdapter(fm) {

    val countCache = phaseType.getPhaseDisplaySlideCount()

    /**
     * getItem is called every time the user moves on to the next page to get the next fragment
     *
     * @param i
     * @return the fragment
     */
    override fun getItem(i: Int): Fragment {
        val fragment: Fragment = when (phaseType) {
            PhaseType.DRAFT -> DraftFrag()
            PhaseType.COMMUNITY_CHECK -> CommunityCheckFrag()
            PhaseType.CONSULTANT_CHECK -> ConsultantCheckFrag()
            PhaseType.DRAMATIZATION -> DramatizationFrag()
            PhaseType.BACKT -> BackTranslationFrag()
            PhaseType.REMOTE_CHECK -> RemoteCheckFrag()
            else -> DraftFrag()
        }

        val passedArgs = Bundle()
        passedArgs.putInt(SLIDE_NUM, i)
        passedArgs.putInt(PHASE_TYPE, phaseType.ordinal)
        fragment.arguments = passedArgs

        return fragment
    }

    /**
     * Returns the count of how many pages are in the pager
     *
     * @return page count
     */
    override fun getCount(): Int {
        //use the cached value so that if there are two quick phase changes, you will use the
        //"ActivePhase" when it was first called, not the last time it was called.  Fixes crashes.
        return countCache
    }

    /**
     * returns the page title for a specific page
     *
     * @param position
     * @return the title
     */
    override fun getPageTitle(position: Int): CharSequence {
        return "Page " + (position + 1)
    }
}

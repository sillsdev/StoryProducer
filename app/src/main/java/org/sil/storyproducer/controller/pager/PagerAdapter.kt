package org.sil.storyproducer.controller.pager

//import org.sil.storyproducer.controller.remote.BackTranslationFrag
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.community.CommunityCheckFrag
import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.dramatization.DramatizationFrag
import org.sil.storyproducer.controller.remote.BackTranslationFrag
import org.sil.storyproducer.controller.remote.RemoteCheckFrag
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace

class PagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    /**
     * getItem is called every time the user moves on to the next page to get the next fragment
     *
     * @param i
     * @return the fragment
     */
    override fun getItem(i: Int): Fragment {
        val fragment: Fragment
        when (Workspace.activePhase.phaseType) {
            PhaseType.DRAFT -> {
                fragment = DraftFrag()
            }
            PhaseType.COMMUNITY_CHECK -> {
                fragment = CommunityCheckFrag()
            }
            PhaseType.CONSULTANT_CHECK -> {
                fragment = ConsultantCheckFrag()
            }
            PhaseType.DRAMATIZATION -> {
                fragment = DramatizationFrag()
            }
            PhaseType.BACKT -> {
                fragment = BackTranslationFrag()
            }
            PhaseType.REMOTE_CHECK -> {
                fragment = RemoteCheckFrag()
            }
            else -> {
                fragment = DraftFrag()
            }
        }
        val passedArgs = Bundle()
        passedArgs.putInt(SlidePhaseFrag.SLIDE_NUM, i)
        fragment.arguments = passedArgs

        return fragment
    }

    /**
     * Returns the count of how many pages are in the pager
     *
     * @return page count
     */
    override fun getCount(): Int {
        return Workspace.activePhase.getPhaseDisplaySlideCount()
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

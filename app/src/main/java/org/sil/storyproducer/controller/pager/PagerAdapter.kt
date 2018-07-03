package org.sil.storyproducer.controller.pager

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import org.sil.storyproducer.controller.MultiRecordFrag

import org.sil.storyproducer.controller.remote.RemoteCheckFrag
import org.sil.storyproducer.controller.community.CommunityCheckFrag
import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag
import org.sil.storyproducer.controller.draft.DraftFrag
import org.sil.storyproducer.controller.dramatization.DramatizationFrag
import org.sil.storyproducer.controller.remote.BackTranslationFrag
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.FileSystem

class PagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val numOfSlides = 0

    /**
     * getItem is called every time the user moves on to the next page to get the next fragment
     *
     * @param i
     * @return the fragment
     */
    override fun getItem(i: Int): Fragment {
        val fragment: Fragment
        val passedArgs = Bundle()
        when (Workspace.activePhase.phaseType) {
            PhaseType.DRAFT -> {
                fragment = DraftFrag()
                passedArgs.putInt(MultiRecordFrag.SLIDE_NUM, i)
            }
            PhaseType.COMMUNITY_CHECK -> {
                fragment = CommunityCheckFrag()
                passedArgs.putInt(CommunityCheckFrag.SLIDE_NUM, i)
            }
            PhaseType.CONSULTANT_CHECK -> {
                fragment = ConsultantCheckFrag()
                passedArgs.putInt(ConsultantCheckFrag.SLIDE_NUM, i)
            }
            PhaseType.DRAMATIZATION -> {
                fragment = DramatizationFrag()
                passedArgs.putInt(DramatizationFrag.SLIDE_NUM, i)
            }
            PhaseType.BACKT -> {
                fragment = BackTranslationFrag()
                passedArgs.putInt(BackTranslationFrag.SLIDE_NUM, i)
            }
            PhaseType.REMOTE_CHECK -> {
                fragment = RemoteCheckFrag()
                passedArgs.putInt(RemoteCheckFrag.SLIDE_NUM, i)
            }
            else -> {
                fragment = DraftFrag()
                passedArgs.putInt(CommunityCheckFrag.SLIDE_NUM, i)
            }
        }
        fragment.arguments = passedArgs

        return fragment
    }

    /**
     * Returns the count of how many pages are in the pager
     *
     * @return page count
     */
    override fun getCount(): Int {
        return Workspace.activeStory.slides.size
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

package org.tyndalebt.spadv.controller.pager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.tyndalebt.spadv.controller.communitywork.CommunityWorkFrag
import org.tyndalebt.spadv.controller.accuracycheck.AccuracyCheckFrag
import org.tyndalebt.spadv.controller.translaterevise.TranslateReviseFrag
import org.tyndalebt.spadv.controller.voicestudio.VoiceStudioFrag
import org.tyndalebt.spadv.controller.remote.RemoteCheckFrag
import org.tyndalebt.spadv.controller.remote.BackTranslationFrag
import org.tyndalebt.spadv.controller.remote.WholeStoryBackTranslationFrag
import org.tyndalebt.spadv.model.PhaseType
import org.tyndalebt.spadv.model.SLIDE_NUM
import org.tyndalebt.spadv.model.Workspace

class PagerAdapter(fm: FragmentManager) : androidx.fragment.app.FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    val countCache = Workspace.activePhase.getPhaseDisplaySlideCount()

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
            PhaseType.TRANSLATE_REVISE -> {
                fragment = TranslateReviseFrag()
            }
            PhaseType.COMMUNITY_WORK -> {
                fragment = CommunityWorkFrag()
            }
            PhaseType.ACCURACY_CHECK -> {
                fragment = AccuracyCheckFrag()
            }
            PhaseType.VOICE_STUDIO -> {
                fragment = VoiceStudioFrag()
            }
            PhaseType.BACK_T -> {
                fragment = BackTranslationFrag()
            }
            PhaseType.WHOLE_STORY -> {
                fragment = WholeStoryBackTranslationFrag()
            }
            PhaseType.REMOTE_CHECK -> {
                fragment = RemoteCheckFrag()
            }
            else -> {
                fragment = TranslateReviseFrag()
            }
        }
        passedArgs.putInt(SLIDE_NUM, i)
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

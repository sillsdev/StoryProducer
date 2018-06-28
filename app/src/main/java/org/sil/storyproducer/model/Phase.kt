package org.sil.storyproducer.model

import android.content.Context
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.export.CreateActivity
import org.sil.storyproducer.controller.export.ShareActivity
import org.sil.storyproducer.controller.learn.LearnActivity
import org.sil.storyproducer.controller.pager.PagerBaseActivity
import org.sil.storyproducer.controller.remote.WholeStoryBackTranslationActivity


enum class PhaseType {
    LEARN, DRAFT, COMMUNITY_CHECK, CONSULTANT_CHECK, DRAMATIZATION, CREATE, SHARE, BACKT, WHOLE_STORY, REMOTE_CHECK
}

/**
 * The business object for phases that are part of the story
 */
class Phase(val phaseType: PhaseType) {


    /**
     * get the title for the phase
     * @return return the title
     */
    fun getName() : String {return phaseType.toString().toLowerCase()}

    fun getCamelName() : String {
        return when (phaseType) {
            PhaseType.COMMUNITY_CHECK -> "communityCheck"
            PhaseType.CONSULTANT_CHECK -> "consultantCheck"
            PhaseType.WHOLE_STORY -> "wholeStory"
            PhaseType.REMOTE_CHECK -> "remoteCheck"
            PhaseType.BACKT -> "backT"
            else -> phaseType.toString().toLowerCase()
        }
    }
    /**
     * get the color for the phase
     * @return return the color
     */
    fun getColor() : Int {
        return when(phaseType){
            PhaseType.LEARN -> R.color.learn_phase
            PhaseType.DRAFT -> R.color.draft_phase
            PhaseType.COMMUNITY_CHECK -> R.color.comunity_check_phase
            PhaseType.CONSULTANT_CHECK -> R.color.consultant_check_phase
            PhaseType.DRAMATIZATION -> R.color.dramatization_phase
            PhaseType.CREATE -> R.color.create_phase
            PhaseType.SHARE -> R.color.share_phase
            PhaseType.BACKT -> R.color.backT_phase
            PhaseType.WHOLE_STORY -> R.color.whole_story_phase
            PhaseType.REMOTE_CHECK -> R.color.remote_check_phase
        }
    }

    fun getTheClass() : Class<*> {
        return when(phaseType){
            PhaseType.LEARN -> LearnActivity::class.java
            PhaseType.DRAFT -> PagerBaseActivity::class.java
            PhaseType.COMMUNITY_CHECK -> PagerBaseActivity::class.java
            PhaseType.CONSULTANT_CHECK -> PagerBaseActivity::class.java
            PhaseType.DRAMATIZATION -> PagerBaseActivity::class.java
            PhaseType.CREATE -> CreateActivity::class.java
            PhaseType.SHARE -> ShareActivity::class.java
            PhaseType.BACKT -> PagerBaseActivity::class.java
            PhaseType.WHOLE_STORY -> WholeStoryBackTranslationActivity::class.java
            PhaseType.REMOTE_CHECK -> PagerBaseActivity::class.java
        }
    }
    companion object {
        fun getLocalPhases() : List<Phase> {
            return listOf(
                    Phase(PhaseType.LEARN),
                    Phase(PhaseType.DRAFT),
                    Phase(PhaseType.COMMUNITY_CHECK),
                    Phase(PhaseType.CONSULTANT_CHECK),
                    Phase(PhaseType.DRAMATIZATION),
                    Phase(PhaseType.CREATE),
                    Phase(PhaseType.SHARE))
        }

        fun getRemotePhases() : List<Phase> {
            return listOf(
                    Phase(PhaseType.LEARN),
                    Phase(PhaseType.DRAFT),
                    Phase(PhaseType.COMMUNITY_CHECK),
                    Phase(PhaseType.WHOLE_STORY),
                    Phase(PhaseType.BACKT),
                    Phase(PhaseType.REMOTE_CHECK),
                    Phase(PhaseType.DRAMATIZATION),
                    Phase(PhaseType.CREATE),
                    Phase(PhaseType.SHARE))
        }
    }
}
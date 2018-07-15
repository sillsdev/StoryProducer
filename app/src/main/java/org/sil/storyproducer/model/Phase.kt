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
     * Return chosen file.  Null if the current phase has no chosen file.
     */
    fun hasChosenFilename(): Boolean {return phaseType in listOf(PhaseType.DRAFT,PhaseType.COMMUNITY_CHECK,PhaseType.DRAMATIZATION,PhaseType.BACKT)}

    fun getChosenFilename(slideNum: Int = Workspace.activeSlideNum): String {
        return when(phaseType){
            PhaseType.LEARN -> Workspace.activeStory.learnAudioFile
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNum].chosenCommunityCheckFile
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile
            else -> ""
        }
    }

    fun setChosenFilename(filename: String, slideNum: Int = Workspace.activeSlideNum){
        when(phaseType){
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile = filename
            PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNum].chosenCommunityCheckFile = filename
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile = filename
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile = filename
            else -> return
        }
        return
    }

    val recordedAudioFiles: MutableList<String>?
    get(){
        return when (phaseType){
            PhaseType.DRAFT -> Workspace.activeSlide!!.draftAudioFiles
            PhaseType.COMMUNITY_CHECK -> Workspace.activeSlide!!.communityCheckAudioFiles
            PhaseType.DRAMATIZATION -> Workspace.activeSlide!!.dramatizationAudioFiles
            PhaseType.BACKT -> Workspace.activeSlide!!.backTranslationAudioFiles
            else -> null
        }
    }

    fun getReferenceAudioFile(slideNum: Int = Workspace.activeSlideNum) : String {
        return when (phaseType){
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].narrationFile
            PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            PhaseType.CONSULTANT_CHECK -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            else -> ""
        }

    }
    /**
     * get the title for the phase
     * @return return the title
     */
    fun getName() : String {return phaseType.toString().toLowerCase()}

    fun getPrettyName() : String {
        return when (phaseType) {
            PhaseType.LEARN -> "Learn"
            PhaseType.DRAFT -> "Draft"
            PhaseType.CREATE -> "Create"
            PhaseType.SHARE -> "Share"
            PhaseType.COMMUNITY_CHECK -> "Community Check"
            PhaseType.CONSULTANT_CHECK -> "Consultant Check"
            PhaseType.WHOLE_STORY -> "Whole Story"
            PhaseType.REMOTE_CHECK -> "Remote Check"
            PhaseType.BACKT -> "Back Translation"
            PhaseType.DRAMATIZATION -> "Dramatization"
            else -> phaseType.toString().toLowerCase()
        }
    }

    fun getShortName() : String {
        return when (phaseType) {
            PhaseType.COMMUNITY_CHECK -> "comChk"
            PhaseType.CONSULTANT_CHECK -> "cnsltChk"
            PhaseType.WHOLE_STORY -> "whlStry"
            PhaseType.REMOTE_CHECK -> "rmotChk"
            PhaseType.BACKT -> "backT"
            PhaseType.DRAMATIZATION -> "drama"
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
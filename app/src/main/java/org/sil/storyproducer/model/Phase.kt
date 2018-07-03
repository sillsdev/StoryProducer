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
    val hasChosenFilename: Boolean = phaseType in listOf(PhaseType.DRAFT,PhaseType.DRAMATIZATION,PhaseType.BACKT)
    var chosenFilename: String
    get(){
        return when(phaseType){
            PhaseType.LEARN -> Workspace.activeStory.learnAudioFile
            PhaseType.DRAFT -> Workspace.activeSlide!!.chosenDraftFile
            PhaseType.DRAMATIZATION -> Workspace.activeSlide!!.chosenDramatizationFile
            PhaseType.BACKT -> Workspace.activeSlide!!.chosenBackTranslationFile
            else -> ""
        }
    }
    set(value){
        when(phaseType){
            PhaseType.DRAFT -> Workspace.activeSlide!!.chosenDraftFile = value
            PhaseType.DRAMATIZATION -> Workspace.activeSlide!!.chosenDramatizationFile = value
            PhaseType.BACKT -> Workspace.activeSlide!!.chosenBackTranslationFile = value
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

    val referenceAudioFile: String
    get(){
        return when (phaseType){
            PhaseType.DRAFT -> Workspace.activeSlide!!.narrationFile
            PhaseType.COMMUNITY_CHECK -> Workspace.activeSlide!!.chosenDraftFile
            PhaseType.DRAMATIZATION -> Workspace.activeSlide!!.chosenDraftFile
            PhaseType.BACKT -> Workspace.activeSlide!!.chosenDraftFile
            else -> ""
        }
    }

    /**
     * get the title for the phase
     * @return return the title
     */
    fun getName() : String {return phaseType.toString().toLowerCase()}

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
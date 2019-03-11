package org.sil.storyproducer.model

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.export.CreateActivity
import org.sil.storyproducer.controller.export.ShareActivity
import org.sil.storyproducer.controller.learn.LearnActivity
import org.sil.storyproducer.controller.pager.PagerBaseActivity
import org.sil.storyproducer.controller.remote.WholeStoryBackTranslationActivity


enum class PhaseType {
    WORKSPACE, REGISTRATION, STORY_LIST, LEARN, DRAFT, COMMUNITY_CHECK, CONSULTANT_CHECK, DRAMATIZATION, CREATE, SHARE, BACKT, WHOLE_STORY, REMOTE_CHECK
}

/**
 * The business object for phases that are part of the story
 */
class Phase(val phaseType: PhaseType) {

    fun getChosenFilename(slideNum: Int = Workspace.activeSlideNum): String {
        return when(phaseType){
            PhaseType.LEARN -> Workspace.activeStory.learnAudioFile
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile
            else -> ""
        }
    }

    fun setChosenFilename(filename: String, slideNum: Int = Workspace.activeSlideNum){
        when(phaseType){
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile = filename
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile = filename
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile = filename
            else -> return
        }
        return
    }

    fun getRecordedAudioFiles(slideNum:Int = Workspace.activeSlideNum) : MutableList<String>? {
        return when (phaseType){
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].draftAudioFiles
            PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNum].communityCheckAudioFiles
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].dramatizationAudioFiles
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].backTranslationAudioFiles
            else -> null
        }
    }

    fun getIcon(phase: PhaseType = phaseType) : Int {
        return when (phase){
            PhaseType.LEARN -> R.drawable.ic_ear_white_48dp
            PhaseType.DRAFT -> R.drawable.ic_mic_white_48dp
            PhaseType.CREATE -> R.drawable.ic_video_call_white_48dp
            PhaseType.SHARE -> R.drawable.ic_share_white_48dp
            PhaseType.COMMUNITY_CHECK -> R.drawable.ic_people_white_48dp
            PhaseType.CONSULTANT_CHECK -> R.drawable.ic_school_white_48dp
            PhaseType.WHOLE_STORY -> R.drawable.ic_school_white_48dp
            PhaseType.REMOTE_CHECK -> R.drawable.ic_school_white_48dp
            PhaseType.BACKT -> R.drawable.ic_headset_mic_white_48dp
            PhaseType.DRAMATIZATION -> R.drawable.ic_mic_box_48dp
            else -> R.drawable.ic_mic_white_48dp
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

    fun getPrettyName() : String {
        return when (phaseType) {
            PhaseType.LEARN -> "Learn"
            PhaseType.DRAFT -> "Translate"
            PhaseType.CREATE -> "Finalize"
            PhaseType.SHARE -> "Share"
            PhaseType.COMMUNITY_CHECK -> "Community Work"
            PhaseType.CONSULTANT_CHECK -> "Accuracy Check"
            PhaseType.WHOLE_STORY -> "Whole Story"
            PhaseType.REMOTE_CHECK -> "Remote Check"
            PhaseType.BACKT -> "Back Translation"
            PhaseType.DRAMATIZATION -> "Voice Studio"
            else -> phaseType.toString().toLowerCase()
        }
    }

    fun getShortName() : String {
        return when (phaseType) {
            PhaseType.DRAFT -> "Translate"
            PhaseType.COMMUNITY_CHECK -> "Community"
            PhaseType.CONSULTANT_CHECK -> "Accuracy"
            PhaseType.WHOLE_STORY -> "Whole"
            PhaseType.REMOTE_CHECK -> "Remote"
            PhaseType.BACKT -> "BackTrans"
            PhaseType.DRAMATIZATION -> "VStudio"
            PhaseType.CREATE -> "Finalize"
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
            else -> R.color.black
        }
    }

    fun getTheClass() : Class<*> {
        return when(phaseType){
            PhaseType.WORKSPACE -> RegistrationActivity::class.java
            PhaseType.REGISTRATION -> RegistrationActivity::class.java
            PhaseType.STORY_LIST -> MainActivity::class.java
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

    fun getPhaseDisplaySlideCount() : Int {
        var tempSlideNum = 0
        val validSlideTypes = when(phaseType){
            PhaseType.DRAMATIZATION -> arrayOf(
                    SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG,SlideType.LOCALCREDITS)
            else -> arrayOf(
                    SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG)
        }
        for (s in Workspace.activeStory.slides)
            if(s.slideType in validSlideTypes){
                tempSlideNum++
            }else{
                break
            }
        return tempSlideNum
    }

    fun checkValidDisplaySlideNum(slideNum: Int) : Boolean {
        val slideType = Workspace.activeStory.slides[slideNum].slideType
        return when(phaseType){
            PhaseType.DRAMATIZATION -> slideType in arrayOf(
                    SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG,SlideType.LOCALCREDITS)
            else -> slideType in arrayOf(
                    SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG)
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

        fun getHelpName(phase: PhaseType) : String {
            return "${phase.name.toLowerCase()}.html"
        }
    }
}
package org.sil.storyproducer.model

import android.util.Log;
import org.sil.storyproducer.R

enum class PhaseType {
    LEARN,
    DRAFT,
    COMMUNITY_CHECK,
    CONSULTANT_CHECK,
    DRAMATIZATION,
    CREATE,
    SHARE,
    BACKT,
    WHOLE_STORY,
    REMOTE_CHECK;

    fun getRecordings(slideNumber: Int = Workspace.activeStory.lastSlideNum): RecordingList {
        return when (this) {
            PhaseType.DRAFT -> Workspace.activeStory.slides[slideNumber].draftRecordings
            PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNumber].communityCheckRecordings
            PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNumber].dramatizationRecordings
            PhaseType.BACKT -> Workspace.activeStory.slides[slideNumber].backTranslationRecordings
            else -> throw Exception("Unsupported phase to get a recordings list from")
        }
    }

    fun getIcon(): Int {
        return when (this) {
            PhaseType.LEARN -> R.drawable.ic_ear_speak
            PhaseType.DRAFT -> R.drawable.ic_mic_white_48dp
            PhaseType.CREATE -> R.drawable.ic_video_call_white_48dp
            PhaseType.SHARE -> R.drawable.ic_share_white_48dp
            PhaseType.COMMUNITY_CHECK -> R.drawable.ic_people_white_48dp
            PhaseType.CONSULTANT_CHECK -> R.drawable.ic_school_white_48dp
            PhaseType.WHOLE_STORY -> R.drawable.ic_story_tell_back_24dp
            PhaseType.REMOTE_CHECK -> R.drawable.ic_remote_check
            PhaseType.BACKT -> R.drawable.ic_slide_tell_back_24dp
            PhaseType.DRAMATIZATION -> R.drawable.ic_mic_box_48dp
        }
    }

    fun getReferenceRecording(slideNumber: Int = Workspace.activeStory.lastSlideNum): Recording? {
        val slide = Workspace.activeStory.slides[slideNumber]
        return when (this) {
            PhaseType.DRAFT -> slide.narration
            PhaseType.COMMUNITY_CHECK,
            PhaseType.CONSULTANT_CHECK,
            PhaseType.DRAMATIZATION,
            PhaseType.BACKT -> slide.draftRecordings.selectedFile
            PhaseType.REMOTE_CHECK -> slide.draftRecordings.selectedFile
            else -> throw Exception("Unsupported stage to get a reference audio file for")
        }
    }

    fun getPrettyName(): String {
        return when (this) {
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
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            PhaseType.DRAFT -> "Translation Draft"
            PhaseType.COMMUNITY_CHECK -> "Comment"
            PhaseType.CONSULTANT_CHECK -> "Accuracy"
            PhaseType.WHOLE_STORY -> "Whole"
            PhaseType.REMOTE_CHECK -> "Remote"
            PhaseType.BACKT -> "BackTrans"
            PhaseType.DRAMATIZATION -> "Studio Recording"
            PhaseType.CREATE -> "Finalize"
            else -> this.toString().toLowerCase()
        }
    }

    fun getShortName(): String {
        return when (this) {
            PhaseType.DRAFT -> "Translate"
            PhaseType.COMMUNITY_CHECK -> "Community"
            PhaseType.CONSULTANT_CHECK -> "Accuracy"
            PhaseType.WHOLE_STORY -> "Whole"
            PhaseType.REMOTE_CHECK -> "Remote"
            PhaseType.BACKT -> "BackTrans"
            PhaseType.DRAMATIZATION -> "VStudio"
            PhaseType.CREATE -> "Finalize"
            else -> this.toString().toLowerCase()
        }
    }

    /**
     * get the color for the phase
     * @return return the color
     */
    fun getColor(): Int {
        return when (this) {
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

    fun getPhaseDisplaySlideCount(): Int {
        var tempSlideNum = 0
        val validSlideTypes = when (this) {
            PhaseType.DRAMATIZATION -> arrayOf(
                    SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG, SlideType.LOCALCREDITS)
            else -> arrayOf(
                    SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG)
        }
        for (s in Workspace.activeStory.slides)
            if (s.slideType in validSlideTypes) {
                tempSlideNum++
            } else {
                break
            }
        return tempSlideNum
    }

    fun checkValidDisplaySlideNum(slideNumber: Int): Boolean {
        // TODO @pwhite: This is a pretty pointless function; would it be
        // possible to remove it? It is used in two places. One is to do a
        // sanity check, so it would only return false in that usage if there
        // is a bug. The other usage is to reset the slide number when a stage
        // is switch to that uses a different slideNumber. It would be good to
        // rethink the usages and see if there is a simpler and less
        // error-prone way to verify that slide numbers are valid.
        val slideType = Workspace.activeStory.slides[slideNumber].slideType
        return when (this) {
            PhaseType.DRAMATIZATION -> slideType in arrayOf(
                    SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG, SlideType.LOCALCREDITS)
            else -> slideType in arrayOf(
                    SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG)
        }
    }

    companion object {
        fun ofInt(i: Int): PhaseType {
            return PhaseType.values()[i]
        }

        fun getLocalPhases(): List<PhaseType> {
            return listOf(
                    PhaseType.LEARN,
                    PhaseType.DRAFT,
                    PhaseType.COMMUNITY_CHECK,
                    PhaseType.CONSULTANT_CHECK,
                    PhaseType.DRAMATIZATION,
                    PhaseType.CREATE,
                    PhaseType.SHARE)
        }

        fun getRemotePhases(): List<PhaseType> {
            return listOf(
                    PhaseType.LEARN,
                    PhaseType.DRAFT,
                    PhaseType.COMMUNITY_CHECK,
                    PhaseType.WHOLE_STORY,
                    PhaseType.BACKT,
                    PhaseType.REMOTE_CHECK,
                    PhaseType.DRAMATIZATION,
                    PhaseType.CREATE,
                    PhaseType.SHARE)
        }

        fun getHelpName(phase: PhaseType): String {
            return "${phase.name.toLowerCase()}.html"
        }
    }
}

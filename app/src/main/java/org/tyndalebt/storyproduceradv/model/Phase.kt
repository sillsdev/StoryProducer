package org.tyndalebt.storyproduceradv.model

import android.content.Context
import com.squareup.moshi.Json
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.controller.RegistrationActivity
import org.tyndalebt.storyproduceradv.controller.export.FinalizeActivity
import org.tyndalebt.storyproduceradv.controller.export.ShareActivity
import org.tyndalebt.storyproduceradv.controller.learn.LearnActivity
import org.tyndalebt.storyproduceradv.controller.pager.PagerBaseActivity
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksActivity
import java.io.*

/**
 * PhaseType enum used to track current phase
 *
 * @JSON annotations set the keys used by JSON serializing and deserializing.
 * At this time these keys are a match for SP v3.0.4 and previous story.json files.
 */
enum class PhaseType {
    @Json(name="WORKSPACE") WORKSPACE,
    @Json(name="REGISTRATION") REGISTRATION,
    @Json(name="STORY_LIST") STORY_LIST,
    @Json(name="LEARN") LEARN,
    @Json(name="DRAFT") TRANSLATE_REVISE,
    @Json(name="WORD_LINKS") WORD_LINKS,
    @Json(name="COMMUNITY_CHECK") COMMUNITY_WORK,
    @Json(name="WHOLE_STORY") WHOLE_STORY,
    @Json(name="BACKT") BACK_T,
    @Json(name="REMOTE_CHECK") REMOTE_CHECK,
    @Json(name="CONSULTANT_CHECK") ACCURACY_CHECK,
    @Json(name="DRAMATIZATION") VOICE_STUDIO,
    @Json(name="CREATE") FINALIZE,
    @Json(name="SHARE") SHARE
}

/**
 * Phase class used to get information relevant to different phases
 */
class Phase (val phaseType: PhaseType) {

    /**
     * Get the icon associated with a phase (used when creating the options menu)
     * @return drawable
     */
    fun getIcon(phase: PhaseType = phaseType) : Int {
        return when (phase){
            PhaseType.LEARN            -> R.drawable.ic_ear_speak
            PhaseType.TRANSLATE_REVISE -> R.drawable.ic_mic_white_48dp
            PhaseType.COMMUNITY_WORK   -> R.drawable.ic_people_white_48dp
            PhaseType.WHOLE_STORY      -> R.drawable.ic_question_answer_48dp
            PhaseType.BACK_T           -> R.drawable.ic_message_white_48dp
            PhaseType.REMOTE_CHECK     -> R.drawable.ic_school_white_48dp
            PhaseType.ACCURACY_CHECK   -> R.drawable.ic_school_white_48dp
            PhaseType.VOICE_STUDIO     -> R.drawable.ic_mic_box_48dp
            PhaseType.FINALIZE         -> R.drawable.ic_video_call_white_48dp
            PhaseType.SHARE            -> R.drawable.ic_share_icon_v2_white
            else -> R.drawable.ic_mic_white_48dp
        }
    }

    /**
     * Get the color associated with a phase
     * @return color
     */
    fun getColor() : Int {
        return when(phaseType){
            PhaseType.LEARN            -> R.color.learn_phase
            PhaseType.TRANSLATE_REVISE -> R.color.translate_revise_phase
            PhaseType.WORD_LINKS       -> R.color.word_links_phase
            PhaseType.COMMUNITY_WORK   -> R.color.community_work_phase
            PhaseType.WHOLE_STORY      -> R.color.whole_story_phase
            PhaseType.BACK_T           -> R.color.backT_phase
            PhaseType.REMOTE_CHECK     -> R.color.remote_check_phase
            PhaseType.ACCURACY_CHECK   -> R.color.accuracy_check_phase
            PhaseType.VOICE_STUDIO     -> R.color.voice_studio_phase
            PhaseType.FINALIZE         -> R.color.finalize_phase
            PhaseType.SHARE            -> R.color.share_phase
            else -> R.color.black
        }
    }

    /**
     * Get path of audio file needed by slide, based on the current phase (narration or a draft)
     * @return String
     */
    fun getReferenceAudioFile(slideNum: Int = Workspace.activeSlideNum) : String {
        val filename = when (phaseType){
            PhaseType.WHOLE_STORY -> Workspace.activeStory.slides[slideNum].narrationFile
            PhaseType.TRANSLATE_REVISE -> Workspace.activeStory.slides[slideNum].narrationFile
            PhaseType.COMMUNITY_WORK   -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
            PhaseType.BACK_T           -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
            PhaseType.REMOTE_CHECK     -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
            PhaseType.ACCURACY_CHECK   -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
            PhaseType.VOICE_STUDIO     -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
            else -> ""
        }
        return Story.getFilename(filename)
    }

    fun getLangDisplayName(context: Context) : String {
        val mResources = context.resources
        return when (phaseType) {
            PhaseType.REGISTRATION     -> mResources.getString(R.string.registration_title)
            PhaseType.LEARN            -> mResources.getString(R.string.learn_title)
            PhaseType.TRANSLATE_REVISE -> mResources.getString(R.string.translate_revise_title)
            PhaseType.WORD_LINKS       -> mResources.getString(R.string.title_activity_wordlink_list)
            PhaseType.COMMUNITY_WORK   -> mResources.getString(R.string.community_work_title)
            PhaseType.WHOLE_STORY      -> mResources.getString(R.string.whole_story_title)
            PhaseType.BACK_T           -> mResources.getString(R.string.back_translation_title)
            PhaseType.REMOTE_CHECK     -> mResources.getString(R.string.remote_check_title)
            PhaseType.ACCURACY_CHECK   -> mResources.getString(R.string.accuracy_check_title)
            PhaseType.VOICE_STUDIO     -> mResources.getString(R.string.voice_studio_title)
            PhaseType.FINALIZE         -> mResources.getString(R.string.finalize_title)
            PhaseType.SHARE            -> mResources.getString(R.string.share_title)
            else -> phaseType.toString().toLowerCase()
        }
    }
     // TODO: refactor (see Issues #546 & #547)

    /**
     * Get name for phase to be displayed in UI alert dialog with help docs
     * @return String
     */
    fun getDisplayName() : String {
        return when (phaseType) {
            PhaseType.REGISTRATION     -> "Registration"
            PhaseType.LEARN            -> "Learn"
            PhaseType.TRANSLATE_REVISE -> "Translate + Revise"
            PhaseType.WORD_LINKS       -> "Word Links"
            PhaseType.COMMUNITY_WORK   -> "Community Work"
            PhaseType.WHOLE_STORY      -> "Story Tell-Back"
            PhaseType.BACK_T           -> "Slide Tell-Back"
            PhaseType.REMOTE_CHECK     -> "Remote Check"
            PhaseType.ACCURACY_CHECK   -> "Accuracy Check"
            PhaseType.VOICE_STUDIO     -> "Voice Studio"
            PhaseType.FINALIZE         -> "Finalize"
            PhaseType.SHARE            -> "Share"
            else -> phaseType.toString().toLowerCase()
        }
    }

    /**
     * Get directory-safe name for phase, used to save audio files
     * @return String
     */
    fun getAudioFileDisplayName(context: Context) : String {
        val mResources = context.resources
        return when (phaseType) {
            PhaseType.TRANSLATE_REVISE -> mResources.getString(R.string.recordings_title)
            PhaseType.WORD_LINKS       -> mResources.getString(R.string.recordings_title)
            PhaseType.COMMUNITY_WORK   -> mResources.getString(R.string.recordings_title)
            PhaseType.WHOLE_STORY      -> mResources.getString(R.string.recordings_title)
            PhaseType.BACK_T           -> mResources.getString(R.string.recordings_title)
            PhaseType.REMOTE_CHECK     -> mResources.getString(R.string.recordings_title)
            PhaseType.ACCURACY_CHECK   -> mResources.getString(R.string.recordings_title)
            PhaseType.VOICE_STUDIO     -> mResources.getString(R.string.recordings_title)
            PhaseType.FINALIZE         -> mResources.getString(R.string.recordings_title)
            else -> phaseType.toString().toLowerCase()
        }
    }

    /**
     * Get directory-safe name for phase, used to save audio files
     * @return String
     */
    fun getDirectorySafeName() : String {
        return when (phaseType) {
            PhaseType.TRANSLATE_REVISE -> "Translation Draft"
            PhaseType.WORD_LINKS       -> "WordLinks"
            PhaseType.COMMUNITY_WORK   -> "Comment"
            PhaseType.WHOLE_STORY      -> "Whole"
            PhaseType.BACK_T           -> "BackTrans"
            PhaseType.REMOTE_CHECK     -> "Remote"
            PhaseType.ACCURACY_CHECK   -> "Accuracy"
            PhaseType.VOICE_STUDIO     -> "Studio Recording"
            PhaseType.FINALIZE         -> "Finalize"
            else -> phaseType.toString().toLowerCase()
        }
    }

    // 11/13/2021 - DKH, Issue 606, Wordlinks quick fix for text back translation
    // This piece of software is used in multiple places in Story Publisher Adv
    // This instructional string is appended to the generated display name
    fun getDisplayNameAdditionalInfo() : String {
        return when (phaseType) {
            PhaseType.WORD_LINKS       -> " --> Press and hold to back translate"
            else -> ""
        }
    }

    /**
     * Get file-safe name for phase, used to save audio files
     * @return String
     */
    fun getFileSafeName() : String {
        return when (phaseType) {
            PhaseType.TRANSLATE_REVISE -> "Translate"
            PhaseType.WORD_LINKS       -> "WordLinks"
            PhaseType.COMMUNITY_WORK   -> "Community"
            PhaseType.WHOLE_STORY      -> "Whole"
            PhaseType.BACK_T           -> "BackTrans"
            PhaseType.REMOTE_CHECK     -> "Remote"
            PhaseType.ACCURACY_CHECK   -> "Accuracy"
            PhaseType.VOICE_STUDIO     -> "VStudio"
            PhaseType.FINALIZE         -> "Finalize"
            else -> phaseType.toString().toLowerCase()
        }
    }

    /**
     * get the activity class of a phase (some based on the PagerBaseActivity)
     * @return Class
     */
    fun getTheClass() : Class<*> {
        return when(phaseType){
            PhaseType.WORKSPACE        -> RegistrationActivity::class.java
            PhaseType.REGISTRATION     -> RegistrationActivity::class.java
            PhaseType.STORY_LIST       -> MainActivity::class.java
            PhaseType.LEARN            -> LearnActivity::class.java
            PhaseType.TRANSLATE_REVISE -> PagerBaseActivity::class.java
            PhaseType.WORD_LINKS       -> WordLinksActivity::class.java
            PhaseType.COMMUNITY_WORK   -> PagerBaseActivity::class.java
            PhaseType.WHOLE_STORY      -> PagerBaseActivity::class.java
            PhaseType.BACK_T           -> PagerBaseActivity::class.java
            PhaseType.REMOTE_CHECK     -> PagerBaseActivity::class.java
            PhaseType.ACCURACY_CHECK   -> PagerBaseActivity::class.java
            PhaseType.VOICE_STUDIO     -> PagerBaseActivity::class.java
            PhaseType.FINALIZE         -> FinalizeActivity::class.java
            PhaseType.SHARE            -> ShareActivity::class.java
        }
    }

    /**
     * Get list of audio files associated with phase using slideNum
     * @return MutableList<String>
     */
    fun getCombNames(slideNum:Int = Workspace.activeSlideNum) : MutableList<String>?{
        return when (phaseType){
            PhaseType.TRANSLATE_REVISE -> Workspace.activeStory.slides[slideNum].translateReviseAudioFiles
            PhaseType.WORD_LINKS       -> {
                val audioFiles : MutableList<String> = mutableListOf()
                for(audioFile in Workspace.activeWordLink.wordLinkRecordings) {
                    audioFiles.add(audioFile.audioRecordingFilename)
                }
                audioFiles
            }
            PhaseType.COMMUNITY_WORK   -> Workspace.activeStory.slides[slideNum].communityWorkAudioFiles
            PhaseType.WHOLE_STORY   -> Workspace.activeStory.slides[slideNum].backTranslationAudioFiles
            PhaseType.BACK_T           -> Workspace.activeStory.slides[slideNum].backTranslationAudioFiles
            PhaseType.VOICE_STUDIO     -> Workspace.activeStory.slides[slideNum].voiceStudioAudioFiles
            else -> null
        }
    }

    fun getPhaseDisplaySlideCount() : Int {

        //Disabled horizontal scrolling on Whole Story Tell-Back
        if(phaseType == PhaseType.WHOLE_STORY){
            return 1
        }

        var tempSlideNum = 0
        val validSlideTypes = when(phaseType){
            PhaseType.VOICE_STUDIO -> arrayOf(
                    SlideType.FRONTCOVER,SlideType.NUMBEREDPAGE,
                    SlideType.LOCALSONG)
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
        // 02/03/2022 - DKH, Issue 596, Review Crash & Exception reports for v3.0.6
        // This routine determines if a slide is displayable, i.e. FRONTCOVER, NUMBEREDPAGE,
        // LOCALSONG are displayable and NONE, LOCALCREDITS, COPYRIGHT, ENDPAGE are not
        // When a null story is created, the active slide number is set to -1.  Routines
        // such as PagerBaseActivity.java call this routine by fetching the active slide number
        // (which can be -1 for a null story) from Workspace.INSTANCE.getActiveSlideNum()
        // and passing it in as an argument.
        // Previously, this routine did not check to see if the slide number actually existed
        // in the story.  We now check to see if the slide exist before trying to determine it's
        // type
        Workspace.activeStory.slides.getOrNull(slideNum)?.let {    // see if this is a valid slide number
            val slideType = it?.slideType  // slide exists, determine if it displayable
            return when (phaseType) {
                PhaseType.VOICE_STUDIO -> slideType in arrayOf(
                        SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                        SlideType.LOCALSONG)
                else -> slideType in arrayOf(
                        SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE,
                        SlideType.LOCALSONG)
            }
        }
        return false  // slide does not exist or is not displayable
    }

    companion object {
        fun getLocalPhases() : List<Phase> {
            return listOf(
                    Phase(PhaseType.LEARN),
                    Phase(PhaseType.TRANSLATE_REVISE),
                    Phase(PhaseType.COMMUNITY_WORK),
                    Phase(PhaseType.ACCURACY_CHECK),
                    Phase(PhaseType.VOICE_STUDIO),
                    Phase(PhaseType.FINALIZE),
                    Phase(PhaseType.SHARE))
        }

        fun getRemotePhases() : List<Phase> {
            return listOf(
                    Phase(PhaseType.LEARN),
                    Phase(PhaseType.TRANSLATE_REVISE),
                    Phase(PhaseType.COMMUNITY_WORK),
                    Phase(PhaseType.WHOLE_STORY),
                    Phase(PhaseType.BACK_T),
                    Phase(PhaseType.REMOTE_CHECK),
                    Phase(PhaseType.VOICE_STUDIO),
                    Phase(PhaseType.FINALIZE),
                    Phase(PhaseType.SHARE))
        }

        /**
         * get the filename for the HTML help doc
         * @return String
         */
        fun getHelpDocFileLang(phase: PhaseType, Language: String) : String {
            return "${Language}/${phase.name.toLowerCase()}.html"
        }

        fun openHelpDocFile(phase: PhaseType, Language: String, context: Context) : InputStream {
            // If the language specific one is available, use it, other use default help file
            val mLanguage = Workspace.readFromFile(context)
            var mFileName = getHelpDocFileLang(phase, Workspace.getLanguageCode(mLanguage!!))

            var iStream: InputStream
            try {
                iStream = context.assets.open(mFileName)
            } catch (e: Exception) {
                mFileName = getHelpDocFileLang(phase, "en")  // default to English
                iStream = context.assets.open(mFileName)
            }
            return iStream
        }

        //Currently unused and non-functional
        fun getReferenceRecording(slideNumber: Int = Workspace.activeStory.lastSlideNum, phaseType: PhaseType): Recording? {
            val slide = Workspace.activeStory.slides[slideNumber]
            return when (phaseType) {
                PhaseType.TRANSLATE_REVISE -> slide.narration
                PhaseType.COMMUNITY_WORK,
                PhaseType.ACCURACY_CHECK,
                PhaseType.VOICE_STUDIO,
                PhaseType.WHOLE_STORY -> slide.narration
                PhaseType.BACK_T -> slide.draftRecordings.selectedFile
                PhaseType.REMOTE_CHECK -> slide.draftRecordings.selectedFile
                else -> throw Exception("Unsupported stage to get a reference audio file for")
            }
        }
    }
}

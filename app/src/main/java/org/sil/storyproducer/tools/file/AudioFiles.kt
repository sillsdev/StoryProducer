package org.sil.storyproducer.tools.file

import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.StorySharedPreferences


import java.io.File
import java.nio.file.FileSystem
import java.text.SimpleDateFormat
import java.util.*

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */

internal val AUDIO_EXT = ".md4"
internal val AUDIO_APPEND_EXT = ".wav"
internal val DRAFT_PREFIX = "draft"
internal val BACKTRANSLATE_PREFIX = "backtranslate"
internal val LEARN_PRACTICE_FILE = "learnPractice$AUDIO_EXT"
internal val WHOLE_STORY_BACKT_FILE = "wholeStoryBackT$AUDIO_EXT"

internal val dtf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ROOT)

/**
 * Creates a relative path for recorded audio based upon the phase, slide number and timestamp.
 * Records the path in the story object.
 * If there is a failure in generating the path, an empty string is returned.
 * @return the path generated, or an empty string if there is a failure.
 */
fun assignNewAudioRelPath() : String {
    if(Workspace.activeStory.title == "") return ""
    val phase = Workspace.activePhase
    val phaseName = phase.getCamelName()
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.
    var relPath = "$PROJECT_DIR/" + phase.getCamelName() + "_" + Workspace.activeSlideNum.toString() + "_" + dtf.format(Date())

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    relPath += when(phase.phaseType){
        PhaseType.DRAFT, PhaseType.DRAMATIZATION -> AUDIO_APPEND_EXT
        else -> AUDIO_EXT
    }

    //register it in the story data structure.
    when(phase.phaseType){
        PhaseType.LEARN -> {Workspace.activeStory.learnAudioFile = relPath}
        PhaseType.DRAFT ->{
            Workspace.activeSlide!!.draftAudioFiles.add(relPath)
            Workspace.activeSlide!!.chosenDraftFile = relPath
        }
        PhaseType.COMMUNITY_CHECK -> {Workspace.activeSlide!!.communityCheckAudioFiles.add(relPath)}
        PhaseType.CONSULTANT_CHECK -> {Workspace.activeSlide!!.consultantCheckAudioFiles.add(relPath)}
        PhaseType.DRAMATIZATION -> {
            Workspace.activeSlide!!.dramatizationAudioFiles.add(relPath)
            Workspace.activeSlide!!.chosenDramatizationFile = relPath
        }
        PhaseType.WHOLE_STORY -> {Workspace.activeStory.wholeStoryBackTAudioFile = relPath}
        else -> relPath = ""
    }
    return relPath
}
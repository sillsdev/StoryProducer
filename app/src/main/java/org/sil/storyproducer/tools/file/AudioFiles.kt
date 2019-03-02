package org.sil.storyproducer.tools.file


import org.sil.storyproducer.model.KeytermRecording
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import kotlin.math.max

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */

internal const val AUDIO_EXT = ".m4a"

/**
 * Creates a relative path for recorded audio based upon the phase, slide number and timestamp.
 * Records the path in the story object.
 * If there is a failure in generating the path, an empty string is returned.
 * @return the path generated, or an empty string if there is a failure.
 */
fun assignNewAudioRelPath() : String {
    val phase = Workspace.activePhase
    val phaseName = phase.getShortName()
    if(Workspace.activeDirRoot == "") return ""
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.
    var relPath = ""
    val files = Workspace.activePhase.getRecordedAudioFiles()

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    when(phase.phaseType) {
        //just one file.  Overwrite when you re-record.
        PhaseType.LEARN, PhaseType.WHOLE_STORY -> {
            relPath = "$PROJECT_DIR/$phaseName$AUDIO_EXT"
        }
        //Make new files every time.  Don't append.
        PhaseType.DRAFT, PhaseType.COMMUNITY_CHECK,
        PhaseType.DRAMATIZATION, PhaseType.CONSULTANT_CHECK, PhaseType.KEYTERM -> {
            //find the next number that is available for saving files at.
            val rFileNum = "${Workspace.activeFilenameRoot}_([0-9]+)".toRegex()
            var maxNum = 0
            for (f in files!!){
                val num = rFileNum.find(f)
                if(num != null)
                    maxNum = max(maxNum,num.groupValues[1].toInt())
            }
            relPath = "${Workspace.activeDir}/${Workspace.activeFilenameRoot}_${maxNum+1}" + AUDIO_EXT
        }
        else -> {}
    }

    //register it in the story data structure.
    when(phase.phaseType){
        //just one file.
        PhaseType.LEARN -> {Workspace.activeStory.learnAudioFile = relPath}
        PhaseType.WHOLE_STORY -> {Workspace.activeStory.wholeStoryBackTAudioFile = relPath}
        //multiple files, no distinction.
        PhaseType.COMMUNITY_CHECK -> {
            Workspace.activeSlide!!.communityCheckAudioFiles.add(relPath)
        }
        PhaseType.CONSULTANT_CHECK -> {Workspace.activeSlide!!.consultantCheckAudioFiles.add(relPath)}
        //multiple files, one chosen.
        PhaseType.DRAFT ->{
            Workspace.activeSlide!!.draftAudioFiles.add(relPath)
            Workspace.activeSlide!!.chosenDraftFile = relPath
        }
        PhaseType.DRAMATIZATION -> {
            Workspace.activeSlide!!.dramatizationAudioFiles.add(relPath)
            Workspace.activeSlide!!.chosenDramatizationFile = relPath
        }
        PhaseType.KEYTERM -> {
            Workspace.activeKeyterm.keytermRecordings.add(0, KeytermRecording("", relPath))
            Workspace.activeKeyterm.chosenKeytermFile = relPath
        }
        else -> relPath = ""
    }
    return relPath
}

fun getTempAppendAudioRelPath():String {return "$PROJECT_DIR/temp$AUDIO_EXT"}

enum class RenameCode {
    SUCCESS,
    ERROR_LENGTH,
    ERROR_SPECIAL_CHARS,
    ERROR_UNDEFINED
}

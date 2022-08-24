package org.tyndalebt.storyproduceradv.tools.file


import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.PROJECT_DIR
import java.util.*
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

fun getChosenFilename(slideNum: Int = Workspace.activeSlideNum): String {
    return Story.getFilename(getChosenCombName(slideNum))
}

fun getChosenDisplayName(slideNum: Int = Workspace.activeSlideNum): String {
    return Story.getDisplayName(getChosenCombName(slideNum))
}

fun getChosenCombName(slideNum: Int = Workspace.activeSlideNum): String {
    return when (Workspace.activePhase.phaseType) {
        PhaseType.LEARN -> Workspace.activeStory.learnAudioFile
        PhaseType.TRANSLATE_REVISE -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile
        PhaseType.WORD_LINKS -> Workspace.activeWordLink.chosenWordLinkFile
        PhaseType.WHOLE_STORY -> Workspace.activeStory.wholeStoryBackTAudioFile
        PhaseType.BACK_T -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile
        PhaseType.REMOTE_CHECK -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile
        PhaseType.VOICE_STUDIO -> Workspace.activeStory.slides[slideNum].chosenVoiceStudioFile
        else -> ""
    }
}

/**
 * Setting to -1 clears the chosen file.
 */
fun setChosenFileIndex(index: Int, slideNum: Int = Workspace.activeSlideNum){
    val nameSize = Workspace.activePhase.getCombNames(slideNum)?.size ?: -1
    val combName = if(index < 0 || index >= nameSize) "" else Workspace.activePhase.getCombNames(slideNum)!![index]

    when(Workspace.activePhase.phaseType){
        PhaseType.TRANSLATE_REVISE -> Workspace.activeStory.slides[slideNum].chosenTranslateReviseFile = combName
        PhaseType.WORD_LINKS -> Workspace.activeWordLink.chosenWordLinkFile = combName
        PhaseType.VOICE_STUDIO -> Workspace.activeStory.slides[slideNum].chosenVoiceStudioFile = combName
        PhaseType.BACK_T -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile = combName
        else -> return
    }
    return
}

fun getRecordedDisplayNames(slideNum:Int = Workspace.activeSlideNum) : MutableList<String>? {
    val filenames : MutableList<String> = arrayListOf()
    val combNames = Workspace.activePhase.getCombNames(slideNum) ?: return filenames
    for (n in combNames){filenames.add(Story.getDisplayName(n))}
    return filenames
}

fun getRecordedAudioFiles(slideNum:Int = Workspace.activeSlideNum) : MutableList<String> {
    val filenames : MutableList<String> = arrayListOf()
    val combNames = Workspace.activePhase.getCombNames(slideNum) ?: return filenames
    for (n in combNames){filenames.add(Story.getFilename(n))}
    return filenames
}

fun assignNewAudioRelPath() : String {
    val combName = createRecordingCombinedName()
    addCombinedName(combName)
    return Story.getFilename(combName)
}

fun updateDisplayName(position:Int, newName:String) {
    // make sure to update the actual list, not a copy.
    val filenames = Workspace.activePhase.getCombNames() ?: return
    if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS) {
        // getCombNames() for WORD_LINKS creates a shallow copy
        //  so the recording needs to be found by position and updated
        Workspace.activeWordLink.wordLinkRecordings[position].audioRecordingFilename = "$newName|${Story.getFilename(filenames[position])}"
    } else {
        filenames[position] = "$newName|${Story.getFilename(filenames[position])}"
    }
}

/**
 * function implements deleteAudioFileFromList() for WordLinks
 *  necessary because WL recordings are WordLinkRecordings, not just string file names as with other phases
 */
fun deleteWLAudioFileFromList(context: Context, pos: Int) {
    val recordings = Workspace.activeWordLink.wordLinkRecordings
    val fileLocation = Story.getFilename(recordings[pos].audioRecordingFilename)
    val filename = recordings[pos].audioRecordingFilename

    recordings.removeAt(pos)
    if (getChosenCombName() == filename) {
        // current chosen WL has been deleted, shift the file index
        if (recordings.size == 0) {
            setChosenFileIndex(-1)
        }else {
            setChosenFileIndex(0)
        }
    }
    // delete the WL recording file
    deleteStoryFile(context, fileLocation)
}


/**
 * function removes file from list of recordings by position
 */
fun deleteAudioFileFromList(context: Context, pos: Int) {
    val recordings = Workspace.activePhase.getCombNames() ?: return
    val fileLocation = Story.getFilename(recordings[pos])
    val filename = recordings[pos]

    recordings.removeAt(pos)
    if (getChosenCombName() == filename) {
        // current chosen WL has been deleted, shift the file index
        if (recordings.size == 0) {
            setChosenFileIndex(-1)
        }else {
            setChosenFileIndex(0)
        }
    }
    // delete the recording file
    deleteStoryFile(context, fileLocation)
}

fun createRecordingCombinedName() : String {
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    return when(Workspace.activePhase.phaseType) {
        //just one file.  Overwrite when you re-record.
        PhaseType.LEARN,
        PhaseType.WHOLE_STORY -> {
            "${Workspace.activePhase.getDirectorySafeName()}|$PROJECT_DIR/${Workspace.activePhase.getFileSafeName()}$AUDIO_EXT"
        }
        //Make new files every time.  Don't append.
        PhaseType.TRANSLATE_REVISE,
        PhaseType.WORD_LINKS,
        PhaseType.COMMUNITY_WORK,
        PhaseType.BACK_T,
        PhaseType.VOICE_STUDIO,
        PhaseType.ACCURACY_CHECK -> {
            //find the next number that is available for saving files at.
            val names = getRecordedDisplayNames()
            val rNameNum = "${Workspace.activePhase.getDirectorySafeName()} ([0-9]+)".toRegex()
            var maxNum = 0
            for (n in names!!){
                try {
                    val num = rNameNum.find(n)
                    if (num != null)
                        maxNum = max(maxNum, num.groupValues[1].toInt())
                }catch(e: Exception){
                    //If there is a crash (such as a bad int parse) just keep going.
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
            // 11/13/2021 - DKH, Issue 606, Wordlinks quick fix for text back translation
            // Append the generated instructional string to the display name.
            var displayName = "${Workspace.activePhase.getDirectorySafeName()} ${maxNum+1}" +
                    Workspace.activePhase.getDisplayNameAdditionalInfo()

            // create the combined string of display name and audio file location
            "${displayName}|${Workspace.activeDir}/${Workspace.activeFilenameRoot}_${Date().time}$AUDIO_EXT"
        }
        else -> {""}
    }
}

fun addCombinedName(name:String){
    //register it in the story data structure.
    when(Workspace.activePhase.phaseType){
        //just one file.
        PhaseType.LEARN -> {Workspace.activeStory.learnAudioFile = name}
        PhaseType.WHOLE_STORY -> {Workspace.activeStory.wholeStoryBackTAudioFile = name}
        //multiple files, no distinction.
        //Add to beginning of list
        PhaseType.COMMUNITY_WORK -> {
            Workspace.activeSlide!!.communityWorkAudioFiles.add(0,name)
        }
        PhaseType.ACCURACY_CHECK -> {Workspace.activeSlide!!.accuracyCheckAudioFiles.add(0,name)}
        //multiple files, one chosen.
        PhaseType.TRANSLATE_REVISE ->{
            Workspace.activeSlide!!.translateReviseAudioFiles.add(0,name)
            Workspace.activeSlide!!.chosenTranslateReviseFile = name
        }
        PhaseType.BACK_T -> {
            Workspace.activeSlide!!.backTranslationAudioFiles.add(0,name)
            Workspace.activeSlide!!.chosenBackTranslationFile = name
        }
        PhaseType.VOICE_STUDIO -> {
            Workspace.activeSlide!!.voiceStudioAudioFiles.add(0, name)
            Workspace.activeSlide!!.chosenVoiceStudioFile = name
        }
        PhaseType.WORD_LINKS -> {
            Workspace.activeWordLink.wordLinkRecordings.add(0, WordLinkRecording(name))
            Workspace.activeWordLink.chosenWordLinkFile = name
        }
        else -> {}
    }
}

fun getTempAppendAudioRelPath():String {return "$PROJECT_DIR/temp$AUDIO_EXT"}


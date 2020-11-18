package org.sil.storyproducer.tools.file


import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
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
        PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile
        PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile
        PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile
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
        PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].chosenDraftFile = combName
        PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].chosenDramatizationFile = combName
        PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].chosenBackTranslationFile = combName
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
    //make sure to update the actual list, not a copy.
    val filenames = Workspace.activePhase.getCombNames() ?: return
    filenames[position] = "$newName|${Story.getFilename(filenames[position])}"
}

fun deleteAudioFileFromList(context: Context, pos: Int) {
    //make sure to update the actual list, not a copy.
    val filenames = Workspace.activePhase.getCombNames() ?: return
    val filename = Story.getFilename(filenames[pos])
    if(getChosenCombName() == filenames[pos]){
        //the chosen filename was deleted!  Make it some other selection.
        filenames.removeAt(pos)
        if(filenames.size == 0) {
            //If there is only 1 left, the resulting index will be -1, or no chosen filename.
            setChosenFileIndex(-1)
        }else{
            setChosenFileIndex(0)
        }
    }else{
        //just delete the file index.
        filenames.removeAt(pos)
    }
    deleteStoryFile(context,filename)
}

fun createRecordingCombinedName() : String {
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    return when(Workspace.activePhase.phaseType) {
        //just one file.  Overwrite when you re-record.
        PhaseType.LEARN, PhaseType.WHOLE_STORY -> {
            "${Workspace.activePhase.getDisplayName()}|$PROJECT_DIR/${Workspace.activePhase.getShortName()}$AUDIO_EXT"
        }
        //Make new files every time.  Don't append.
        PhaseType.DRAFT, PhaseType.COMMUNITY_CHECK,
        PhaseType.DRAMATIZATION, PhaseType.CONSULTANT_CHECK -> {
            //find the next number that is available for saving files at.
            val names = getRecordedDisplayNames()
            val rNameNum = "${Workspace.activePhase.getDisplayName()} ([0-9]+)".toRegex()
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
            "${Workspace.activePhase.getDisplayName()} ${maxNum+1}|${Workspace.activeDir}/${Workspace.activeFilenameRoot}_${Date().time}$AUDIO_EXT"
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
        PhaseType.COMMUNITY_CHECK -> {
            Workspace.activeSlide!!.communityCheckAudioFiles.add(0,name)
        }
        PhaseType.CONSULTANT_CHECK -> {Workspace.activeSlide!!.consultantCheckAudioFiles.add(0,name)}
        //multiple files, one chosen.
        PhaseType.DRAFT ->{
            Workspace.activeSlide!!.draftAudioFiles.add(0,name)
            Workspace.activeSlide!!.chosenDraftFile = name
        }
        PhaseType.DRAMATIZATION -> {
            Workspace.activeSlide!!.dramatizationAudioFiles.add(0,name)
            Workspace.activeSlide!!.chosenDramatizationFile = name
        }
        else -> {}
    }
}

fun getTempAppendAudioRelPath():String {return "$PROJECT_DIR/temp$AUDIO_EXT"}


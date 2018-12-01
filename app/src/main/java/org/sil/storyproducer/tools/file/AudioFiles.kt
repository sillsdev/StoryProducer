package org.sil.storyproducer.tools.file

import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.StorySharedPreferences


import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */

internal val AUDIO_EXT = ".m4a"

internal val dtf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT)

/**
 * Creates a relative path for recorded audio based upon the phase, slide number and timestamp.
 * Records the path in the story object.
 * If there is a failure in generating the path, an empty string is returned.
 * @return the path generated, or an empty string if there is a failure.
 */
fun assignNewAudioRelPath() : String {
    if(Workspace.activeStory.title == "") return ""
    val phase = Workspace.activePhase
    val phaseName = phase.getShortName()
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.
    var relPath: String = ""
    val files = Workspace.activePhase.getRecordedAudioFiles()

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    when(phase.phaseType) {
        //just one file.  Overwrite when you re-record.
        PhaseType.LEARN, PhaseType.WHOLE_STORY -> {
            relPath = "$PROJECT_DIR/$phaseName" + AUDIO_EXT
        }
        //Make new files every time.  Don't append.
        PhaseType.DRAFT, PhaseType.COMMUNITY_CHECK,
        PhaseType.DRAMATIZATION, PhaseType.CONSULTANT_CHECK -> {
            //find the next number that is available for saving files at.
            val rFileNum = "$PROJECT_DIR/$phaseName${Workspace.activeSlideNum}_([0-9]+)".toRegex()
            var maxNum = 0
            for (f in files!!){
                val num = rFileNum.find(f)
                if(num != null)
                    maxNum = max(maxNum,num.groupValues[1].toInt())
            }
            relPath = "$PROJECT_DIR/$phaseName${Workspace.activeSlideNum}_${maxNum+1}" + AUDIO_EXT
        }
        PhaseType.KEYTERM -> {
            //find the next number that is available for saving files at.
            val rFileNum = "${Workspace.activeKeyterm.term}_${Workspace.activeKeyterm.term.hashCode()}/${Workspace.activeKeyterm.term}_([0-9]+)".toRegex()
            var maxNum = 0
            for (f in files!!){
                val num = rFileNum.find(f)
                if(num != null)
                    maxNum = max(maxNum,num.groupValues[1].toInt())
            }
            relPath = "${Workspace.activeKeyterm.term}_${Workspace.activeKeyterm.term.hashCode()}/${Workspace.activeKeyterm.term}_${maxNum+1}" + AUDIO_EXT
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
            Workspace.activeKeyterm.backTranslations.add(BackTranslation(mutableListOf(), relPath))
            Workspace.activeKeyterm.chosenKeytermFile = relPath
        }
        else -> relPath = ""
    }
    return relPath
}

fun getTempAppendAudioRelPath():String {return "$PROJECT_DIR/temp$AUDIO_EXT"}

//TODO delete everything below.
internal const val WAV_EXTENSION = ".wav"
internal const val WHOLESTORY_AUDIO_PREFIX = "wholestory"

object AudioFiles {
    private val PREFER_EXTENSION = ".m4a"

    private val DRAFT_AUDIO_PREFIX = "translation"
    private val COMMENT_PREFIX = "comment"
    private val DRAMATIZATION_AUDIO_PREFIX = "dramatization"

    private val BACKTRANSLATION_AUDIO_PREFIX = "backtranslation"

    private enum class ModalType {
        DRAFT, COMMUNITY, DRAMATIZATION, BACKTRANSLATION, WHOLESTORY
    }

    enum class RenameCode {
        SUCCESS,
        ERROR_LENGTH,
        ERROR_SPECIAL_CHARS,
        ERROR_UNDEFINED
    }



    //*** WSBT ***
    fun getWholeStory(story: String): File {
        return File(FileSystem.getProjectDirectory(story), WHOLESTORY_AUDIO_PREFIX + PREFER_EXTENSION)
    }

    @JvmOverloads
    fun getDraft(story: String, slide: Int, draftTitle: String = StorySharedPreferences.getDraftForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), DRAFT_AUDIO_PREFIX + slide + "_" + draftTitle + PREFER_EXTENSION)
    }



    //*** Community Check ***

    fun getComment(story: String, slide: Int, commentTitle: String): File {
        return File(FileSystem.getProjectDirectory(story), COMMENT_PREFIX + slide + "_" + commentTitle + PREFER_EXTENSION)
    }



    @JvmOverloads
    fun getBackTranslation(story: String, slide: Int, backTitle: String = StorySharedPreferences.getBackTranslationForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), BACKTRANSLATION_AUDIO_PREFIX + slide + "_" + backTitle + WAV_EXTENSION)
    }



    /**
     * deletes the designated audio dramatization
     * @param story the story the dramatization comes from
     * @param slide the slide the dramatization comes from
     * @param draftTitle the name of the dramatization in question
     */
    fun deleteBackTranslation(story: String, slide: Int, draftTitle: String) {
        val file = getBackTranslation(story, slide, draftTitle)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * renames the designated audio dramatization if the new name is valid and the file exists
     * @param story the story the dramatization comes from
     * @param slide the slide of the story the dramatization comes from
     * @param oldTitle the old title of the dramatization
     * @param newTitle the proposed new title for the dramatization
     * @return returns success or error code of renaming
     */
    fun renameBackTranslation(story: String, slide: Int, oldTitle: String, newTitle: String): RenameCode {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.BACKTRANSLATION, PREFER_EXTENSION)
    }

    /**
     * Returns a list of dramatization titles for the story and slide in question
     * @param story the story where the dramatization come from
     * @param slide the slide where the dramatization come from
     * @return the array of dramatization titles
     */
    fun getBackTranslationTitles(story: String, slide: Int): Array<String> {
        return getRecordingTitlesHelper(story, slide, BACKTRANSLATION_AUDIO_PREFIX, WAV_EXTENSION)
    }

    @JvmOverloads
    fun getDramatization(story: String, slide: Int, dramaTitle: String = StorySharedPreferences.getDramatizationForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), DRAMATIZATION_AUDIO_PREFIX + slide + "_" + dramaTitle + WAV_EXTENSION)
    }




    //**** Helpers ***//
    private fun renameAudioFileHelper(story: String, slide: Int, oldTitle: String, newTitle: String, type: ModalType, extension: String): RenameCode {
        // Requirements for file names:
        //        - must be under 20 characters
        //        - must be only contain alphanumeric characters or spaces/underscores
        if (newTitle.length > 20) {
            return RenameCode.ERROR_LENGTH
        }
        if (!newTitle.matches("[A-Za-z0-9\\s_]+".toRegex())) {
            return RenameCode.ERROR_SPECIAL_CHARS
        }
        var file = getComment(story, slide, oldTitle)
        when (type) {
        //set the file based on the different file types
            AudioFiles.ModalType.DRAFT -> file = getDraft(story, slide, oldTitle)
            AudioFiles.ModalType.COMMUNITY -> file = getComment(story, slide, oldTitle)
            AudioFiles.ModalType.DRAMATIZATION -> file = getDramatization(story, slide, oldTitle)
            AudioFiles.ModalType.BACKTRANSLATION -> file = getBackTranslation(story, slide, oldTitle)
        }

        var renamed = false
        if (file.exists()) {
            val newPathName = file.absolutePath.replace(oldTitle + extension, newTitle + extension)
            val newFile = File(newPathName)
            if (!newFile.exists()) {
                renamed = file.renameTo(newFile)
            }
        }
        return if (renamed) {
            RenameCode.SUCCESS
        } else {
            RenameCode.ERROR_UNDEFINED
        }
    }

    private fun getRecordingTitlesHelper(story: String, slide: Int, prefix: String, extension: String): Array<String> {
        val titles = ArrayList<String>()
        val storyDirectory = FileSystem.getProjectDirectory(story)
        val storyDirectoryFiles = storyDirectory.listFiles()
        for (storyDirectoryFile in storyDirectoryFiles) {
            val filename = storyDirectoryFile.getName()
            if (filename.startsWith(prefix + slide + "_") && filename.endsWith(extension)) {
                titles.add(getTitleFromPath(filename, prefix, extension))
            }
        }
        val returnTitlesArray = arrayOfNulls<String>(titles.size)
        return titles.toTypedArray()
    }

    /**
     * Extract title from path.
     */
    private fun getTitleFromPath(filename: String, prefix: String, extension: String): String {
        //Note: Assume no dots in filename.
        return filename.replaceFirst("$prefix\\d+_".toRegex(), "").replace(extension, "")
    }

}

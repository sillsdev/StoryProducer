package org.sil.storyproducer.model


import android.os.Build
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.model.logging.LogEntry
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


internal const val PROJECT_DIR = "project"
internal const val VIDEO_DIR = "videos"
internal const val PROJECT_FILE = "story.json"
internal val RE_TITLE_NUMBER = "([0-9]+[A-Za-z]?)?[_ -]*(.+)".toRegex()
internal val RE_DISPLAY_NAME = "([^|]+)[|.]".toRegex()
internal val RE_FILENAME = "([^|]+[|])?(.*)".toRegex()

@JsonClass(generateAdapter = true)

class Story(var title: String, var slides: List<Slide>){

    // DKH - 6/7/2021 Merge conflict resolution (next 3 lines placed here) - needed for Issue 407 (Story Filter)
    enum class StoryType {
        OLD_TESTAMENT, NEW_TESTAMENT, OTHER;
    }

    // DKH - Updated 06/02/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
    // Record versionCode & versionName which come from build.gradle (Module: StoreyProducer.app)
    // Record timeStamp for when story.json file was written
    // This will allow future Story Producer Apps to be backwards compatibility with old stories
    // This will also allow for debugging of stories that have parse errors

    // These are the initial story default values and will be updated from a story.json file if
    // function storyFromJason is called and the story.json file contains these fields
    // These values are also updated from function "Story.ToJason" when it is time to
    // update the story.json file
    var storyToJasonAppVersionCode = 0  // default value - no value available
    var storyToJasonAppVersionName = "" // default value - no value available
    var storyToJasonTimeStamp = ""  // default value - no value available

    var isApproved: Boolean = false
    var learnAudioFile = ""
    var wholeStoryBackTAudioFile = ""
    var wholeStoryBackTranslationUploadState = UploadState.NOT_UPLOADED
    var activityLogs: MutableList<LogEntry> = ArrayList()
    var outputVideos: MutableList<String> = ArrayList()
    var lastPhaseType: PhaseType = PhaseType.LEARN
    var lastSlideNum: Int = 0
    var remoteId: Int? = null
    var importAppVersion = ""
    var localCredits = ""

    val inProgress: Boolean get() {
        for(slide in slides){
            if(slide.translateReviseAudioFiles.isNotEmpty()) {
                return true;
            }
        }
        return false;
    }

    val isComplete: Boolean get() {
        return outputVideos.isNotEmpty()
    }

    val type : StoryType get() {
        return try {
            // Get number from story
            when (title.split("")[1].toInt()) {
                0 -> StoryType.OTHER
                1 -> StoryType.OLD_TESTAMENT
                2 -> StoryType.NEW_TESTAMENT
                else -> StoryType.OTHER
            }
        } catch(e : NumberFormatException) {
            StoryType.OTHER
        }
    }

    val shortTitle: String get() {
        val match = RE_TITLE_NUMBER.find(title)
        return if(match != null){
            match.groupValues[2]
        } else {
            title
        }
    }
    val titleNumber: String get() {
        val match = RE_TITLE_NUMBER.find(title)
        return if(match != null){
            match.groupValues[1]
        } else {
            "N/A"
        }
    }

    fun addVideo(video: String){
        if(!(video in outputVideos)){
            outputVideos.add(video)
            outputVideos.sort()
        }
    }

    companion object{
        fun getDisplayName(combName:String): String {
            val match = RE_DISPLAY_NAME.find(combName)
            return if(match != null){ match.groupValues[1] } else {""}
        }
        fun getFilename(combName:String): String {
            val match = RE_FILENAME.find(combName)
            return if(match != null){ match.groupValues[2] } else {""}
        }
    }


}

fun emptyStory() : Story {return Story("",ArrayList())}


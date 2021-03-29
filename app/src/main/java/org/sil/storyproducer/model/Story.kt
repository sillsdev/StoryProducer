package org.sil.storyproducer.model


import android.content.Context
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.R
import org.sil.storyproducer.model.logging.LogEntry
import java.util.*

internal const val PROJECT_DIR = "project"
internal const val VIDEO_DIR = "videos"
internal const val PROJECT_FILE = "story.json"
internal val RE_TITLE_NUMBER = "([0-9]+[A-Za-z]?)?[_ -]*(.+)".toRegex()
internal val RE_DISPLAY_NAME = "([^|]+)[|.]".toRegex()
internal val RE_FILENAME = "([^|]+[|])?(.*)".toRegex()

@JsonClass(generateAdapter = true)
class Story(var title: String, var slides: List<Slide>) {

    enum class StoryType {
        OLD_TESTAMENT, NEW_TESTAMENT, OTHER;

        fun getFilterName(type: StoryType, context : Context): String {
            return when(type){
                OLD_TESTAMENT -> context.getString(R.string.ot_toolbar)
                NEW_TESTAMENT -> context.getString(R.string.nt_toolbar)
                OTHER -> context.getString(R.string.other_toolbar)
                else -> "Empty"
            }
        }
    }

    var isApproved: Boolean = false
    var learnAudioFile = ""
    var wholeStoryBackTAudioFile = ""
    var activityLogs: MutableList<LogEntry> = ArrayList()
    var outputVideos: MutableList<String> = ArrayList()
    var lastPhaseType: PhaseType = PhaseType.LEARN
    var lastSlideNum: Int = 0
    var importAppVersion = ""
    var localCredits = ""
    var inProgress: Boolean = false
        get() {
            for(slide in slides){
                if(slide.translateReviseAudioFiles.isNotEmpty()) {
                    return true;
                }
            }
            return false;
        }
    var isComplete: Boolean = false
        get() {
            return outputVideos.isNotEmpty()
        }

    // FILTER TODO: make transient, also make sure values are correct
    val type : StoryType
        get() {
            return try {
                // Get number from story
                val storyNumber = shortTitle.split(" ")[0].toInt()
                when {
                    storyNumber < 200 -> StoryType.OLD_TESTAMENT
                    storyNumber < 300 -> StoryType.NEW_TESTAMENT
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


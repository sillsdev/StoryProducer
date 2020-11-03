package org.sil.storyproducer.model


import com.squareup.moshi.JsonClass
import org.sil.storyproducer.model.logging.LogEntry
import java.util.*

internal const val PROJECT_DIR = "project"
internal const val VIDEO_DIR = "videos"
internal const val PROJECT_FILE = "story.json"
internal val RE_TITLE_NUMBER = "([0-9]+[A-Za-z]?)?[_ -]*(.+)".toRegex()
internal val RE_DISPLAY_NAME = "([^|]+)[|.]".toRegex()
internal val RE_FILENAME = "([^|]+[|])?(.*)".toRegex()

@JsonClass(generateAdapter = true)
class Story(var title: String, val slides: List<Slide>){

    var importAppVersion: String = ""
    var isApproved: Boolean = false
    var learnAudioFile = ""
    var wholeStoryBackTAudioFile = ""
    var activityLogs: MutableList<LogEntry> = ArrayList()
    var outputVideos: MutableList<String> = ArrayList()
    var lastPhaseType: PhaseType = PhaseType.LEARN
    var lastSlideNum: Int = 0
    var fullVideo: String = ""
    var isVideoStory = false
    var localCredits = ""
    val numSlides : Int get() {
        var num = 0
        for(slide in slides) {
            if(slide.slideType == SlideType.NUMBEREDPAGE || slide.slideType == SlideType.FRONTCOVER) {
                num++
            }
        }
        return num
    }

    var phases: List<Phase> = ArrayList()

    fun getPhaseNames() : List<String> {
        val list = mutableListOf<String>()
        for(phase in phases) {
            list.add(phase.getPrettyName())
        }
        return list
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
        if(video !in outputVideos){
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

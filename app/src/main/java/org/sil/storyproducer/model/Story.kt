package org.sil.storyproducer.model


import com.squareup.moshi.JsonClass
import org.sil.storyproducer.model.logging.LogEntry

import java.util.*

internal const val PROJECT_DIR = "project"
internal const val VIDEO_DIR = "videos"
internal const val PROJECT_FILE = "story.json"

@JsonClass(generateAdapter = true)
class Story(var title: String, val slides: List<Slide>){

    var learnAudioFile = ""
    var wholeStoryBackTAudioFile = ""
    var activityLogs: MutableList<LogEntry> = ArrayList()
    var lastPhaseType: PhaseType = PhaseType.LEARN
    var lastSlideNum: Int = 0
    companion object

    //TODO replace the "size-1" with this.  Will there be templates without a last slide?
    fun numberOfContentSlides() : Int{
        var num = 0
        for(s in slides){
            if((s.imageFile != "") and (s.content != "")) num++
        }
        return num
    }
}

fun emptyStory() : Story {return Story("",ArrayList())}


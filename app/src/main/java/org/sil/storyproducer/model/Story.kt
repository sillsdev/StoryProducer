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
    var outputVideos: MutableList<String> = ArrayList()
    var lastPhaseType: PhaseType = PhaseType.LEARN
    var lastSlideNum: Int = 0
    companion object

    fun addVideo(video: String){
        if(!(video in outputVideos)){
            outputVideos.add(video)
            outputVideos.sort()
        }
    }

    fun getVideoTitle() : String {
        var a = "hello.txt"
        val ovNoPath : MutableList<String> = ArrayList()
        for (ov in outputVideos){
            ovNoPath.add(ov.split(".")[0])
        }
        a.split(".")[0]
        var i = 1
        if(!((title) in ovNoPath)) return title
        for(i in 1..100){
            val temp = "${title}_$i"
            if(!((temp) in ovNoPath)) return temp
        }
        return ""
    }
}

fun emptyStory() : Story {return Story("",ArrayList())}


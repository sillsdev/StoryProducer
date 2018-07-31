package org.sil.storyproducer.model

import android.graphics.Rect
import android.util.Log
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.model.logging.LogEntry
import org.sil.storyproducer.tools.file.FileSystem

import org.sil.storyproducer.tools.file.ProjectXML
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect
import org.sil.storyproducer.tools.media.graphics.RectHelper
import java.io.*

import java.util.*

internal const val PROJECT_DIR = "project"
internal const val VIDEO_DIR = "videos"
internal const val PROJECT_FILE = "story.json"

@JsonClass(generateAdapter = true)
class Story(var title: String, val slides: List<Slide>){

    var learnAudioFile = ""
    var wholeStoryBackTAudioFile = ""
    var activityLogs: MutableList<LogEntry> = ArrayList()
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


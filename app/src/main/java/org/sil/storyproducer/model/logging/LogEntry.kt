package org.sil.storyproducer.model.logging

import android.content.Context
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import java.text.SimpleDateFormat
import java.util.*

@JsonClass(generateAdapter = true)
class LogEntry(var dateTimeString: String,
               var description: String, var phase: PhaseType,
               var startSlideNum: Int = -1, var endSlideNum: Int = -1) {

    fun appliesToSlideNum(compareNum: Int): Boolean {
        if (phase == PhaseType.LEARN)
            if(compareNum in startSlideNum..endSlideNum ||
               compareNum in endSlideNum..startSlideNum)
                return true
        if (startSlideNum == compareNum) return true
        return false
    }

}

fun saveLearnLog(context: Context, startSlide: Int, endSlide: Int, duration: Long, isRecording: Boolean = false){
    val mResources = context.resources
    var ret = if(isRecording){"Record "}else{"Playback "}

    ret += if (startSlide == endSlide) {
        mResources.getQuantityString(R.plurals.logging_numSlides, 1) + " " + (startSlide)
    } else {
        mResources.getQuantityString(R.plurals.logging_numSlides, 2) + " " + (startSlide) + "-" + (endSlide)
    }
    //format duration:
    val secUnit = mResources.getString(R.string.SECONDS_ABBREVIATION)
    val minUnit = mResources.getString(R.string.MINUTES_ABBREVIATION)
    if (duration < 1000) {
        ret += " (<1 $secUnit)"
    }else {
        val roundedSecs = (duration / 1000.0 + 0.5).toInt()
        val mins = roundedSecs / 60
        var minString = ""
        if (mins > 0) {
            minString = mins.toString() + " " + minUnit + " "
        }
        ret += " (" + minString + roundedSecs % 60 + " " + secUnit + ")"
    }
    saveLog(ret,startSlide,endSlide)
}

fun saveLog(description: String,startSlideNum: Int = Workspace.activeStory.lastSlideNum, endSlideNum: Int = Workspace.activeStory.lastSlideNum) {
    val dateTimeString = SimpleDateFormat("EEE MMM dd yyyy h:mm a", Locale.US).format(GregorianCalendar().time)
    val phase = Workspace.activeStory.lastPhaseType

    val le = LogEntry(dateTimeString,
            description, phase, startSlideNum,endSlideNum)
    Workspace.activeStory.activityLogs.add(le)
}

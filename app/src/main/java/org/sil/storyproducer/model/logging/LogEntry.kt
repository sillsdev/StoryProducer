package org.sil.storyproducer.model.logging

import android.content.Context
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import java.util.GregorianCalendar

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@JsonClass(generateAdapter = true)
class LogEntry(var dateTimeString: String,
               var description: String, var phase: Phase, var slideNum: Int = -1) {

    fun appliesToSlideNum(compareNum: Int): Boolean {
        if (phase.phaseType == PhaseType.LEARN)
            return true
        if (slideNum == compareNum) return true
        return false
    }

}

fun saveLearnLog(context: Context, startSlide: Int, endSlide: Int, duration: Long){
    val mResources = context.resources
    var ret: String
    if (startSlide == endSlide) {
        ret = mResources.getQuantityString(R.plurals.logging_numSlides, 1) + " " + (startSlide + 1)
    } else {
        ret = mResources.getQuantityString(R.plurals.logging_numSlides, 2) + " " + (startSlide + 1) + "-" + (endSlide + 1)
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
    saveLog(ret)
}

fun saveLog(description: String) {
    val dateTimeString = SimpleDateFormat("EEE MMM dd yyyy h:mm a", Locale.US).format(GregorianCalendar().time)
    val phase = Workspace.activePhase
    val slideNum = Workspace.activeSlideNum

    val le = LogEntry(dateTimeString,
            description, phase, slideNum)
    Workspace.activeStory.activityLogs.add(le)
}
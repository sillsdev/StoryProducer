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
    var exportedVideos: MutableList<String> = ArrayList()
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

/**
 * Get the slide from the story template at the specified index.
 * @param story
 * @param index
 * @return requested slide or null if not found.
 */

fun TemplateSlides(story: String, index: Int): TemplateSlide? {
    val slides = createSlidesFromProjectXML(story)
    return if (slides != null && slides.size > index) {
        slides[index]
    } else null
}

/**
 * Convert a story's ProjectXML into a list of TemplateSlide.
 * @param story
 * @return list of story's slides or null if unable to read/parse project.xml.
 */
private fun createSlidesFromProjectXML(story: String?): List<TemplateSlide>? {
    val templatePath = FileSystem.getTemplatePath(story)

    val xml: ProjectXML
    try {
        xml = ProjectXML(story)
    } catch (e: Exception) {
        Log.e("Temaplate", "Error reading or parsing project.xml file!", e)
        return null
    }

    val slides = ArrayList<TemplateSlide>()

    for (i in 0..xml.units.size-1) {
        val unit = xml.units[i]

        val narrationPath = unit.narrationFilename
        val narration = if (narrationPath == null) null else File(templatePath, narrationPath)

        val imagePath = unit.imageInfo.filename

        val width = unit.imageInfo.width
        val height = unit.imageInfo.height
        val imageDimensions = Rect(0, 0, width, height)

        val start = unit.imageInfo.motion.start
        //Ensure the rectangle fits within the image.
        RectHelper.clip(start, imageDimensions)

        val end = unit.imageInfo.motion.end
        //Ensure the rectangle fits within the image.
        RectHelper.clip(end, imageDimensions)

        //TODO: Should we use crop here? (Are start and end relative to crop or absolute?)
        var crop: Rect? = null
        if (unit.imageInfo.edit != null) {
            crop = unit.imageInfo.edit.crop
        }
        val kbfx = KenBurnsEffect(start, end, crop)

        var soundtrack: File? = null
        var soundtrackVolume = 0
        if (i > 0) {
            val previous = slides[i - 1]
            soundtrack = previous.soundtrack
            soundtrackVolume = previous.soundtrackVolume
        }
        if (unit.imageInfo.musicTrack != null) {
            val soundtrackPath = unit.imageInfo.musicTrack.filename
            soundtrack = File(templatePath, soundtrackPath)
            soundtrackVolume = unit.imageInfo.musicTrack.volume
        }

        val currentSlide = TemplateSlide(narration,
                File(imagePath), imageDimensions, kbfx, soundtrack, soundtrackVolume)
        slides.add(currentSlide)
    }

    return slides
}

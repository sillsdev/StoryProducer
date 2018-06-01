package org.sil.storyproducer.model

import android.graphics.Rect
import android.os.Environment
import android.util.Log
import android.util.Xml
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass

import org.xmlpull.v1.XmlPullParser

import org.sil.storyproducer.tools.file.FileSystem
import org.sil.storyproducer.tools.file.ProjectXML
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect
import org.sil.storyproducer.tools.media.graphics.RectHelper
import org.xmlpull.v1.XmlPullParserException

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*


private val PROJECT_DIR = "project"
private val PROJECT_FILE = "story.json"

@JsonClass(generateAdapter = true)
data class Story(
        val path: File,
        val slides: List<Slide>)
{
    val projectPath = File(path,PROJECT_DIR)
    val projectFile = File(projectPath,PROJECT_FILE)

    fun writeToJson(){
        val moshi = Moshi
                .Builder()
                .add(FileAdapter())
                .add(RectAdapter())
                .build()
        val adapter = Story.jsonAdapter(moshi)
        //TODO It still fails because it cannot write to SD card.  I don't know what is wrong.
        projectFile.createNewFile()
        projectFile.writeText(adapter.toJson(this))
    }
    companion object
}

fun parseStoryIfPresent(path: File): Story? {
    var story: Story?
    //Check if path is path
    if(!path.isDirectory) return null
    val projectPath = File(path,PROJECT_DIR)
    //make a project directory if there is none.
    if (projectPath.isDirectory) {
        //parse the project file, if there is one.
//        story = Story.fromJson(File(projectPath,PROJECT_FILE))
        //if there is a story from the file, do not try to read any templates, just return.
//        if(story != null) return story
    }
    projectPath.mkdir()
    //See if there is an xml photostory file there
    story = parsePhotoStory(path)
    //TODO If not, See if there is an html bloom file there
    //write the story (if it is not null) to json.
    if(story != null) story.writeToJson()
    return story
}

private fun parsePhotoStory(path: File): Story? {
    //start building slides
    val psProjectPath = File(path,"project.xml")
    if(!psProjectPath.exists()) return null
    //The file "project.xml" is there, it is a photostory project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(FileInputStream(psProjectPath),null)
    parser.nextTag()
    parser.require(XmlPullParser.START_TAG, null, "MSPhotoStoryProject")
    parser.next()
    while ((parser.eventType != XmlPullParser.END_TAG) || (parser.name != "MSPhotoStoryProject")) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
            continue
        }
        val tag = parser.name
        when (tag) {
            "VisualUnit" -> slides.add(parseSlideXML(parser))
            else -> {
                //skip the tag
                var depth: Int = 1
                while (depth != 0) {
                    when (parser.next()) {
                        XmlPullParser.END_TAG -> depth--
                        XmlPullParser.START_TAG -> depth++
                    }
                }
            }
        }
        parser.next()
    }
    return Story(path,slides)
}

@Throws(XmlPullParserException::class, IOException::class)
private fun parseSlideXML(parser: XmlPullParser): Slide {
    val slide = Slide()

    parser.require(XmlPullParser.START_TAG, null, "VisualUnit")
    parser.next()
    while ((parser.eventType != XmlPullParser.END_TAG) || (parser.name != "VisualUnit")) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
            continue
        }
        val tag = parser.name
        when (tag) {
            "Narration" -> {
                slide.narrationPath = File(parser.getAttributeValue(null, "path"))
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "Narration")
            }
            "Image" -> {
                slide.imagePath = File(parser.getAttributeValue(null, "path"))
                slide.width = Integer.parseInt(parser.getAttributeValue(null, "width"))
                slide.height = Integer.parseInt(parser.getAttributeValue(null, "height"))
            }
            "RotateAndCrop" -> {
                parser.nextTag()
                slide.crop = parseRect(parser, "Rectangle")
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "RotateAndCrop")
            }
            "MusicTrack" -> {
                slide.volume = Integer.parseInt(parser.getAttributeValue(null, "volume"))

                parser.nextTag()
                parser.require(XmlPullParser.START_TAG, null, "SoundTrack")

                slide.musicPath = File(parser.getAttributeValue(null, "path"))

                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "SoundTrack")

                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "MusicTrack")
            }
            "Motion" -> {
                val rectTag = "Rect"
                parser.nextTag()
                slide.startMotion = parseRect(parser, rectTag)
                parser.nextTag()
                slide.endMotion = parseRect(parser, rectTag)

                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "Motion")
            }
        //ignore
            else -> {
                //skip the tag
                var depth: Int = 1
                while (depth != 0) {
                    when (parser.next()) {
                        XmlPullParser.END_TAG -> depth--
                        XmlPullParser.START_TAG -> depth++
                    }
                }
            }
        }
        parser.next()
    }
    return slide
}

@Throws(IOException::class, XmlPullParserException::class)
private fun parseRect(parser: XmlPullParser, rectangleTag: String): Rect {
    parser.require(XmlPullParser.START_TAG, null, rectangleTag)

    val left = Integer.parseInt(parser.getAttributeValue(null, "upperLeftX"))
    val top = Integer.parseInt(parser.getAttributeValue(null, "upperLeftY"))
    val width = Integer.parseInt(parser.getAttributeValue(null, "width"))
    val height = Integer.parseInt(parser.getAttributeValue(null, "height"))

    val right = left + width
    val bottom = top + height

    parser.nextTag()
    parser.require(XmlPullParser.END_TAG, null, rectangleTag)

    return Rect(left, top, right, bottom)
}


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

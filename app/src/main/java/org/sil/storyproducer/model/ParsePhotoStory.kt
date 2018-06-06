package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Rect
import android.support.v4.provider.DocumentFile
import android.util.Xml
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Story
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.util.ArrayList

fun parsePhotoStoryXML(context: Context, storyPath: DocumentFile): Story? {
    //See if there is an xml photostory file there
    val psxml = storyPath.findFile("project.xml")
    if(psxml == null) return null
    val xmlContents = context.contentResolver.openInputStream(psxml.uri)
    //The file "project.xml" is there, it is a photostory project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(xmlContents,null)
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
            "VisualUnit" -> {
                var slide = parseSlideXML(parser)
                //open up text file that has title, ext.  They are called 0.txt, 1.txt, etc.
                val textFile = storyPath.findFile(slide.textFile)
                if(textFile != null){
                    val iStream = context.contentResolver.openInputStream(textFile.uri)
                    val fileContents = iStream.reader().use { it.readText() }
                    val textList = fileContents.split("~")
                    if (textList.size > 0) slide.title = textList[0]
                    if (textList.size > 1) slide.subtitle= textList[1]
                    if (textList.size > 2) slide.reference = textList[2]
                    if (textList.size > 3) slide.content = textList[3]
                }
                slides.add(slide)
            }
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
    val story = Story(storyPath.uri,slides)
    story.setContext(context)
    return story
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
                slide.narrationFile = parser.getAttributeValue(null, "path")
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "Narration")
            }
            "Image" -> {
                slide.imageFile = parser.getAttributeValue(null, "path")
                val noExtRange = 0..(slide.imageFile.length
                        - File(slide.imageFile).extension.length-2)
                slide.textFile = slide.imageFile.slice(noExtRange) + ".txt"
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

                slide.musicFile = parser.getAttributeValue(null, "path")

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
package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Rect
import android.support.v4.provider.DocumentFile
import android.util.Xml
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryText
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.util.ArrayList

fun parsePhotoStoryXML(context: Context, storyPath: DocumentFile): Story? {
    //See if there is an xml photostory file there
    val xmlContents = getStoryChildInputStream(context,"project.xml",storyPath.name!!) ?: return null
    //The file "project.xml" is there, it is a photostory project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(xmlContents,null)
    parser.nextTag()
    parser.require(XmlPullParser.START_TAG, null, "MSPhotoStoryProject")
    parser.next()
    var firstSlide = true
    while ((parser.eventType != XmlPullParser.END_TAG) || (parser.name != "MSPhotoStoryProject")) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
            continue
        }
        val tag = parser.name
        when (tag) {
            "VisualUnit" -> {
                val slide = parseSlideXML(parser)
                if(firstSlide) {
                    slide.slideType = SlideType.FRONTCOVER
                    firstSlide = false
                }
                //open up text file that has title, ext.  They are called 0.txt, 1.txt, etc.
                val textFile = getStoryText(context,slide.textFile,storyPath.name!!)
                if(textFile != null){
                    val textList = textFile.split("~")
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

    //Assume that the final slide is actually the copyright slide
    slides.last().slideType = SlideType.COPYRIGHT
    //The copyright slide should show the copyright in the LWC.
    slides.last().translatedContent = slides.last().content

    //Add the song slide
    var slide = Slide()
    slide.slideType = SlideType.LOCALSONG
    slide.content = context.getString(R.string.LS_prompt)
    //add as second last slide
    slides.add(slides.size-1,slide)

    //Add the Local credits slide
    slide = Slide()
    slide.slideType = SlideType.LOCALCREDITS
    //add as second last slide
    slides.add(slides.size-1,slide)

    return Story(storyPath.name!!,slides)
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
            "Edit" -> {
                //Do nothing.  There could either be "Text Overlay" or "rotate and crop" information.
                //This node is here just to look further down the tree.
            }
            "TextOverlay" -> {
                //This is only for first slides and credit slides, although the content of the
                //first slide is overwritten by the text.
                slide.content = parser.getAttributeValue(null, "text")
            }
            "RotateAndCrop" -> {
                parser.nextTag()
                slide.crop = parseRect(parser, "Rectangle")
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "RotateAndCrop")
            }
            "MusicTrack" -> {
                //TODO fix volume reading.. How to convert from an int (9) to a float (ratio of 1?)?
                //slide.volume = Integer.parseInt(parser.getAttributeValue(null, "volume")).toDouble()
                slide.volume = 0.25f

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
package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Rect
import android.support.v4.provider.DocumentFile
import android.util.Log
import android.util.Xml
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryText
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.ArrayList

fun parsePhotoStoryXML(context: Context, storyPath: DocumentFile): Story? {
    Log.e("@pwhite", "Parsing photo story xml ${storyPath.name}")
    //See if there is an xml photostory file there
    val xmlContents = getStoryChildInputStream(context, "project.xml", storyPath.name!!)
            ?: return null
    //The file "project.xml" is there, it is a photostory project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(xmlContents, null)
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
                val slide = parseSlideXML(context, parser, storyPath)
                //open up text file that has title, ext.  They are called 0.txt, 1.txt, etc.
                val textFile = getStoryText(context, slide.textFile, storyPath.name!!)
                if (textFile != null) {
                    val textList = textFile.split("~")
                    if (textList.size > 0) slide.title = textList[0].removePrefix(" ").removeSuffix(" ")
                    if (textList.size > 1) slide.subtitle = textList[1].removePrefix(" ").removeSuffix(" ")
                    if (textList.size > 2) slide.reference = textList[2].removePrefix(" ").removeSuffix(" ")
                    if (textList.size > 3) slide.content = textList[3].trim()
                }
                if (firstSlide) {
                    slide.slideType = SlideType.FRONTCOVER
                    firstSlide = false
                }
                slides.add(slide)
            }
            else -> {
                skipToNextTag(parser)
            }
        }
        parser.next()
    }

    if (slides.isEmpty()) {
        return null
    }

    //Assume that the final slide is actually the copyright slide
    slides.last().slideType = SlideType.COPYRIGHT
    //The copyright slide should show the copyright in the LWC.
    slides.last().translatedContent = slides.last().content
    slides.last().musicFile = MUSIC_NONE

    //Add the song slide
    var slide = Slide()
    slide.slideType = SlideType.LOCALSONG
    slide.content = context.getString(R.string.LS_prompt)
    slide.musicFile = MUSIC_NONE
    //add as second last slide
    slides.add(slides.size - 1, slide)

    //Add the Local credits slide
    slide = Slide()
    slide.slideType = SlideType.LOCALCREDITS
    slide.content = context.getString(R.string.LC_prompt)
    slide.musicFile = MUSIC_NONE
    //add as second last slide
    slides.add(slides.size - 1, slide)

    return Story(storyPath.name!!, slides)
}

@Throws(XmlPullParserException::class, IOException::class)
private fun parseSlideXML(context: Context, parser: XmlPullParser, storyPath: DocumentFile): Slide {
    Log.e("@pwhite", "parsing a slide")
    val slide = Slide()

    parser.require(XmlPullParser.START_TAG, null, "VisualUnit")
    parser.next()
    while ((parser.eventType != XmlPullParser.END_TAG) || (parser.name != "VisualUnit")) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
            continue
        }
        when (parser.name) {
            "Narration" -> {
                Log.e("@pwhite", "Found a narration")
                val path = parser.getAttributeValue(null, "path")
                slide.narration = if (storyRelPathExists(context, path, storyPath.name!!)) {
                    Recording(path, "narration")
                } else {
                    null
                }
                Log.e("@pwhite", "Found path in xml file - path = $path, narration = ${slide.narration}")
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "Narration")
            }
            "Image" -> parseImage(slide, parser)
            else -> skipToNextTag(parser)
        }
        parser.next()
    }
    return slide
}

private fun parseImage(slide: Slide, parser: XmlPullParser) {
    slide.imageFile = parser.getAttributeValue(null, "path")
    slide.textFile = slide.imageFile.replace(Regex("""\..+$"""), ".txt")
    slide.width = Integer.parseInt(parser.getAttributeValue(null, "width"))
    slide.height = Integer.parseInt(parser.getAttributeValue(null, "height"))
    parser.nextTag()
    while (parser.name != "Image") {
        when (parser.name) {
            "Edit" -> {
                parseEdit(parser, slide)
            }
            "MusicTrack" -> {
                parseMusicTrack(slide, parser)
            }
            "Motion" -> {
                parseMotion(parser, slide)
            }
        }
        parser.next()
    }
    parser.require(XmlPullParser.END_TAG, null, "Image")
}

private fun parseEdit(parser: XmlPullParser, slide: Slide) {
    parser.nextTag()
    while (parser.name != "Edit") {
        when (parser.name) {
            "RotateAndCrop" -> {
                parser.nextTag()
                slide.crop = parseRect(parser, "Rectangle")
                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "RotateAndCrop")
            }
            "TextOverlay" -> {
                //This is only for first slides and credit slides, although the content of the
                //first slide is overwritten by the text.
                slide.content = parser.getAttributeValue(null, "text")
                skipToNextTag(parser)
                parser.require(XmlPullParser.END_TAG, null, "TextOverlay")
            }
        }
        parser.next()
    }
    parser.require(XmlPullParser.END_TAG, null, "Edit")
}

private fun parseMusicTrack(slide: Slide, parser: XmlPullParser) {
    val rawVolume = Integer.parseInt(parser.getAttributeValue(null, "volume"))
    val normalizedVolume = (rawVolume.toFloat() / 100.0f)
    slide.volume = normalizedVolume

    parser.nextTag()
    if (parser.name == "SoundTrack") {
        parser.require(XmlPullParser.START_TAG, null, "SoundTrack")

        slide.musicFile = parser.getAttributeValue(null, "path")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, null, "SoundTrack")

        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, null, "MusicTrack")
    }
}

private fun parseMotion(parser: XmlPullParser, slide: Slide) {
    val rectTag = "Rect"
    parser.nextTag()
    slide.startMotion = parseRect(parser, rectTag)
    parser.nextTag()
    slide.endMotion = parseRect(parser, rectTag)

    parser.nextTag()
    parser.require(XmlPullParser.END_TAG, null, "Motion")
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

private fun skipToNextTag(parser: XmlPullParser) {
    var depth = 1
    while (depth != 0) {
        when (parser.next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
        }
    }
}
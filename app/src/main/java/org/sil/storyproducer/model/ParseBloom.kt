package org.sil.storyproducer.model

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.arthenica.mobileffmpeg.FFmpeg
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.*
import java.io.File
import java.util.*


fun parseBloomHTML(context: Context, storyPath: DocumentFile): Story? {
    //See if there is a BLOOM html file there
    val childDocs = storyPath.listFiles()
    var html_name = ""
    for (d in childDocs) {
        val f = d.name
        if (f == null)
            continue
        if (f.endsWith(".html") || f.endsWith(".htm")){
            html_name = f
            continue    // why continue and not break?
        }
    }
    if(html_name == "") return null
//    val htmlText = getText(context,"${storyPath.name}/$html_name") ?: return null
    val htmlText = storyPath.findFile(html_name)?.let {
            htmlFile -> getDocumentText(context, htmlFile) } ?: return null
    //The file "index.html" is there, it is a Bloom project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val story = Story(storyPath.name!!, slides)
    story.importAppVersion = BuildConfig.VERSION_NAME

    val soup = Jsoup.parse(htmlText)

    //add the title slide
    val frontCoverSlideBuilder = BloomFrontCoverSlideBuilder()
    frontCoverSlideBuilder.build(context, storyPath, soup)?.also {
        slides.add(it)
    } ?: return null

    val lang = frontCoverSlideBuilder.lang

    var slide = Slide()
    val pages = soup.getElementsByAttributeValueContaining("class","numberedPage")
    if(pages.size <= 2) return null
    for (page in pages) {
        if (page.attr("class").contains("numberedPage")) {
            NumberedPageSlideBuilder().build(context, storyPath, page, lang)?.also {
                slides.add(it)
            }
        }
    }

    //Add the song slide
    slide = Slide()
    slide.slideType = SlideType.LOCALSONG
    slide.content = context.getString(R.string.LS_prompt)
    slide.musicFile = MUSIC_NONE
    slides.add(slide)

    //Before the first page is the bloomDataDiv stuff.  Get the originalAcknowledgments.
    //If they are there, append to the end of the slides.
    val mOrgAckns = soup.getElementsByAttributeValueMatching("class","(?=.*bloom-translationGroup)(?=.*originalAcknowledgments)")
    if(mOrgAckns.size >= 1){
        val mOrgAckn = mOrgAckns[0]
        slide = Slide()
        slide.slideType = SlideType.COPYRIGHT
        val mOAParts = mOrgAckn.getElementsByAttributeValueContaining("class","bloom-editable")
        slide.content = ""
        for(p in mOAParts){
            slide.content += p.wholeText()
        }
        //cleanup whitespace
        slide.content = slide.content.trim().replace("\\s*\\n\\s*".toRegex(),"\n")
        slide.translatedContent = slide.content
        slide.musicFile = MUSIC_NONE
        slides.add(slide)
    }

    return story
}

//Image and transition pattern
val reRect = "([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+)".toRegex()

fun parsePage(context: Context, frontCoverGraphicProvided: Boolean, page: Element, slide: Slide, storyPath: DocumentFile): Boolean {
    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true

    val audios = page.getElementsByAttributeValueContaining("class", "audio-sentence")

    //narration
    if (slide.narrationFile.isEmpty()) {
        if (audios.size >= 1) {
            if (audios.size > 1) {
                // Using ffmpeg to concatenate multiple sentences in one page
                // ffmpeg -f concat -safe 0 -i mylist.txt -c copy output.mp3
                // but first we need to copy the source audio files to internal storage
                // so that ffmpeg can access them.
                var totalInputAudioFiles = 0
                for (i in 0 until audios.size) {
                    if (audios[i].hasAttr("data-duration"))
                        totalInputAudioFiles++
                }
                if (totalInputAudioFiles > 1) {
                    var concatTempFolder = File(context.filesDir, "temp_concat")
                    concatTempFolder.deleteRecursively()
                    concatTempFolder.mkdirs()
                    var concatTempFile = File(concatTempFolder,"temp_audio_concat_list.txt")
                    var totalInputFiles = 0
                    for (i in 0 until audios.size) {
                        var audioStoryDoc = storyPath.findFile("audio")?.let {
                            it.findFile("${audios[i].id()}.mp3")
                        } ?: continue
                        if (audioStoryDoc.exists() && audioStoryDoc.length() > 0) {
                            val audioFileOut = File(concatTempFolder, "/${audios[i].id()}.mp3")
                            copyToFilesDir(context, audioStoryDoc.uri, audioFileOut)
                            if (audioFileOut.exists() && audioFileOut.length() > 0) {
                                // escape any single quotes in file name
                                val audioFileStrOut = audioFileOut.path.replace("'", "'\\''")
                                // input file is a list of source files to concat starting with "file " and single quoted
                                concatTempFile.appendText("file '${audioFileStrOut}'\n")
                                totalInputFiles++
                            }
                        }
                    }
                    if (totalInputFiles > 0) {
                        var ffmpegArgs: MutableList<String> = mutableListOf()
                        ffmpegArgs.add("-f")
                        ffmpegArgs.add("concat")
                        ffmpegArgs.add("-safe")
                        ffmpegArgs.add("0")
                        ffmpegArgs.add("-i")
                        ffmpegArgs.add(concatTempFile.absolutePath)
                        ffmpegArgs.add("-c")
                        ffmpegArgs.add("copy")
                        val concatTempOutputFileStr = "${concatTempFolder}/${audios[0].id()}_output.mp3"
                        ffmpegArgs.add(concatTempOutputFileStr)
                        FFmpeg.execute(ffmpegArgs.toTypedArray())
                        Log.w("ParseBloom.parsePage", FFmpeg.getLastCommandOutput() ?: "No FFMPEG output")
                        val audioStoryConcatDocFind = storyPath.findFile("audio")?.let {
                            it.findFile("${audios[0].id()}_output.mp3") }
                        if (audioStoryConcatDocFind != null)
                            audioStoryConcatDocFind.delete()    // delete the output file if it already exists
                        var audiostoryConcatDoc = storyPath.findFile("audio")?.let {
                            it.createFile("audio/mpeg","${audios[0].id()}_output.mp3") }
                        if (audiostoryConcatDoc != null) {
                            // now copy the concatenated output file to the Story audio subfolder
                            copyFromFilesDir(context, File(concatTempOutputFileStr), audiostoryConcatDoc.uri)
                            slide.narrationFile = "audio/${audios[0].id()}_output.mp3"
                        }
                    }
                    concatTempFolder.deleteRecursively()
                }
                if (totalInputAudioFiles == 1)
                    slide.narrationFile = "audio/${audios[0].id()}.mp3"
            } else {
                slide.narrationFile = "audio/${audios[0].id()}.mp3"
            }
        } else {
            return false
        }
    }

    if (!slide.isFrontCover() && !slide.isNumberedPage()) {
        slide.content = ""
        for (a in audios) {
            slide.content += a.wholeText()
        }
        slide.content = slide.content.trim().replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    //soundtrack
    val soundtrack = page.getElementsByAttribute("data-backgroundaudio")
    if(soundtrack.size >= 1){
        slide.musicFile = "audio/${soundtrack[0].attr("data-backgroundaudio")}"
        // DKH - 07/23/2021
        // Issue #585: SP fails to read new templates made with Story Producer Template Maker
        // The attr method on class Node (ie, soundtrack[0] object) does not return a null but either
        // the attribute string or an empty sting
        // The following method throws this exception: "java.lang.NumberFormatException: empty String",
        // because it tries to do a ".toFloat()" on an empty string
        // slide.volume = (soundtrack[0].attr("data-backgroundaudiovolume") ?: "0.25").toFloat()
        // Replace the previous line of code with the following, which checks for string length
        // to determine if we have an empty string

        // grab the node attribute
        val slideVolume = soundtrack[0].attr("data-backgroundaudiovolume")
        if(slideVolume.length == 0) { // if the attribute length is zero, we have an empty string
            slide.volume = 0.25F     // assign a default volume
        }else {
            // convert slideVolume string to a float
            slide.volume = slideVolume.toFloat()
        }
    }

    //image
    val images = page.getElementsByAttributeValueContaining("class","bloom-imageContainer")
    if(images.size >= 1){
        val image = images[0]
        if (!slide.isFrontCover() || frontCoverGraphicProvided) {
            slide.imageFile = image.attr("src")
            if (slide.imageFile == "") {
                //bloomd books store the image in a different location
                slide.imageFile = image.attr("style")
                //typical format: background-image:url('1.jpg')
                slide.imageFile = slide.imageFile.substringAfter("'").substringBefore("'")
            }
            if (slide.imageFile == "") {
                val src = image.getElementsByAttribute("src")
                if (src.size >= 1) slide.imageFile = src[0].attr("src")
            }
        }
        BitmapFactory.decodeFileDescriptor(getStoryFileDescriptor(context,slide.imageFile,"image/*","r",storyPath.name!!), null, bmOptions)
        slide.height = bmOptions.outHeight
        slide.width = bmOptions.outWidth
        // Now keeping default start and end Ken Burns motion settings null for enhanced default behaviour
//        slide.startMotion = Rect(0, 0, slide.width, slide.height)
//        slide.endMotion = Rect(0, 0, slide.width, slide.height)

        val mSR = reRect.find(image.attr("data-initialrect"))
        if(mSR != null) {
            val x = mSR.groupValues[1].toDouble()*slide.width
            val y = mSR.groupValues[2].toDouble()*slide.height
            val w = mSR.groupValues[3].toDouble()*slide.width
            val h = mSR.groupValues[4].toDouble()*slide.height
            slide.startMotion = Rect((x).toInt(), //left
                    (y).toInt(),  //top
                    (x+w).toInt(),   //right
                    (y+h).toInt())  //bottom
        }
        val mER = reRect.find(image.attr("data-finalrect"))
        if(mER != null) {
            val x = mER.groupValues[1].toDouble()*slide.width
            val y = mER.groupValues[2].toDouble()*slide.height
            val w = mER.groupValues[3].toDouble()*slide.width
            val h = mER.groupValues[4].toDouble()*slide.height
            slide.endMotion = Rect((x).toInt(), //left
                    (y).toInt(),  //top
                    (x+w).toInt(),   //right
                    (y+h).toInt())  //bottom
        }
    }
    return true
}


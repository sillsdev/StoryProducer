package org.sil.storyproducer.model

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.arthenica.mobileffmpeg.FFmpeg
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
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
            break
        }
    }
    if(html_name == "") return null
    val htmlText = storyPath.findFile(html_name)?.let {
            htmlFile -> getDocumentText(context, htmlFile) } ?: return null
    //The file "index.html" is there, it is a Bloom project.  Parse it.
    val slides: MutableList<Slide> = ArrayList()
    val story = Story(storyPath.name!!, slides)
    story.importAppVersion = BuildConfig.VERSION_NAME

    val soup = Jsoup.parse(htmlText)

    // Here we generate additional parameters to the Bloom HTML parsing methods.
    // This is so that DocumentFile.listFiles() and DocumentFile.findFile() methods
    // are called the minimum number of times as they are quite slow to use:
    var storyAudioPath = storyPath.findFile("audio") ?: return null
    var storyAudioFiles = storyAudioPath.listFiles()

    // In addition storyAudioMap enables a faster access to individual
    // audio DocumentFile instances in the "audio" subfolder:
    var storyAudioMap : MutableMap<String, DocumentFile> = mutableMapOf()
    for (storyAudioFile in storyAudioFiles) {
        storyAudioMap[storyAudioFile.name!!] = storyAudioFile
    }

    //add the title slide
    val frontCoverSlideBuilder = BloomFrontCoverSlideBuilder()
    frontCoverSlideBuilder.build(context, storyPath, storyAudioPath, storyAudioMap, soup)?.also {
        slides.add(it)
    } ?: return null

    val lang = frontCoverSlideBuilder.lang
    story.langCode = lang

    val isSPAuthored = frontCoverSlideBuilder.isSPAuthored

    var slide = Slide()
    val pages = soup.getElementsByAttributeValueContaining("class","numberedPage")
    if(pages.size <= 2) return null
    NumberedPageSlideBuilder.prevPageImage = "" // no previous images

    for (page in pages) {
        NumberedPageSlideBuilder()
                .build(context, storyPath, storyAudioPath, storyAudioMap, page, lang, isSPAuthored)?.also {
            slides.add(it)
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

fun parsePage(context: Context, frontCoverGraphicProvided: Boolean, page: Element, slide: Slide, storyPath: DocumentFile, storyAudioPath: DocumentFile, storyAudioMap: MutableMap<String, DocumentFile>, lang: String): Boolean {
    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true

    val audios = page.getElementsByAttributeValueContaining("class", "audio-sentence")

    //narration
    if (slide.narrationFile.isEmpty()) {
        if (audios.size >= 1) {
            // find first or concatinate all audio sentenses into one narration audio file
            slide.narrationFile = parseAndConcatenatePageAudio(context, storyAudioPath, storyAudioMap, lang, audios)
        } else {
            // no audio in this page but maybe an image file for next page
            val images = page.getElementsByAttributeValueContaining("class", "bloom-imageContainer")
            var imageFile = parseImageFromElement(slide, frontCoverGraphicProvided, images)
            slide.imageFile = imageFile // save this page's image file in case it is needed for the next page
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
    if (soundtrack.size >= 1) {
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
        if (slideVolume.length == 0) { // if the attribute length is zero, we have an empty string
            slide.volume = 0.25F     // assign a default volume
        } else {
            // convert slideVolume string to a float
            slide.volume = slideVolume.toFloat()
        }
    }

    //image
    val images = page.getElementsByAttributeValueContaining("class", "bloom-imageContainer")
    if (images.size >= 1 || slide.prevPageImageFile.isNotEmpty()) {
        var imageFile = ""
        if (images.size >= 1) {
            imageFile = parseImageFromElement(slide, frontCoverGraphicProvided, images)
        }
        if (imageFile.isEmpty())
            imageFile = slide.prevPageImageFile
        if (imageFile.isNotEmpty()) {
            slide.imageFile = imageFile
            val imagePfdInStream = getStoryFileDescriptor(context, slide.imageFile, "image/*", "r", storyPath.name!!)
            if (imagePfdInStream != null) {
                BitmapFactory.decodeFileDescriptor(imagePfdInStream.fileDescriptor, null, bmOptions)
//                imagePfdInStream.close()
            }
            slide.height = bmOptions.outHeight
            slide.width = bmOptions.outWidth
            // Now keeping default start and end Ken Burns motion settings null for enhanced default behaviour
//        slide.startMotion = Rect(0, 0, slide.width, slide.height)
//        slide.endMotion = Rect(0, 0, slide.width, slide.height)
            if (images.size >= 1) {
                val image = images[0]
                val mSR = reRect.find(image.attr("data-initialrect"))
                if (mSR != null) {
                    val x = mSR.groupValues[1].toDouble() * slide.width
                    val y = mSR.groupValues[2].toDouble() * slide.height
                    val w = mSR.groupValues[3].toDouble() * slide.width
                    val h = mSR.groupValues[4].toDouble() * slide.height
                    slide.startMotion = Rect((x).toInt(), //left
                            (y).toInt(),  //top
                            (x + w).toInt(),   //right
                            (y + h).toInt())  //bottom
                }
                val mER = reRect.find(image.attr("data-finalrect"))
                if (mER != null) {
                    val x = mER.groupValues[1].toDouble() * slide.width
                    val y = mER.groupValues[2].toDouble() * slide.height
                    val w = mER.groupValues[3].toDouble() * slide.width
                    val h = mER.groupValues[4].toDouble() * slide.height
                    slide.endMotion = Rect((x).toInt(), //left
                            (y).toInt(),  //top
                            (x + w).toInt(),   //right
                            (y + h).toInt())  //bottom
                }
            }
        }
    }
    return true
}



fun parseAndConcatenatePageAudio(context: Context, storyAudioPath: DocumentFile, storyAudioMap: MutableMap<String, DocumentFile>, lang: String, audios: Elements): String {
    // Using ffmpeg to concatenate multiple sentences in one page
    // ffmpeg -f concat -safe 0 -i mylist.txt -c copy output.mp3
    // but first we need to copy the source audio files to internal storage
    // so that ffmpeg can access them.
    var narrationFile = ""
    var totalInputAudioFiles = 0
    var firstInputAudioFile = ""
    var outputAudioFileName = ""
    val audioConcatDocs : MutableList<DocumentFile> = ArrayList()

    for (i in 0 until audios.size) {
        if (audios[i].attr("class").contains("ImageDescriptionEdit-style") ||
                audios[i].attr("class").contains("smallCoverCredits"))
            continue

        val audioLang = getAudioAncestorLang(audios[i])
        if (audioLang.isNotEmpty() && lang.isNotEmpty() && audioLang != lang)
            continue

        val searchAudioFile = "${audios[i].id()}.mp3"
        val audioStoryDocFound = storyAudioMap[searchAudioFile] ?: continue
        audioConcatDocs.add(audioStoryDocFound)
        if (totalInputAudioFiles == 0) {
            firstInputAudioFile = "audio/${searchAudioFile}"
            outputAudioFileName = "${audios[i].id()}_output.mp3"
        }
        totalInputAudioFiles++
    }
    if (totalInputAudioFiles > 1) {
        val concatTempFolder = File(context.filesDir, "temp_concat")
        concatTempFolder.deleteRecursively()
        concatTempFolder.mkdirs()
        val concatTempFile = File(concatTempFolder, "temp_audio_concat_list.txt")
        var totalInputFiles = 0
        for (audioStoryDoc in audioConcatDocs) {
            val audioFileOut = File(concatTempFolder, audioStoryDoc.name!!)
            // Copy all the individual files to be concatenated to internal storage
            // This is so that the FFmpeg extension can access all the input and output files
            copyToFilesDir(context, audioStoryDoc.uri, audioFileOut)
            if (audioFileOut.exists() && audioFileOut.length() > 0) {
                // escape any single quotes in file name
                val audioFileStrOut = audioFileOut.path.replace("'", "'\\''")
                // input file is a list of source files to concat starting with "file " and single quoted
                concatTempFile.appendText("file '${audioFileStrOut}'\n")
                totalInputFiles++
            }
        }
        if (totalInputFiles > 0) {
            val ffmpegArgs: MutableList<String> = mutableListOf()
            ffmpegArgs.add("-f")
            ffmpegArgs.add("concat")
            ffmpegArgs.add("-safe")
            ffmpegArgs.add("0")
            ffmpegArgs.add("-i")
            ffmpegArgs.add(concatTempFile.absolutePath)
            ffmpegArgs.add("-c")
            ffmpegArgs.add("copy")
            val concatTempOutputFileStr = "${concatTempFolder.path}/${outputAudioFileName}"
            ffmpegArgs.add(concatTempOutputFileStr)
            FFmpeg.execute(ffmpegArgs.toTypedArray())
            Log.w("ParseBloom.parsePage", FFmpeg.getLastCommandOutput()
                    ?: "No FFMPEG output")

            // search for the output file for all the concatenated files
            val audioStoryConcatDocFind = storyAudioMap[outputAudioFileName]

            // delete the output file if it already exists (prevents numbered versions being created)
            if (audioStoryConcatDocFind != null) {
                storyAudioMap.remove(outputAudioFileName)
                audioStoryConcatDocFind.delete()
            }

            // create the concatination output file
            val audioStoryConcatDoc = storyAudioPath.createFile("", outputAudioFileName)
            if (audioStoryConcatDoc != null) {
                // add the concatenated file to the map of "audio" sub-folder files
                storyAudioMap[outputAudioFileName] = audioStoryConcatDoc
                // now copy the concatenated output file to the Story 'audio' subfolder
                copyFromFilesDir(context, File(concatTempOutputFileStr), audioStoryConcatDoc.uri)
                narrationFile = "audio/${outputAudioFileName}"
            }
        }
        concatTempFolder.deleteRecursively()
    } else if (totalInputAudioFiles == 1) {
        narrationFile = firstInputAudioFile   // select the first valid audio filename
    }

    return narrationFile
}

fun parseImageFromElement(slide: Slide, frontCoverGraphicProvided: Boolean, images: Elements) : String {
    if (images.size == 0)
        return ""
    val image = images[0]
    if (!slide.isFrontCover() || frontCoverGraphicProvided) {
        var imageFile = image.attr("src")
        if (imageFile == "") {
            //bloomd books store the image in a different location
            imageFile = image.attr("style")
            //typical format: background-image:url('1.jpg')
            imageFile = Uri.decode(imageFile.substringAfter("'").substringBeforeLast("'"))
        }
        if (imageFile == "") {
            val src = image.getElementsByAttribute("src")
            if (src.size >= 1)
                imageFile = Uri.decode(src[0].attr("src"))
        }
        return imageFile
    }
    return ""
}

// Search up to grandparent for a language tag
fun getAudioAncestorLang(element: Element): String {
    var e = element
    for (i in 0 until 3) {
        var lang = e.attr("lang")
        if (lang.isNotEmpty())
            return lang
        e = e.parent()
    }
    return ""
}

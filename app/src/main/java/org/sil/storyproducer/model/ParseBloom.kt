package org.sil.storyproducer.model

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.documentfile.provider.DocumentFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.getChildDocuments
import org.sil.storyproducer.tools.file.getStoryFileDescriptor
import org.sil.storyproducer.tools.file.getText
import java.util.*


fun parseBloomHTML(context: Context, storyPath: DocumentFile): Story? {
    //See if there is a BLOOM html file there
    val childDocs = getChildDocuments(context, storyPath.name!!)
    var html_name = ""
    for (f in childDocs) {
        if (f.endsWith(".html") || f.endsWith(".htm")){
            html_name = f
            continue
        }
    }
    if(html_name == "") return null
    val htmlText = getText(context,"${storyPath.name}/$html_name") ?: return null
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

    //Add the Local credits slide
    slide = Slide()
    slide.slideType = SlideType.LOCALCREDITS
    slide.content = context.getString(R.string.LC_prompt)
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
            slide.narrationFile = "audio/${audios[0].id()}.mp3"
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
        slide.volume = (soundtrack[0].attr("data-backgroundaudiovolume") ?: "0.25").toFloat()
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
        slide.startMotion = Rect(0, 0, slide.width, slide.height)
        slide.endMotion = Rect(0, 0, slide.width, slide.height)

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


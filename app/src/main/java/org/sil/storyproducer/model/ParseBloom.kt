package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Rect
import androidx.documentfile.provider.DocumentFile
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.getText
import java.util.*
import java.util.regex.Pattern
import org.sil.storyproducer.tools.file.getChildDocuments
import android.graphics.BitmapFactory
import org.sil.storyproducer.tools.file.getStoryFileDescriptor


fun parseBloomHTML(context: Context, storyPath: androidx.documentfile.provider.DocumentFile): Story? {
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

    //Image and transition pattern
    val reSlideType = Pattern.compile("(^[^\"]+)")
    val reAudioSentence = Pattern.compile("audio-sentence.+id=\"(\\w+)[^>]+>([^<]+)")
    val reParagraph = Pattern.compile("<p>([^<>]+)</p>")
    val reScripture = Pattern.compile("^\\w+\\s+[0-9]")
    val reSoundTrack = Pattern.compile("data-backgroundaudio=\"([^\"]+)")
    val reSoundTrackVolume = Pattern.compile("data-backgroundaudiovolume=\"([^\"]+)")
    val reImage = Pattern.compile("\"(\\w+.(jpg|png))")
    val reSR = Pattern.compile("data-initialrect=\"([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+)")
    val reER = Pattern.compile("data-finalrect=\"([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+)")
    val reOrgAckn = Pattern.compile("originalAcknowledgments.*>([\\w\\W]*?)</div>")
    val reOrgAcknSplit = Pattern.compile(">([^<]*[a-zA-Z0-9]+[^<]*)<")

    val pageTextList = htmlText.split("class=\"bloom-page")
    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true

    if(pageTextList.size <= 2) return null

    //add the title slide
    var slide = Slide()
    var mNarration = reAudioSentence.matcher(pageTextList[0])
    if(mNarration.find()) {
        slide.slideType = SlideType.FRONTCOVER
        slide.narrationFile = "audio/${mNarration.group(1)}.mp3"
        slide.content = mNarration.group(2) ?: ""
        slide.title = slide.content
        val mSubtitle = reParagraph.matcher(pageTextList[0])
        if(mSubtitle.find()){
            slide.reference = mSubtitle.group(1) ?: ""
        }
        slides.add(slide)
    } else { return null }

    for(i in 1 until pageTextList.size){
        //Don't keep the first element, as it is before the first slide.
        slide = Slide()
        val t = pageTextList[i]

        //slide type
        val mSlideVariables = reSlideType.matcher(t)
        if(mSlideVariables.find()){
            val sv = mSlideVariables.group(1)!!.split(" ")
            if("numberedPage" in sv) slide.slideType = SlideType.NUMBEREDPAGE
            //only add the numbered pages.
            else continue
        }

        //narration
        mNarration = reAudioSentence.matcher(t)
        if(mNarration.find()){
            slide.narrationFile = "audio/${mNarration.group(1)}.mp3"
        }

        val mParagraphs = reParagraph.matcher(t)
        while(mParagraphs.find()){
            val text = mParagraphs.group(1) ?: ""
            if(reScripture.matcher(text).find()){
                if(slide.reference == "") slide.reference = text
                else slide.reference += " $text"
            }else{
                if(slide.content == "") slide.content = text
                else slide.content += " $text"
            }
        }
        if(i==1) slide.title = slide.content  //first slide title

        //soundtrack
        val mSoundTrack = reSoundTrack.matcher(t)
        if(mSoundTrack.find()) {slide.musicFile = "audio/${mSoundTrack.group(1)}"}
        val mSoundTrackV = reSoundTrackVolume.matcher(t)
        if(mSoundTrackV.find()) {slide.volume = mSoundTrackV.group(1)!!.toFloat()}

        //image
        val mImage = reImage.matcher(t)
        if(mImage.find()){
            slide.imageFile = mImage.group(1) ?: ""
            BitmapFactory.decodeFileDescriptor(getStoryFileDescriptor(context,slide.imageFile,"image/*","r",storyPath.name!!), null, bmOptions)
            slide.height = bmOptions.outHeight
            slide.width = bmOptions.outWidth
            slide.startMotion = Rect(0, 0, slide.width, slide.height)
            slide.endMotion = Rect(0, 0, slide.width, slide.height)

            val mSR = reSR.matcher(t)
            if(mSR.find()) {
                val x = mSR.group(1)!!.toDouble()*slide.width
                val y = mSR.group(2)!!.toDouble()*slide.height
                val w = mSR.group(3)!!.toDouble()*slide.width
                val h = mSR.group(4)!!.toDouble()*slide.height
                slide.startMotion = Rect((x).toInt(), //left
                        (y).toInt(),  //top
                        (x+w).toInt(),   //right
                        (y+h).toInt())  //bottom
            }
            val mER = reER.matcher(t)
            if(mER.find()) {
                val x = mER.group(1)!!.toDouble()*slide.width
                val y = mER.group(2)!!.toDouble()*slide.height
                val w = mER.group(3)!!.toDouble()*slide.width
                val h = mER.group(4)!!.toDouble()*slide.height
                slide.endMotion = Rect((x).toInt(), //left
                        (y).toInt(),  //top
                        (x+w).toInt(),   //right
                        (y+h).toInt())  //bottom
            }
        }
        slides.add(slide)
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
    val mOrgOckn = reOrgAckn.matcher(pageTextList[0])
    if(mOrgOckn.find()){
        slide = Slide()
        slide.slideType = SlideType.COPYRIGHT
        val mOAParts = reOrgAcknSplit.matcher(mOrgOckn.group(1) ?: "")
        slide.content = ""
        var firstLine = true
        while(mOAParts.find()){
            if(!firstLine) slide.content += "\n"
            firstLine = false
            slide.content += mOAParts.group(1)
        }
        slide.content
        slide.translatedContent = slide.content
        slide.musicFile = MUSIC_NONE
        slides.add(slide)
    }

    return Story(storyPath.name!!,slides)
}
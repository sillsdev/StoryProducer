package org.sil.storyproducer.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.support.v4.provider.DocumentFile
import android.text.Layout
import com.squareup.moshi.*

import java.io.File
import com.squareup.moshi.ToJson
import com.squareup.moshi.FromJson
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import org.sil.storyproducer.tools.media.story.AutoStoryMaker


/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
@JsonClass(generateAdapter = true)
class Slide{
    // template information
    var slideType: SlideType = SlideType.NUMBEREDPAGE
    var narrationFile = ""
    var title = ""
    var subtitle = ""
    var reference = ""
    var content = ""

    var imageFile = ""
    var textFile = ""

    //TODO initialize to no crop and no motion from picture size
    var width: Int = 0
    var height: Int = 0
    var crop: Rect? = null
    var startMotion: Rect? = null
    var endMotion: Rect? = null

    var musicFile = ""
    var volume = 0.0

    //translated text
    var translatedContent: String = ""

    //recorded audio files
    var draftAudioFiles: MutableList<String> = ArrayList()
    var chosenDraftFile = ""
    var communityCheckAudioFiles: MutableList<String> = ArrayList()
    var consultantCheckAudioFiles: MutableList<String> = ArrayList()
    var dramatizationAudioFiles: MutableList<String> = ArrayList()
    var chosenDramatizationFile = ""
    var backTranslationAudioFiles: MutableList<String> = ArrayList()
    var chosenBackTranslationFile = ""

    //consultant approval
    var isChecked: Boolean = false

    fun getOverlayText(dispStory: Boolean = false) : TextOverlay? {
        //There is no text overlay on normal slides or "no slides"
        if(!dispStory){
            if(slideType in arrayOf(SlideType.NUMBEREDPAGE, SlideType.NONE )) return null
        }
        val overlayText = when(slideType){
            SlideType.CREDITS1 -> "$content\n$translatedContent"
            else -> translatedContent
        }
        val tOverlay = TextOverlay(overlayText)
        val fontSize : Int = when(slideType){
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 32
            SlideType.CREDITS1, SlideType.CREDITS2ATTRIBUTIONS -> 24
            SlideType.NUMBEREDPAGE, SlideType.NONE -> 16
        }
        tOverlay.setFontSize(fontSize)
        if(slideType == SlideType.NUMBEREDPAGE)
            tOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
        return tOverlay
    }

    companion object
}

enum class SlideType {
    NONE, FRONTCOVER, NUMBEREDPAGE, CREDITS1, CREDITS2ATTRIBUTIONS, ENDPAGE
}

@JsonClass(generateAdapter = true)
class RectJson(var left: Int = 0,
               var top: Int = 0,
               var right: Int = 0,
               var bottom: Int = 0)

class RectAdapter {
    @FromJson fun rectFromJson(rectJson: RectJson): Rect {
        return Rect(rectJson.left,rectJson.top,rectJson.right,rectJson.bottom)
    }

    @ToJson fun rectToJson(rect: Rect): RectJson {
        return RectJson(rect.left,rect.top,rect.right,rect.bottom)
    }
}

class UriAdapter {
    @FromJson fun fromJson(uriString: String): Uri {
        return Uri.parse(uriString)
    }

    @ToJson fun toJson(uri: Uri): String {
        return uri.toString()
    }
}
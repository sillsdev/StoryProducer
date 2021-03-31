package org.sil.storyproducer.model

import android.graphics.Rect
import android.net.Uri
import android.text.Layout
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.sil.storyproducer.tools.media.imagestory.graphics.TextOverlay
import java.util.*

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
    private val simpleContent: String
    get() {
        //Remove all newlines
        var temp = "[\\r\\n]+".toRegex().replace(content,"")
        //Remove anything that is surrounded by "[]"
        temp = "\\[[^\\]]*\\]?".toRegex().replace(temp,"")
        //remove everything before a : if there is one
        temp =  ".*\\:".toRegex().replace(temp,"")
        //remove everything after a .!? if there is one
        temp =  "[\\.\\!\\?].*".toRegex().replace(temp,"")
        //Make all double spaces one space.
        return "\\s+".toRegex().replace(temp," ")
    }

    var imageFile = ""
    var textFile = ""
    var videoFile = ""
    var audioPosition = 0
    var startTime:Int = 0
    var endTime:Int = 0
    var finalVideoFile = ""

    //TODO initialize to no crop and no motion from picture size
    var width: Int = 0
    var height: Int = 0
    var crop: Rect? = null
    var startMotion: Rect? = null
    var endMotion: Rect? = null

    var musicFile = MUSIC_CONTINUE
    var volume = 0.0f

    //translated text
    var translatedContent: String = ""

    @Json(name="draftAudioFiles")
    var translateReviseAudioFiles: MutableList<String> = ArrayList()
    @Json(name="chosenDraftFile")
    var chosenTranslateReviseFile = ""
    @Json(name="communityCheckAudioFiles")
    var communityWorkAudioFiles: MutableList<String> = ArrayList()
    @Json(name="consultantCheckAudioFiles")
    var accuracyCheckAudioFiles: MutableList<String> = ArrayList()
    @Json(name="dramatizationAudioFiles")
    var voiceStudioAudioFiles: MutableList<String> = ArrayList()
    @Json(name="chosenDramatizationFile")
    var chosenVoiceStudioFile = ""
    @Json(name="backTranslationAudioFiles")
    var backTranslationAudioFiles: MutableList<String> = ArrayList()
    @Json(name="chosenBackTranslationFile")
    var chosenBackTranslationFile = ""

    //consultant approval
    var isChecked: Boolean = false

    fun isFrontCover() = slideType == SlideType.FRONTCOVER
    fun isNumberedPage() = slideType == SlideType.NUMBEREDPAGE
    fun getOverlayText(dispStory: Boolean = false, origTitle: Boolean = false) : TextOverlay? {
        //There is no text overlay on normal slides or "no slides"
        if(!dispStory){
            if(slideType in arrayOf(SlideType.NUMBEREDPAGE, SlideType.NONE )) return null
        }
        val tOverlay = when(slideType) {
            SlideType.FRONTCOVER -> if (origTitle) TextOverlay(simpleContent) else TextOverlay(translatedContent)

            else -> TextOverlay(translatedContent)
        }
        val fontSize : Int = when(slideType){
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 32
            SlideType.COPYRIGHT -> 16
            SlideType.NUMBEREDPAGE, SlideType.LOCALSONG, SlideType.NONE -> 16
            else -> 16
        }
        tOverlay.setFontSize(fontSize)
        if(slideType in arrayOf(SlideType.NUMBEREDPAGE,SlideType.LOCALSONG))
            tOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
        return tOverlay
    }

    fun getFinalFileString(): String {
        return if(chosenVoiceStudioFile == ""){
            chosenTranslateReviseFile.substringAfter("|")
        } else{
            chosenVoiceStudioFile.substringAfter("|")
        }
    }
//
//    fun getFinalFile() : File {
//        return File(getFinalFileString());
//    }

    companion object
}

enum class SlideType {
    // LOCALCREDITS is obsolete but provided for reading projects from v3.0.2 or earlier
    NONE, FRONTCOVER, NUMBEREDPAGE, LOCALSONG, LOCALCREDITS, COPYRIGHT, ENDPAGE
}

const val MUSIC_NONE = "noSoundtrack"
const val MUSIC_CONTINUE = "continueSoundtrack"

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

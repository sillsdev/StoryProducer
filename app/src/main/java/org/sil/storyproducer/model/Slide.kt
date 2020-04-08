package org.sil.storyproducer.model

import android.graphics.Rect
import android.net.Uri
import android.text.Layout
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import java.text.SimpleDateFormat
import java.util.*

enum class UploadState {
    UPLOADING,
    NOT_UPLOADED,
    UPLOADED
}

class Slide{
    var isApproved: Boolean = false

    // template information
    var slideType: SlideType = SlideType.NUMBEREDPAGE
    var narration: Recording? = null
    var title = ""
    var subtitle = ""
    var reference = ""
    var content = ""
    val simpleContent: String
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

    //recorded audio files
    var draftRecordings = RecordingList()
    var communityCheckRecordings = RecordingList()
    var consultantCheckRecordings = RecordingList()
    var dramatizationRecordings = RecordingList()
    var backTranslationRecordings = RecordingList()

    var backTranslationUploadState = UploadState.NOT_UPLOADED
    var backTranslationTranscript: String? = null
    var backTranslationTranscriptIsDirty = false

    //consultant approval
    var isChecked: Boolean = false

    fun getOverlayText(dispStory: Boolean = false, origTitle: Boolean = false) : TextOverlay? {
        //There is no text overlay on normal slides or "no slides"
        if(!dispStory){
            if(slideType in arrayOf(SlideType.NUMBEREDPAGE, SlideType.NONE )) return null
        }
        val tOverlay = when(slideType) {
            SlideType.FRONTCOVER -> if (origTitle) TextOverlay(simpleContent) else TextOverlay(translatedContent)
            SlideType.LOCALCREDITS -> TextOverlay("$translatedContent\n" +
                    "This video is licensed under a Creative Commons Attribution" +
                    "-NonCommercial-ShareAlike 4.0 International License " +
                    "© ${SimpleDateFormat("yyyy", Locale.US).format(GregorianCalendar().time)}")
            else -> TextOverlay(translatedContent)
        }
        val fontSize : Int = when(slideType){
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 32
            SlideType.LOCALCREDITS, SlideType.COPYRIGHT -> 16
            SlideType.NUMBEREDPAGE, SlideType.LOCALSONG, SlideType.NONE -> 16
        }
        tOverlay.setFontSize(fontSize)
        if(slideType in arrayOf(SlideType.NUMBEREDPAGE,SlideType.LOCALSONG))
            tOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
        return tOverlay
    }

    companion object
}

enum class SlideType {
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

package org.tyndalebt.spadv.model

import android.graphics.Rect
import android.net.Uri
import android.text.Layout
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import org.tyndalebt.spadv.tools.media.graphics.TextOverlay
import java.text.SimpleDateFormat
import java.util.*

enum class UploadState {
    UPLOADING,
    NOT_UPLOADED,
    UPLOADED
}
/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
@JsonClass(generateAdapter = true)
class Slide{
    // SP422 - DKH 5/6/2022 Enable images on all the slides to be swapped out via the camera tool
    // This extension is used on a local image from the camera tool
    val localSlideExtension = "_Local.png"
    // template information
    var isApproved: Boolean = false

    var slideType: SlideType = SlideType.NUMBEREDPAGE
    var narration: Recording? = null
    var narrationFile = ""
    var title = ""
    var subtitle = ""
    var reference = ""
    var content = ""
    var imageFile = ""
    var textFile = ""
    val simpleContent: String = ""

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

    var draftRecordings = RecordingList()
    var backTranslationRecordings = RecordingList()

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
    var backTranslationUploadState = UploadState.NOT_UPLOADED
    var backTranslationTranscript: String? = null
    var backTranslationTranscriptIsDirty = false

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
            SlideType.LOCALCREDITS -> TextOverlay("$translatedContent\n" +
                    "This video is licensed under a Creative Commons Attribution" +
                    "-NonCommercial-ShareAlike 4.0 International License " +
                    "© ${SimpleDateFormat("yyyy", Locale.US).format(GregorianCalendar().time)}")
            else -> TextOverlay(translatedContent)
        }

        val fontSize : Int = when(slideType){
            // 8/3/22 was 32 and were told it needed reduced several for FrontCover
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 24
            SlideType.LOCALCREDITS, SlideType.COPYRIGHT -> 16
            SlideType.NUMBEREDPAGE, SlideType.LOCALSONG, SlideType.NONE -> 16
        }

        val outlineShow : Boolean = when(slideType){
            SlideType.FRONTCOVER -> true
            else -> false
        }

        return tOverlay
    }

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

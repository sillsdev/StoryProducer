package org.sil.storyproducer.model

import android.graphics.Rect
import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
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

    fun isFrontCover() = slideType == SlideType.FRONTCOVER
    fun isNumberedPage() = slideType == SlideType.NUMBEREDPAGE

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
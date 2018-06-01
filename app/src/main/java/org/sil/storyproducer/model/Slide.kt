package org.sil.storyproducer.model

import android.graphics.Rect
import com.squareup.moshi.*

import java.io.File
import com.squareup.moshi.ToJson
import com.squareup.moshi.FromJson



/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
@JsonClass(generateAdapter = true)
class Slide{
    // template information
    var narrationPath: File = File("")

    var imagePath: File = File("")
    //TODO initialize to no crop and no motion from picture size
    var width: Int = 0
    var height: Int = 0
    var crop: Rect? = null
    var startMotion: Rect? = null
    var endMotion: Rect? = null

    var musicPath: File = File("")
    var volume = 0

    //draft
    var draftAudioFiles: MutableList<File> = ArrayList()
    var chosenDraftFile: File = File("")
    var draftText: String = ""

    //
    companion object
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

class FileAdapter {
    @FromJson fun fromJson(path: String): File? {return File(path)}
    @ToJson fun toJson(path: File): String {return path.toString()}
}
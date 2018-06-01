package org.sil.storyproducer.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    var title = ""
    var subtitle = ""
    var reference = ""
    var content = ""

    var imagePath : File = File("")
    var textPath : File = File("")
    set(value){
        field = value
        if(value.isFile) {
            val narration = value.readText()
            val narrations = narration.split("~")
            if (narrations.size > 0) this.title = narrations[0]
            if (narrations.size > 1) this.subtitle= narrations[1]
            if (narrations.size > 2) this.reference = narrations[2]
            if (narrations.size > 3) this.content = narrations[3]
        }
    }

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

    fun getImage(sampleSize: Int = 1): Bitmap? {
        if(imagePath.exists()){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeFile(imagePath.toString(), options)
        }
        return null
    }

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
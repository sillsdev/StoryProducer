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
    internal var path = File("")

    // template information
    var narrationFile = ""
    var title = ""
    var subtitle = ""
    var reference = ""
    var content = ""

    var imageFile = ""
    var textFile = ""
    set(value){
        field = value
        val textPath = File(path,value)
        if(textPath.isFile) {
            val text = textPath.readText()
            val textList = text.split("~")
            if (textList.size > 0) this.title = textList[0]
            if (textList.size > 1) this.subtitle= textList[1]
            if (textList.size > 2) this.reference = textList[2]
            if (textList.size > 3) this.content = textList[3]
        }
    }

    //TODO initialize to no crop and no motion from picture size
    var width: Int = 0
    var height: Int = 0
    var crop: Rect? = null
    var startMotion: Rect? = null
    var endMotion: Rect? = null

    var musicFile = ""
    var volume = 0

    //draft
    var draftAudioFiles: MutableList<String> = ArrayList()
    var chosenDraftFile = ""
    var draftText: String = ""

    fun getImage(sampleSize: Int = 1): Bitmap? {
        if(File(path,imageFile).exists()){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeFile(File(path,imageFile).toString(), options)
        }
        return null
    }

    fun configurePath(path: File) {
        this.path = path
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
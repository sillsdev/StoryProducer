package org.sil.storyproducer.model

import android.graphics.Rect

import com.squareup.moshi.JsonClass

import java.io.File

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

}

package org.sil.storyproducer.model

import android.graphics.Rect
import java.io.File

/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
class Slide{
    var narrationPath: File = File("")

    var imagePath: File = File("")
    var width: Int = 0
    var height: Int = 0
    var crop: Rect? = null
    var startMotion: Rect? = null
    var endMotion: Rect? = null

    var musicPath: File = File("")
    var volume = 0
}

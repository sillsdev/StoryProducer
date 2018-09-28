package org.sil.storyproducer.tools.media.graphics


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import org.sil.storyproducer.tools.file.getStoryChildOutputStream
import org.sil.storyproducer.tools.file.getStoryImage

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Throws(IOException::class)
fun overlayJPEG(context: Context, relPath: String, outRelPath: String, overlay: TextOverlay) {

    val source = getStoryImage(context, relPath)
    val dest = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(dest)

    overlay.draw(canvas)

    val out = getStoryChildOutputStream(context,outRelPath)
    dest.compress(Bitmap.CompressFormat.JPEG, 95, out)
    out!!.flush()
    out.close()
}

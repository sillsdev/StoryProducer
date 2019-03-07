package org.sil.storyproducer.tools

import android.graphics.Bitmap

object BitmapScaler {
    // Scale and maintain aspect ratio given a desired width
    // BitmapScaler.scaleToFitWidth(bitmap, 100);
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }


    // Scale and maintain aspect ratio given a desired height
    // BitmapScaler.scaleToFitHeight(bitmap, 100);
    fun scaleToFitHeight(bitmap: Bitmap, height: Int): Bitmap {
        val factor = height / bitmap.height.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), height, true)
    }

}

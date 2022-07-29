package org.tyndalebt.spadv.tools

import android.graphics.Bitmap
import android.graphics.RectF

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

    fun centerCrop(bitmap: Bitmap, h: Int, w: Int) : Bitmap{
        val dstBtm : Bitmap
        return if (bitmap.width*1f/bitmap.height > w*1f/h){
            //more width - scale to height and cut the width
            dstBtm = scaleToFitHeight(bitmap,h)
            Bitmap.createBitmap(dstBtm,(dstBtm.width - w)/2, 0,w,h)
        }else{
            //more height - scale to width and cut the height
            dstBtm = scaleToFitWidth(bitmap,w)
            Bitmap.createBitmap(dstBtm,0, (dstBtm.height - h)/2,w,h)
        }
    }

    fun centerCropRectF(bh: Int, bw: Int, h:Int, w:Int) : RectF {
        return if (bw*1f/bh > w*1f/h){
            //more width - scale to height and cut the width
            val wCrop = (bw*h/bh*1f - w)/2f
            RectF(-wCrop, 0f, w+wCrop, h*1f)
        }else{
            //more height - scale to width and cut the height
            val hCrop = (bh*w/bw*1f - h)/2f
            RectF(0f, -hCrop, w*1f, h+hCrop)
        }
    }

}

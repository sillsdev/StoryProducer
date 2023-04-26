package org.sil.storyproducer.service

import android.content.Context
import android.graphics.*
import androidx.preference.PreferenceManager
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.tools.file.getStoryChildInputStream

class SlideService(val context: Context) {

    val mContext = context

    fun getImage(slideNum: Int, sampleSize: Int, story: Story): Bitmap {
        if (shouldShowDefaultImage(slideNum, story)) {
            return genDefaultImage()
        } else {
            return getImage(story.slides[slideNum].imageFile, sampleSize, false, story)
        }
    }

    fun shouldShowDefaultImage(slideNum: Int, story: Story): Boolean {
        return story.title.isNullOrEmpty()
                || story.slides.getOrNull(slideNum)?.imageFile.isNullOrEmpty()
    }

    fun getImage(relPath: String, sampleSize: Int = 1, useAllPixels: Boolean = false, story: Story): Bitmap {
        // DKH - Updated 03/13/2021 to fix Issue 548: In Android 11 Story Producer crashes in Finalize
        //                         phase and no video is produced
        // This routine is called for every slide in a story during FINALIZE.  "relPath" is the name of
        // the image file for the slide (e.g., "1.jpg") but some slides images are optional such as the title
        // slide and the song slide.  "relPath" for a slide without an image is passed as "" (empty string).
        // Which  means open the root directory in getStoryChildInputStream
        // Before the fix, iStream.available was called on an empty string file.  Previous to Android 11,
        // iStream.available() return a zero when called on an empty string file.
        // For Android 11, iStream.available() on an empty string file throws an exception which
        // was not caught by this routine.
        // For issue 548, check for an empty string and return a default image
        // According to the documentation,
        // iStream.available() can throw an exception, so a try/catch was added.
        // restructure routine for better flow
        if(relPath != "") {
            val iStream = getStoryChildInputStream(context, relPath, story.title)

            try {
                if (iStream !== null && iStream.available() != 0) {
                    // The stream is valid and there is data, so do the processing
                    val options = BitmapFactory.Options()
                    options.inSampleSize = sampleSize
                    if (useAllPixels) {
                        options.inTargetDensity = 1
                    }

                    val bmp = BitmapFactory.decodeStream(iStream, null, options)!!
                    if (useAllPixels) {
                        bmp.density = Bitmap.DENSITY_NONE
                    }

                    return bmp
                }
            } catch (e: Exception) {
                // can be throw by iSteam.available
                // return default image on exception
            }
        }
        return genDefaultImage()
    }

    fun genDefaultImage(): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.greybackground)
    }

    fun scaleImage(originalBitmap: Bitmap, width: Int, height: Int, useWidescreenSetting: Boolean): Bitmap {

        // Load the original bitmap from a file or resource
        // Define the desired aspect ratio of the output bitmap
        // Calculate the new width and height of the output bitmap
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val originalAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val newWidth: Int
        val newHeight: Int
        var imageScale = 1.0f
        val desiredAspectRatio = getVideoScreenRatio(useWidescreenSetting)
        if (originalAspectRatio > desiredAspectRatio) {
            // The original bitmap is wider than the desired aspect ratio, so we need to pad it vertically
            imageScale = width.toFloat() / originalWidth.toFloat()
            newWidth = (originalWidth * imageScale).toInt()
            newHeight = ((originalWidth.toFloat() / desiredAspectRatio).toInt() * imageScale).toInt()
        } else {
            // The original bitmap is taller than the desired aspect ratio, so we need to pad it horizontally
            imageScale = height.toFloat() / originalHeight.toFloat()
            newWidth = ((originalHeight.toFloat() * desiredAspectRatio).toInt() * imageScale).toInt()
            newHeight = (originalHeight * imageScale).toInt()
        }

        // Create a new bitmap with the desired aspect ratio and a transparent background
        val newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        // Draw the original bitmap onto the new bitmap using a canvas
        val canvas = Canvas(newBitmap)

        val paint = Paint()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context);
        val altBgImageColor = prefs.getString("bloom_bgimage_color", "")?.trim();
        if (altBgImageColor != null && altBgImageColor.isNotEmpty()) {
            try {
                paint.color = Color.parseColor(altBgImageColor)
            } catch (e: IllegalArgumentException) {
                paint.color = Color.LTGRAY
            }
        }
        else
            paint.color = Color.LTGRAY
        paint.style = Paint.Style.FILL

        canvas.drawRect(0f, 0f, newWidth.toFloat(), newHeight.toFloat(), paint)

        val left = (newWidth - (originalWidth*imageScale)) / 2
        val right = (newWidth + (originalWidth*imageScale)) / 2
        val top = (newHeight - (originalHeight*imageScale)) / 2
        val bottom = (newHeight + (originalHeight*imageScale)) / 2
        canvas.drawBitmap(originalBitmap, null, Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()), null)

        return newBitmap
    }

    fun shouldScaleForAspectRatio(width: Int, height: Int, desiredAspectRatio: Float): Boolean {

        val actualAspectRatio = width.toFloat() / height.toFloat()
        val percentDiff = if (actualAspectRatio / desiredAspectRatio >= 1.0f)
            ((actualAspectRatio / desiredAspectRatio) - 1) * 100
        else
            ((desiredAspectRatio / actualAspectRatio) - 1) * 100
        return percentDiff > maxAspectRatioError

    }

    fun isVideoWideScreen(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean("video_wide", false);
    }

    fun getVideoScreenRect(isMp4Video: Boolean, useWidescreenSetting: Boolean): Rect {
        if (useWidescreenSetting && isVideoWideScreen()) {
            if (isMp4Video)
                return Rect(0, 0, 1280, 720)
            else
                return Rect(0, 0, 176, 144) // 3gp widescreen not supported
        } else {
            if (isMp4Video)
                return Rect(0, 0, 768, 576)
            else
                return Rect(0, 0, 176, 144)
        }
    }

    fun getVideoScreenRatio(useWidescreenSetting: Boolean): Float {
        val vidoRect = getVideoScreenRect(true, useWidescreenSetting)
        return vidoRect.width().toFloat() / vidoRect.height().toFloat()
    }

    companion object {
        const val maxAspectRatioError = 4.0f    // max 4% error
    }

}

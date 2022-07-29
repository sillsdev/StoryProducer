package org.tyndalebt.spadv.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.tools.file.getStoryChildInputStream

class SlideService(val context: Context) {

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

}

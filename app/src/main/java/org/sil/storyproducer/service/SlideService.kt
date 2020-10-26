package org.sil.storyproducer.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.sil.storyproducer.film.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.tools.file.getStoryChildInputStream

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
        val iStream = getStoryChildInputStream(context, relPath, story.title)
        if (iStream === null || iStream.available() == 0) {
            return genDefaultImage()
        }

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

    fun genDefaultImage(): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.greybackground)
    }

}
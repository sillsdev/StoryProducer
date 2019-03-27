package org.sil.storyproducer.controller.remote

import org.sil.storyproducer.controller.WholeStoryActivity
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.storyRelPathExists

//TODO Add background audio functionality
//TODO Check if all slides have audio associated with them
class WholeStoryBackTranslationActivity : WholeStoryActivity() {
    override fun getToolbarBooleanArray(): BooleanArray {
        return booleanArrayOf(true, false, false, true)
    }

    override fun getAudioFileFromSlide(slide: Slide) : String{
        return slide.chosenDraftFile
    }

    override fun isWatchedOnce(): Boolean {
        return storyRelPathExists(this,Workspace.activeStory.wholeStoryBackTAudioFile)
    }
}

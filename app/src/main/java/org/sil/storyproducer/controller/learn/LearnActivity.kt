package org.sil.storyproducer.controller.learn

import org.sil.storyproducer.controller.WholeStoryActivity
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.storyRelPathExists

class LearnActivity : WholeStoryActivity() {
    override fun getToolbarBooleanArray(): BooleanArray {
        return booleanArrayOf(true, false, false, false)
    }

    override fun getAudioFileFromSlide(slide: Slide) : String{
        return slide.narrationFile
    }

    override fun isWatchedOnce(): Boolean {
        return storyRelPathExists(this,Workspace.activeStory.learnAudioFile)
    }
}

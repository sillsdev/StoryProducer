package org.sil.storyproducer.tools.media.story

import android.widget.Toast
import org.sil.storyproducer.controller.export.CreateActivity
import org.sil.storyproducer.tools.media.Producer

class SlideProducer(var storyMaker: AutoStoryMaker, override var parent:CreateActivity): Producer{
    override var isActive: Boolean = false
    override var title:String = ""
    override var progressUpdater: Runnable = Runnable {
        var isDone = false
        var isSuccess = false
        while (!isDone) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                //If progress updater is interrupted, just stop.
                return@Runnable
            }

            var progress = 0.0
            synchronized(CreateActivity.storyMakerLock) {
                progress = storyMaker.progress
                isDone = storyMaker.isDone
            }
            updateProgress((progress * CreateActivity.PROGRESS_MAX).toInt())
        }
        isSuccess = storyMaker.isSuccess

        parent.runOnUiThread {
            stopExport()
            if(isSuccess)
                Toast.makeText(parent.baseContext, "Video created!", Toast.LENGTH_LONG).show()
            else
                Toast.makeText(parent.baseContext, "Error!", Toast.LENGTH_LONG).show()
        }
    }
    private fun stopExport() {
        synchronized(CreateActivity.storyMakerLock) {
            storyMaker.close()
        }
        //update the list view
        parent.toggleVisibleElements()
    }

    override fun start() {
        isActive = true
        storyMaker.start()
        isActive = false
    }
}
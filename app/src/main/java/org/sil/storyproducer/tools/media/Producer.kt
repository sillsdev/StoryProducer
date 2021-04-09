package org.sil.storyproducer.tools.media

import org.sil.storyproducer.controller.export.FinalizeActivity

interface Producer {
    var progressUpdater:Runnable
    var parent:FinalizeActivity
    var isActive:Boolean
    var title:String
    fun updateProgress(progress: Int) {
        parent.runOnUiThread { parent.mProgressBar.progress = progress }
    }
    fun start()
}
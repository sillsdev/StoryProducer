package org.sil.storyproducer.view

import android.content.Intent
import android.net.Uri
import org.sil.storyproducer.controller.BaseController

interface BaseActivityView {

    fun startActivityForResult(intent: Intent, request: Int)
    fun showMain()
    fun showRegistration()
    fun takePersistableUriPermission(uri: Uri)
    fun showReadingTemplatesDialog(controller: BaseController)
    fun updateReadingTemplatesDialog(current: Int, total: Int, currentTemplate: String)
    fun hideReadingTemplatesDialog()
    fun showCancellingReadingTemplatesDialog()

}
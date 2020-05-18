package org.sil.storyproducer.view

import android.content.Intent
import android.net.Uri

interface BaseActivityView {

    fun startActivityForResult(intent: Intent, request: Int)
    fun showMain()
    fun showRegistration()
    fun takePersistableUriPermission(uri: Uri)
    fun showReadingTemplatesDialog(total: Int)
    fun updateReadingTemplatesDialog(complete: Int, total: Int)
    fun hideReadingTemplatesDialog()

}
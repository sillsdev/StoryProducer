package org.sil.storyproducer.view

import android.content.Intent
import android.net.Uri

interface BaseActivityView {

    fun startActivityForResult(intent: Intent, request: Int)
    fun showMain()
    fun showRegistration()
    fun takePersistableUriPermission(uri: Uri)
    fun showReadingTemplatesDialog()
    fun hideReadingTemplatesDialog()

}
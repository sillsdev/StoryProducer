package org.sil.storyproducer.view

import android.content.Intent
import android.net.Uri

interface BaseActivityView {

    fun startActivityForResult(intent: Intent, request: Int)
    fun showRegistration()
    fun takePersistableUriPermission(uri: Uri)

}
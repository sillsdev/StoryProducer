package org.tyndalebt.spadv.view

import android.content.Intent
import android.net.Uri
import org.tyndalebt.spadv.controller.BaseController

interface BaseActivityView {

    fun startActivityForResult(intent: Intent, request: Int)
    fun showMain()
    // DKH - 05/12/2021
    // Issue #573: SP will hang/crash when submitting registration
    // Updated showRegistration interface to allow calling function in the current activity
    // to specify whether the  finish() call should be executed.  The finish call terminates
    // the current activity.  Value true means call finish(). Calling showRegistration with
    // no argument sets executeFinishActivity to true
    fun showRegistration(executeFinishActivity: Boolean = true)
    fun takePersistableUriPermission(uri: Uri)
    fun showReadingTemplatesDialog(controller: BaseController)
    fun updateReadingTemplatesDialog(current: Int, total: Int, currentTemplate: String)
    fun hideReadingTemplatesDialog()
    fun showCancellingReadingTemplatesDialog()

}
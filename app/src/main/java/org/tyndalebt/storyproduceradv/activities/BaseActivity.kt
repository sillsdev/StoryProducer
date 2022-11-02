package org.tyndalebt.storyproduceradv.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import io.reactivex.disposables.CompositeDisposable
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.BaseController
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.controller.RegistrationActivity
import org.tyndalebt.storyproduceradv.controller.SelectTemplatesFolderController
import org.tyndalebt.storyproduceradv.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_REQUEST_CODES
import org.tyndalebt.storyproduceradv.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.tyndalebt.storyproduceradv.controller.remote.RemoteCheckFrag
import org.tyndalebt.storyproduceradv.controller.remote.getPhoneId
import org.tyndalebt.storyproduceradv.controller.wordlink.WordLinksListActivity
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.Network.VolleySingleton
import org.tyndalebt.storyproduceradv.view.BaseActivityView
import timber.log.Timber

open class BaseActivity : AppCompatActivity(), BaseActivityView {

    lateinit var controller: SelectTemplatesFolderController

    private var readingTemplatesDialog: AlertDialog? = null
    private var cancellingReadingTemplatesDialog: AlertDialog? = null
    protected val subscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        Timber.tag(javaClass.simpleName).v("onCreate")
        controller = SelectTemplatesFolderController(this, this, Workspace)
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(javaClass.simpleName).v("onResume")
        val prefs = getSharedPreferences(RemoteCheckFrag.R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("FirebaseChanged", false))
        {
            updateFBToken(this)
        }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        if (SELECT_TEMPLATES_FOLDER_REQUEST_CODES.contains(request)) {
            controller.onFolderSelected(request, result, data)
        }
    }

    fun initWorkspace() {
        Workspace.initializeWorkspace(this)

        if (Workspace.workdocfile.isDirectory) {
            controller.updateStories()
        } else {
            showWelcomeDialog()
        }
    }

    private fun showWelcomeDialog() {
        startActivity(Intent(this, WelcomeDialogActivity::class.java))
        finish()
    }

    fun updateTemplatesFolder() {
        controller.openDocumentTree(UPDATE_TEMPLATES_FOLDER)
    }

    override fun takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    override fun showMain() {
        startActivity(Intent(this, MainActivity::class.java))
        Workspace.startedMain = true
        Workspace.processReceivedApprovals()
        finish()
    }
    // DKH - 05/12/2021
    // Issue #573: SP will hang/crash when submitting registration
    //
    // showRegistration Argument allows the caller in the current Activity to finish or
    // not finish the current Activity before starting the RegistrationActivity.
    // The MainActivity should set executeFinishActivity to false so that when the registration
    // is complete, there is a Story Producer activity that will control execution.
    // After the RegistrationActivity completes, control is returned to the MainActivity
    // where the list of story templates are displayed
    //
    // 06/14/2021 - DKH, Issue 407, Pull Request 561 - Merge into Latest sillsdev
    // Updated selectItem in DrawerItemclickListener to set Workspace.showRegistration
    // to true and then call showMain() instead of calling showRegistration.  This is equivalent
    // to calling showRegistration.  See selectItem for more detail.
    //
    // All showRegistration calls should be done through the MainActivity to
    // avoid hanging Story Producer.
    override fun showRegistration(executeFinishActivity: Boolean) {
        startActivity(Intent(this, RegistrationActivity::class.java))

        // If true, then this Activity will finish and exit
        if(executeFinishActivity) {
            finish()
        }
    }

    fun showWordLinksList() {
        startActivity(Intent(this, WordLinksListActivity::class.java))
        finish()
    }

    override fun showReadingTemplatesDialog(controller: BaseController) {
        readingTemplatesDialog = AlertDialog.Builder(this)
                .setTitle(R.string.scanning_sp_templates)
                .setMessage("")
                .setNegativeButton(R.string.cancel) { d, i -> controller.cancelUpdate() }
                .setCancelable(false)
                .create()

        readingTemplatesDialog?.show()
    }

    override fun showCancellingReadingTemplatesDialog() {
        cancellingReadingTemplatesDialog = AlertDialog.Builder(this)
                .setTitle(R.string.scanning_sp_templates)
                .setMessage(R.string.cancelling)
                .create()

        cancellingReadingTemplatesDialog?.show()
    }

    override fun updateReadingTemplatesDialog(current: Int, total: Int, currentTemplate: String) {
        readingTemplatesDialog?.setMessage("$current of $total templates\n\n$currentTemplate")
    }

    override fun hideReadingTemplatesDialog() {
        readingTemplatesDialog?.dismiss()
        readingTemplatesDialog = null
        cancellingReadingTemplatesDialog?.dismiss()
        cancellingReadingTemplatesDialog = null
    }

    fun showSelectTemplatesFolderDialog() {
/*
        AlertDialog.Builder(this)
                .setTitle(buildSelectTemplatesTitle())
                .setMessage(buildSelectTemplatesMessage())
                .setPositiveButton(R.string.ok) { _, _ -> updateTemplatesFolder() }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()

 */
        updateTemplatesFolder()
    }

    private fun buildSelectTemplatesTitle(): Spanned {
        val title = "<b>${getString(R.string.update_workspace)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildSelectTemplatesMessage(): Spanned {
        val message = getString(R.string.workspace_selection_help)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }

    fun showAboutDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_title))
                .setView(buildAboutDialogView())
                .setPositiveButton(getString(R.string.ok), null)
                .create()
                .show()
    }

    private fun buildAboutDialogView(): View {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        return layoutInflater.inflate(R.layout.dialog_about, null).apply {
            findViewById<TextView>(R.id.appVersion)
                    .setText(getString(R.string.app_version, versionName))
        }
    }

    private fun updateFBToken(context: Context) {
        if (!Workspace.registration.complete)
            return  // Registration will send token for the first time
        var js: MutableMap<String, String> = hashMapOf()
        var resp: String? = null
        var testErr = ""
        val myContext = context.applicationContext
        val reg = Workspace.registration

        val prefs = getSharedPreferences(RemoteCheckFrag.R_CONSULTANT_PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString("FirebaseToken", " ")
        val prefsEditor = prefs.edit()
        prefsEditor.putBoolean("FirebaseChanged", false)  // hasn't changed since last time sent to server'
        prefsEditor.apply()

        val phoneId = getPhoneId(myContext)

        js["Key"] = getString(R.string.api_token)
        js["PhoneId"] = phoneId
        js["FirebaseToken"] = token!!

        Log.i("LOG_VOLLEY", js.toString())
        val registerPhoneUrl = Workspace.getRoccUrlPrefix(context) + getString(R.string.url_update_fb_token)
        val req = object : StringRequest(Request.Method.POST, registerPhoneUrl, Response.Listener { response ->
            Log.i("LOG_VOLEY", response)
            resp = response
        }, Response.ErrorListener { error ->
            Log.e("LOG_VOLLEY", error.toString())
            Log.e("LOG_VOLLEY", "HIT ERROR")
            testErr = error.toString()
        }) {
            override fun getParams(): Map<String, String> {
                return js
            }
        }

        val test = VolleySingleton.getInstance(myContext).requestQueue
        test.add(req)
    }
}


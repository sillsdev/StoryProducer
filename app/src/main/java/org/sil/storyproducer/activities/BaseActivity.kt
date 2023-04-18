package org.sil.storyproducer.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.*
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_REQUEST_CODES
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.wordlink.WordLinksListActivity
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.view.BaseActivityView
import org.sil.storyproducer.controller.bldownload.BLDownloadActivity
import org.sil.storyproducer.tools.file.isUriStorageMounted
import timber.log.Timber

open class BaseActivity : AppCompatActivity(), BaseActivityView {

    lateinit var controller: SelectTemplatesFolderController

    private var readingTemplatesDialog: AlertDialog? = null
    private var cancellingReadingTemplatesDialog: AlertDialog? = null
    protected val subscriptions = CompositeDisposable()

    companion object {
        const val BLOOM_DL_TEMPLATES_ACTIVITY = 0
        const val BLOOM_DL_FEATURED_ACTIVITY = 1
        const val BLOOM_DL_ACTIVITY_INDEX = "BLDL_Activity_Index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        Timber.tag(javaClass.simpleName).v("onCreate")
        controller = SelectTemplatesFolderController(this, this, Workspace)
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(javaClass.simpleName).v("onResume")
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        // This is a generic handler for all template folder picking request results
        // The result can be checked for RESULT_CANCELED in a derived class for specific actions
        if (result == RESULT_OK) {
            if (SELECT_TEMPLATES_FOLDER_REQUEST_CODES.contains(request)) {
                controller.onFolderSelected(request, result, data)
            }
        }
    }

    fun initWorkspace() {
        Workspace.initializeWorkspace(this)
        var showWelcome = true  // set to false if no need to show welcome wizard
        do {
            if (Workspace.workdocfile.isDirectory) {    // i.e. is the workspace still accessable
                controller.updateStories()  // We already have a workspace folder
                showWelcome = false
                break
            }
            if (!isUriStorageMounted(Workspace.workdocfile.uri)) {
                // TODO: Replace 'media not mounted' toast message with a better Startup Wizard
                Toast.makeText(this, R.string.workspace_not_mounted, Toast.LENGTH_LONG).show()
                showWelcome = true
                break
            }
            val workspaceCreatedMsg = Workspace.createAppSpecificWorkspace()
            if (workspaceCreatedMsg.isNotEmpty()) {
                controller.updateStories()  // Update after a new app-specific workspace was created
                Toast.makeText(this, workspaceCreatedMsg, Toast.LENGTH_LONG).show()
                showWelcome = false
                break
            }
        } while (false)

        if (showWelcome) {
            showWelcomeDialog()
        }

    }

    private fun showWelcomeDialog() {
        startActivity(Intent(this, WelcomeDialogActivity::class.java))
//        finish()  // removed to keep back button working on MainActivity
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
//        finish()  // removed to keep back button working on MainActivity
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
        AlertDialog.Builder(this)
                .setTitle(buildSelectTemplatesTitle())
                .setMessage(buildSelectTemplatesMessage())
                .setPositiveButton(R.string.next) { _, _ -> updateTemplatesFolder() }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    private fun buildSelectTemplatesTitle(): Spanned {
        val title = "<b>${getString(R.string.select_workspace_folder)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildSelectTemplatesMessage(): Spanned {
        val message = getString(R.string.select_workspace_help_msg)
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

    fun showDevSettings() {
        startActivity(Intent(this,  SettingsActivity::class.java))
    }

    private fun buildAboutDialogView(): View {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        return layoutInflater.inflate(R.layout.dialog_about, null).apply {
            findViewById<TextView>(R.id.appVersion)
                    .setText(getString(R.string.app_version, versionName))
        }
    }

    fun showBLDownloadDialog(downloadIndex: Int) {
        val bldlIntent = Intent(this, BLDownloadActivity::class.java)
        bldlIntent.putExtra(BLOOM_DL_ACTIVITY_INDEX, downloadIndex)
        startActivity(bldlIntent)
    }


}

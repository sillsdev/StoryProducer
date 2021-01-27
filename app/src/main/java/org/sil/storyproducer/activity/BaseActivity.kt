package org.sil.storyproducer.activity

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
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.BaseController
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.SelectTemplatesFolderController
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_REQUEST_CODES
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.view.BaseActivityView
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
            showSelectTemplatesFolder()
        }
    }

    private fun showSelectTemplatesFolder() {
        startActivity(Intent(this, WorkspaceDialogUpdateActivity::class.java))
        finish()
    }

    fun selectTemplatesFolder() {
        controller.openDocumentTree(SELECT_TEMPLATES_FOLDER)
    }

    fun selectTemplatesFolderAndAddDemo() {
        controller.openDocumentTree(SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO)
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

    override fun showRegistration() {
        startActivity(Intent(this, RegistrationActivity::class.java))
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
        AlertDialog.Builder(this)
                .setTitle(buildSelectTemplatesTitle())
                .setMessage(buildSelectTemplatesMessage())
                .setPositiveButton(R.string.ok) { _, _ -> updateTemplatesFolder() }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
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

}
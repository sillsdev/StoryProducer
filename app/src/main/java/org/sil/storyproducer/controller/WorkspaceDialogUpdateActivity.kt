package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.appcompat.app.AppCompatActivity
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace

class WorkspaceDialogUpdateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Workspace.initializeWorskpace(this)
        showWelcomeDialog()
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildTitle())
                .setMessage(buildMessage())
                .setPositiveButton(R.string.use_internal_demo) { _, _ -> showCreateAndSelectFolderDialog() }
                .setNegativeButton(R.string.update_workspace) { _, _ -> openDocumentTree(SELECT) }
                .create()
                .show()
    }

    private fun showCreateAndSelectFolderDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildTitle())
                .setMessage(R.string.please_create_and_then_select_folder)
                .setPositiveButton(R.string.ok) { _, _ -> openDocumentTree(CREATE_AND_SELECT) }
                .create()
                .show()
    }

    private fun buildTitle(): Spanned {
        val title = "<b>${getString(R.string.welcome_to_story_producer)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildMessage(): Spanned {
        val message = getString(R.string.workspace_selection_help)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }

    private fun openDocumentTree(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(URI_PERMISSION_FLAGS)

        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && arrayOf(CREATE_AND_SELECT, SELECT).contains(requestCode)) {
            data?.data?.also {
                setupWorkspace(requestCode, it)
            }
        }
        showRegistration()
    }

    private fun setupWorkspace(requestCode: Int, uri: Uri) {
        Workspace.setupWorkspacePath(this, uri)

        contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        if (requestCode == CREATE_AND_SELECT) {
            Workspace.addDemoToWorkspace(this)
        }
    }

    private fun showRegistration() {
        startActivity(Intent(this, RegistrationActivity::class.java))
        finish()
    }

    companion object {

        private const val CREATE_AND_SELECT = 51
        private const val SELECT = 52

        private const val URI_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }

}
package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
        showDialog()
    }

    private fun showDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildTitle())
                .setMessage(buildMessage())
                .setPositiveButton(R.string.ok) { _, _ -> openDocumentTree() }
                .create()
                .show()
    }

    private fun buildTitle(): Spanned {
        val title = "<b>${getString(R.string.update_workspace)}</b>"
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

    private fun openDocumentTree() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(URI_PERMISSION_FLAGS)

        startActivityForResult(intent, RQS_OPEN_DOCUMENT_TREE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == RQS_OPEN_DOCUMENT_TREE) {
            Workspace.setupWorkspacePath(this,data?.data!!)
            contentResolver.takePersistableUriPermission(data.data!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {

        private const val RQS_OPEN_DOCUMENT_TREE = 52

        private const val URI_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }

}
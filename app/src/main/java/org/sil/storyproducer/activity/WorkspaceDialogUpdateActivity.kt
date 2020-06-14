package org.sil.storyproducer.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import org.sil.storyproducer.R

class WorkspaceDialogUpdateActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcomeDialog()
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildTitle())
                .setMessage(buildMessage())
                .setPositiveButton(R.string.use_internal_demo) { _, _ -> showCreateAndSelectFolderDialog() }
                .setNegativeButton(R.string.update_workspace) { _, _ -> selectTemplatesFolder() }
                .setCancelable(false)
                .create()
                .show()
    }

    private fun showCreateAndSelectFolderDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildTitle())
                .setMessage(R.string.please_create_and_then_select_folder)
                .setPositiveButton(R.string.ok) { _, _ -> selectTemplatesFolderAndAddDemo() }
                .setCancelable(false)
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
        val message = getString(R.string.use_internal_demo_or_select_template_folder)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }

}
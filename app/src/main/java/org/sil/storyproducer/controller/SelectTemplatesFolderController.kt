package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.isUriAutomaticallySelected
import org.sil.storyproducer.view.BaseActivityView

class SelectTemplatesFolderController(
        view: BaseActivityView,
        context: Context,
        val workspace: Workspace
) : BaseController(view, context) {

    fun openDocumentTree(request: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(URI_PERMISSION_FLAGS)
        view.startActivityForResult(intent, request)
    }

    // show dialog to ask if user wants to migrate the old stories
    private fun showMigrateIntenalStoriesDialog(request: Int, uri: Uri) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.migrate_stories_title))
            .setMessage(context.getString(R.string.migrate_stories_message))
            .setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                setupWorkspace(request, uri, true)
            }
            .setNegativeButton(context.getString(R.string.no)) { _, _ ->
                setupWorkspace(request, uri, false)
            }
            .setOnCancelListener() {
                setupWorkspace(request, uri, false)
            }
            .create()

        dialog.show()
    }

    fun onFolderSelected(request: Int, result: Int, data: Intent?) {
        data?.data?.also { uri ->
            if (result == Activity.RESULT_OK) {
                if (!workspace.setupWorkspacePath(context, uri, false))
                    return
                val newStories = workspace.storyDirectories()
                if (        // Check that new workspace is empty before migrating (moving) stories AND
                        newStories.isEmpty() &&
                            // check that there was an old workspace folder AND
                        workspace.previousWorkDocFile.exists() &&
                            // check that a different workspace folder was selected AND
                        workspace.previousWorkDocFile.uri != workspace.workdocfile.uri &&
                            // check that the old workspace was automatically selected AND
                        isUriAutomaticallySelected(workspace.previousWorkDocFile.uri) &&
                            // check that there exist stories in the old folder that do not exist in the new selected folder
                        workspace.oldStoryDirectories(newStories, true).isNotEmpty()) {
                    showMigrateIntenalStoriesDialog(request, uri)   // dialog calls setupWorkspace() appropriately
                } else {
                    setupWorkspace(request, uri, false)
                }
            }
        }
    }

    internal fun setupWorkspace(request: Int, uri: Uri, migrate: Boolean = false) {
        view.takePersistableUriPermission(uri)

        if (!workspace.setupWorkspacePath(context, uri))
            return

        if (shouldAddDemoToWorkspace(request)) {
            workspace.addDemoToWorkspace(context)
        }

        updateStories(migrate)  // always refresh list of stories
    }

    fun shouldAddDemoToWorkspace(request: Int): Boolean {
        if (request == SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO)
            return true

        // Add demo to automatically selected worksapce when no installed stories or stories to unzip or move
        if (isUriAutomaticallySelected(Workspace.workdocfile.uri) &&
                workspace.storyFilesToScanOrUnzipOrMove().isEmpty())
            return true

        return false
    }

    companion object {

        const val SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO = 51
        const val SELECT_TEMPLATES_FOLDER = 52
        const val UPDATE_TEMPLATES_FOLDER = 53
        val SELECT_TEMPLATES_FOLDER_REQUEST_CODES = arrayOf(
                SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO,
                SELECT_TEMPLATES_FOLDER,
                UPDATE_TEMPLATES_FOLDER
        )

        const val URI_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }

}

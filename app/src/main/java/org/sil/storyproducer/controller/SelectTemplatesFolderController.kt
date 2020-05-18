package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.sil.storyproducer.model.Workspace
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

    fun onFolderSelected(request: Int, result: Int, data: Intent?) {
        data?.data?.also { uri ->
            if (shouldSetupWorkspace(result, uri)) {
                setupWorkspace(request, uri)
                updateStories()
            }
        }
    }

    fun shouldSetupWorkspace(result: Int, uri: Uri?): Boolean {
        return result == Activity.RESULT_OK
                && !workspace.workdocfile.uri?.lastPathSegment.orEmpty().equals(uri?.lastPathSegment)
    }

    internal fun setupWorkspace(request: Int, uri: Uri) {
        view.takePersistableUriPermission(uri)

        workspace.setupWorkspacePath(context, uri)

        if (shouldAddDemoToWorkspace(request)) {
            workspace.addDemoToWorkspace(context)
        }
    }

    fun shouldAddDemoToWorkspace(request: Int): Boolean {
        return request == SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
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
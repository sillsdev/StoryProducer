package org.sil.storyproducer.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.view.BaseActivityView

class SelectTemplatesFolderController(val view: BaseActivityView, val context: Context) {

    fun openDocumentTree(request: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(URI_PERMISSION_FLAGS)
        view.startActivityForResult(intent, request)
    }

    fun onActivityResult(request: Int, result: Int, data: Intent?) {
        if (shouldSetupWorkspace(request, result)) {
            data?.data?.also { setupWorkspace(request, it) }
        }

        if (shouldShowRegistration(request)) {
            view.showRegistration()
        }
    }

    protected fun setupWorkspace(request: Int, uri: Uri) {
        if (shouldClearWorkspace(request)) {
            Workspace.clearWorkspace()
        }

        Workspace.setupWorkspacePath(context, uri)

        view.takePersistableUriPermission(uri)

        if (shouldAddDemoToWorkspace(request)) {
            Workspace.addDemoToWorkspace(context)
        }
    }

    fun shouldSetupWorkspace(request: Int, result: Int): Boolean {
        return SELECT_TEMPLATES_FOLDER_REQUEST_CODES.contains(request)
                && result == Activity.RESULT_OK
    }

    fun shouldShowRegistration(request: Int): Boolean {
        return true
    }

    fun shouldClearWorkspace(request: Int): Boolean {
        return request == UPDATE_TEMPLATES_FOLDER
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
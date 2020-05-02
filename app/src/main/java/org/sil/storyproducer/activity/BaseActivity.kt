package org.sil.storyproducer.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.SelectTemplatesFolderController
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.view.BaseActivityView

open class BaseActivity : AppCompatActivity(), BaseActivityView {

    lateinit var selectTemplatesFolderController: SelectTemplatesFolderController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectTemplatesFolderController = SelectTemplatesFolderController(this, this)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        selectTemplatesFolderController.onActivityResult(request, result, data)
    }

    fun selectTemplatesFolder() {
        selectTemplatesFolderController.openDocumentTree(SELECT_TEMPLATES_FOLDER)
    }

    fun selectTemplatesFolderAndAddDemo() {
        selectTemplatesFolderController.openDocumentTree(SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO)
    }

    fun updateTemplatesFolder() {
        selectTemplatesFolderController.openDocumentTree(UPDATE_TEMPLATES_FOLDER)
    }

    override fun takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    override fun showRegistration() {
        startActivity(Intent(this, RegistrationActivity::class.java))
        finish()
    }

}
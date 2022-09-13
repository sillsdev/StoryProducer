package org.tyndalebt.storyproduceradv.activities

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.File


/**
 * Activity creates the welcome screen dialog, used when the Workspace's project directory
 * hasn't been set up (new app install)
 */
class WelcomeDialogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*
        // Ask permission before trying to build folders (checkForDirectory)
        //  android:requestLegacyExternalStorage="true" needed to be added to the manifest folder for this to work on Android 10

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        else
            checkForDirectory()
*/
        showWelcomeDialog()
    }

/*
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // proceed
                    checkForDirectory()
                }
                else {
                    //not allowed
                    val errorToast = Toast.makeText(this, "This permission is required to save template information", Toast.LENGTH_LONG)
                    errorToast.show()
                }
            }
            else -> {
                // ignore other requests
            }
        }
    }

    private fun checkForDirectory() {

        val storages = ContextCompat.getExternalFilesDirs(this, null)

        var res: Boolean
        if (storages.size == 1) {
            // Build 1SP Workspace folder on the only storage space

              val docFile = File(storages[0], "1SP Workspace")
              if (!docFile.exists()) {
                  if (docFile.mkdirs()) {
                      println("directory created successfully")
                  }
              }
        }
        else {
            val docFile = File(storages[1], "1SP Workspace")
            if (!docFile.exists()) {
                if (docFile.mkdirs()) {
                    println("directory created successfully")
                }
            }
        }
    }

    fun externalMemoryAvailable(): Boolean {
        val storages = ContextCompat.getExternalFilesDirs(this, null)
        return storages.size > 1 && storages[0] != null && storages[1] != null
    }
*/

    private fun showWelcomeDialog() {
        val welcomeDialog = AlertDialog.Builder(this).create()

        // Issue #541: welcome screen is now created using a layout View
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_welcome_screen, null) as ViewGroup
        val mainTextView = view.findViewById<TextView>(R.id.main_textview)
        mainTextView.setText(buildMessage())
        mainTextView.movementMethod = LinkMovementMethod.getInstance()

        // demo button not available, show error as of 3.0.4beta
        val demoTextView = view.findViewById<TextView>(R.id.demo_textview);
        demoTextView.setOnClickListener {
            val errorToast = Toast.makeText(this, R.string.welcome_screen_demo_error, Toast.LENGTH_SHORT)
            errorToast.show()
        }

        val selectTemplatesButton = view.findViewById<Button>(R.id.select_templates_button);
        selectTemplatesButton.setOnClickListener {
            showSelectTemplatesFolderDialog()
            welcomeDialog.dismiss()
        }

        // build rest of welcome dialog
        welcomeDialog.setTitle(buildTitle())
        welcomeDialog.setView(view)
        welcomeDialog.setCancelable(false)
        welcomeDialog.show()
    }

    private fun buildTitle(): Spanned {
        val title = "<b>${getString(R.string.welcome_to_story_producer)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildMessage(): Spanned {
        // DKH - 01/15/2022 Issue #571: Add a menu item for accessing templates from Google Drive
        // The strings.xml file contains the "Welcome Screen" html in the following string:
        // <string name="welcome_screen_select_template_folder">).
        // This update moved the actual URL for the template folder from the strings.xml file
        // to the Workspace object.  A place holder string was placed in the "Welcome Screen" html,
        // so, update the place holder string with the actual URL before we display the
        // "Welcome Screen" to the user.
//        val message = getString(R.string.welcome_screen_select_template_folder).
//            replace(Regex(Workspace.URL_FOR_TEMPLATES_PLACE_HOLDER), Workspace.URL_FOR_TEMPLATES)
        val message = getString(R.string.welcome_screen_select_template_folder_new)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }
}

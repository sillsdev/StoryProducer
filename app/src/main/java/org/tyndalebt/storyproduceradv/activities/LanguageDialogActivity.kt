package org.tyndalebt.storyproduceradv.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
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
import java.io.IOException
import java.io.InputStream


/**
 * Activity creates the language screen dialog, used when the Workspace's default language
 * hasn't been set up (new app install)
 */
class LanguageDialogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Workspace.addLanguageToWorkspace(this)
//        showLanguageDialog()

    }

    private fun showLanguageDialog() {
        // open languageFile list and display it for selecting strings file to use
        // will be changed to the copy put into the app area after it is changed
        //     in the Workspace - addLanguageToWorkspace file

        var MyInput1: String = ""
        var MyInput2: String = ""

        try {
            val inputStream: InputStream = assets.open("language.csv")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            MyInput1 = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }



    }

}



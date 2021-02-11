package org.sil.storyproducer.activities

import android.app.AlertDialog
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
import org.sil.storyproducer.R

/**
 * Activity creates the welcome screen dialog, used when the Workspace's project directory
 * hasn't been set up (new app install)
 */
class WelcomeDialogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcomeDialog()
    }

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
        val message = getString(R.string.welcome_screen_select_template_folder)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }

}

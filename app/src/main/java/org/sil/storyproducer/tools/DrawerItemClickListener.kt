package org.sil.storyproducer.tools

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.WorkspaceUpdateActivity

class DrawerItemClickListener(private val activity: AppCompatActivity) : AdapterView.OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        selectItem(position)
    }

    /** Swaps fragments in the main content view  */
    private fun selectItem(position: Int) {
        val intent: Intent
        when (position) {
            0 -> {
                intent = Intent(activity.applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                activity.finish()
            }
            1 -> {
                intent = Intent(activity, RegistrationActivity::class.java)
                activity.startActivity(intent)
                activity.finish()
            }
            2 -> {
                intent = Intent(activity, WorkspaceUpdateActivity::class.java)
                activity.startActivity(intent)
                activity.finish()
            }
            3 -> {
                val dialog = AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.license_title))
                        .setMessage(activity.getString(R.string.license_body))
                        .setPositiveButton(activity.getString(R.string.ok)) { _, _ -> }.create()
                dialog.show()
            }
        }
    }
}

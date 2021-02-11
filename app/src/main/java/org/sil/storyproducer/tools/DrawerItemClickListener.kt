package org.sil.storyproducer.tools

import android.content.Intent
import android.view.View
import android.widget.AdapterView
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.model.Workspace

class DrawerItemClickListener(private val activity: BaseActivity) : AdapterView.OnItemClickListener {

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
                activity.showRegistration()
            }
            2 -> {
                activity.showSelectTemplatesFolderDialog()
            }
            3 -> {
                Workspace.addDemoToWorkspace(activity)
            }
            4 -> {
                activity.showAboutDialog()
            }
        }
    }
}

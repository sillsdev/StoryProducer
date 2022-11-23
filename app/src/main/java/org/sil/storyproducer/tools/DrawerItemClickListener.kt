package org.sil.storyproducer.tools

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import androidx.drawerlayout.widget.DrawerLayout
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.model.Workspace

class DrawerItemClickListener(private val activity: BaseActivity) : AdapterView.OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {

        val activity : Activity? = view.context as Activity
        if (activity != null) {
            val mDrawerLayout = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
            mDrawerLayout.closeDrawers()    // keep it tidy
        }

        selectItem(position)
    }

    /** Swaps fragments in the main content view  */
    private fun selectItem(position: Int) {
        when (position) {
            0 -> {
                activity.finish()   // finish this activity to reveal main activity
            }
            1 -> {
                // Showing registration on top of the MainActivity in the task stack
                // This should allow registering without finishing the main activity
                // and without hanging in the splash screen as previously experienced
                activity.showRegistration(true)
            }
            2 -> {
                activity.showBLDownloadDialog()
            }
//            3 -> {
//                // DKH - 01/23/2022 Issue #571: Add a menu item for accessing templates from Google Drive
//                // A new menu item was added that opens a URL for the user to download templates.
//                // If we get here, the user wants to browse for more templates, so,
//                // open the URL in a new activity
//                Workspace.startDownLoadMoreTemplatesActivity(activity)
//            }
            3 -> {
                activity.showWordLinksList()
            }
            4 -> {
                activity.showSelectTemplatesFolderDialog()
            }
            5 -> {
                Workspace.addDemoToWorkspace(activity)
                activity.controller.updateStories()  // refresh list of stories
            }
            6 -> {
                activity.showAboutDialog()
            }
        }
    }
}

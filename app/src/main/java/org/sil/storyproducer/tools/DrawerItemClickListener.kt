package org.sil.storyproducer.tools

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import androidx.drawerlayout.widget.DrawerLayout
import org.sil.bloom.reader.BloomLibraryActivity
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.wordlink.WordLinksListActivity
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

        var pos = position
        var wordLinksMenuPos = Workspace.wordLinksRemoveMenuPos(activity)
        if (wordLinksMenuPos != -1 && pos >= wordLinksMenuPos)
            pos++   // skip over WordLinks menu position if necessary
        when (pos) {
            0 -> {
                if (activity !is MainActivity)
                    activity.finish()   // if this is not the main activity then
                                        // finish this activity to reveal main activity
            }
            1 -> {
                // Showing registration on top of the MainActivity in the task stack
                // This should allow registering without finishing the main activity
                // and without hanging in the splash screen as previously experienced
                activity.showRegistration(false)
                if (activity !is MainActivity)
                    activity.finish()   // replace this activity with Registration activity on top
                                        // but only if the current activity is not the Main Activity
            }
            2 -> {
                activity.showBLDownloadDialog(BaseActivity.BLOOM_DL_TEMPLATES_ACTIVITY)
                if (activity !is MainActivity)
                    activity.finish()   // replace this activity with SP Bloom Template DL activity on top
                                        // but only if the current activity is not the Main Activity
            }
//            2 -> {
//                activity.showBLDownloadDialog(BaseActivity.BLOOM_DL_FEATURED_ACTIVITY)
//                if (activity !is MainActivity)
//                    activity.finish()   // replace this activity with Featured Bloom Book DL activity on top
//                                        // but only if the current activity is not the Main Activity
//            }
            3 -> {
                val intent = Intent(MainActivity.mainActivity, BloomLibraryActivity::class.java)
                MainActivity.mainActivity?.startActivity(intent)
                if (activity !is MainActivity)
                    activity.finish()   // replace this activity with Bloom Library WebView activity on top
                                        // but only if the current activity is not the Main Activity
            }
            4 -> {
                if (activity !is WordLinksListActivity) {
                    activity.showWordLinksList()
                }
            }
            5 -> {
                activity.showSelectTemplatesFolderDialog()
            }
            6 -> {
                Workspace.addDemoToWorkspace(activity)
                activity.controller.updateStories()  // refresh list of stories
            }
            7 -> {
                activity.showSettings()
            }
            8 -> {
                activity.showAboutDialog()
            }
        }
    }
}

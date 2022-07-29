package org.tyndalebt.spadv.tools

import android.content.Intent
import android.view.View
import android.widget.AdapterView
import org.tyndalebt.spadv.activities.BaseActivity
import org.tyndalebt.spadv.controller.MainActivity
import org.tyndalebt.spadv.model.Workspace

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
                // 06/14/2021 - DKH, Issue 407, Pull Request 561 - Merge into Latest sillsdev
                // The new Filter layout for the Story Templates broke the capability to launch
                // the showRegistration () from the calling activity.  SP uses the paradigm of
                // always finishing an activity and then moving onto the next activity.  In this case
                // it would be to finish the current activity and move on to the
                // Registration Activity.  However, when a submit is
                // done from registration, another application such as WhatsApp or Messenger takes
                // over and when they finished, they exit without starting another SP activity.
                // This causes SP to hang in the Splash Activity with no where to go.  This is
                // similar to the issue fixed in
                // Issue #573: SP will hang/crash when submitting registration
                // In that fix, the Main activity does not finish before starting the registration
                // activity.  So, employ the same fix, ie, tell the main activity to display the
                // registration when the main activity is started and then start the main activity
                // See comments in MainActivity for more details
                Workspace.showRegistration = true
                activity.showMain()
            }
            2 -> {
                // DKH - 01/23/2022 Issue #571: Add a menu item for accessing templates from Google Drive
                // A new menu item was added that opens a URL for the user to download templates.
                // If we get here, the user wants to browse for more templates, so,
                // open the URL in a new activity
                Workspace.startDownLoadMoreTemplatesActivity(activity)
            }
            3 -> {
                activity.showWordLinksList()
            }
            4 -> {
                activity.showSelectTemplatesFolderDialog()
            }
            5 -> {
                Workspace.addDemoToWorkspace(activity)
            }
            6 -> {
                activity.showAboutDialog()
            }
        }
    }
}

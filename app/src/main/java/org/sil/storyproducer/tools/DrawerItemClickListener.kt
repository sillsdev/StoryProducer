package org.sil.storyproducer.tools

import android.content.Intent
import android.view.View
import android.widget.AdapterView
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.activities.WelcomeDialogActivity
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
                activity.showWordLinksList()
            }
            2 -> {
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
            3 -> {
                // DKH - 11/8/2021
                // Issue #571: Add a menu item for accessing templates from Google Drive
                // Instead of adding a new menu item, repurpose the "Select 'SP Templates' Folder"
                // option in the hamburger menu in the Phase screen (eg, Learn, Translate + Revise, etc)
                // The user selects "Select 'SP Templates' Folder" from the hamburger menu and the
                // "Welcome Dialog Screen" appears.  The user then selects the option  to
                // "Use Google Drive and download story Templates" in the "Welcome Dialog Screen.
                // This places Story Producer in the background and Google Drive interface appears.
                // The user downloads the templates into the download directory on the phone and
                // then uses the Android folder app to moves the files
                // from the download folder into a target folder (user may create a new folder for
                // the newly downloaded templates or use an existing folder).
                // The user then brings Story Producer to
                // the foreground.  The user then selects "Select 'SP Templates' Folder" at the
                // bottom of the "Welcome Dialog Screen" and proceeds to process the target folder.
                // Previous call interface: showSelectTemplatesFolderDialog()
                // New call interface to bring up "Welcome Dialog Screen"
                activity.startActivity(Intent(activity.applicationContext, WelcomeDialogActivity::class.java))
                // since this menu selection is in a Phase activity, exit the phase activity
                // which will force the control to the "Story Template" screen in the
                // main activity
                activity.finish()
            }
            4 -> {
                Workspace.addDemoToWorkspace(activity)
            }
            5 -> {
                activity.showAboutDialog()
            }
        }
    }
}

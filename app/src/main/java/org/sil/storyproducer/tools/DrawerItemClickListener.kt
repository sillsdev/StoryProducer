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
                activity.showSelectTemplatesFolderDialog()
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

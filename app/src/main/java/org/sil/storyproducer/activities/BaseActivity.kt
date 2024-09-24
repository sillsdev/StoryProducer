package org.sil.storyproducer.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import io.reactivex.disposables.CompositeDisposable
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.BaseController
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.SelectTemplatesFolderController
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_REQUEST_CODES
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.SettingsActivity
import org.sil.storyproducer.controller.bldownload.BLDownloadActivity
import org.sil.storyproducer.controller.wordlink.WordLinksListActivity
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.DrawerItemClickListener
import org.sil.storyproducer.tools.file.isUriStorageMounted
import org.sil.storyproducer.view.BaseActivityView
import timber.log.Timber

open class BaseActivity : AppCompatActivity(), BaseActivityView {

    lateinit var controller: SelectTemplatesFolderController

    private var readingTemplatesDialog: AlertDialog? = null
    private var cancellingReadingTemplatesDialog: AlertDialog? = null

    protected val subscriptions = CompositeDisposable()

    protected var mDrawerList: ListView? = null
    protected var mDrawerLayout: androidx.drawerlayout.widget.DrawerLayout? = null
    protected var mDrawerToggle: ActionBarDrawerToggle? = null
    protected var mAdapter: ArrayAdapter<String>? = null
    protected var mPopupHelpUtils: PopupHelpUtils? = null
    protected var rootView: View? = null


    companion object {
        const val BLOOM_DL_TEMPLATES_ACTIVITY = 0
        const val BLOOM_DL_FEATURED_ACTIVITY = 1
        const val BLOOM_DL_ACTIVITY_INDEX = "BLDL_Activity_Index"

        fun setLockTextAndImage(context: Context, lockTextView: TextView?) {
            if (lockTextView == null)
                return

            // set bold spannable
            var lockScreenText = (lockTextView.text ?: return) as String
            val boldStart = lockScreenText.indexOf("<b>")
            val boldEnd = lockScreenText.indexOf("</b>") - 3
            lockScreenText = lockScreenText.replace("<b>", "")
            lockScreenText = lockScreenText.replace("</b>", "")
            if (boldStart < 0 || boldEnd < 0)
                return
            val spannableString = SpannableString(lockScreenText)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), boldStart, boldEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // set tick image spannable
            val tickImageSpan = ImageSpan(context, R.drawable.ic_checkmark_green)
            val tickStart = lockScreenText.indexOf("|")
            val tickEnd = tickStart + 1
            spannableString.setSpan(tickImageSpan, tickStart, tickEnd, 0)

            // replace the text with spannable bold and image
            lockTextView.text = spannableString
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)
        Timber.tag(javaClass.simpleName).v("onCreate")
        controller = SelectTemplatesFolderController(this, this, Workspace)
    }

    override fun onPause() {
        super.onPause()
        stopAndDeletePopupMenus()
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(javaClass.simpleName).v("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndDeletePopupMenus()
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)

        // This is a generic handler for all template folder picking request results
        // The result can be checked for RESULT_CANCELED in a derived class for specific actions
        if (result == RESULT_OK) {
            if (SELECT_TEMPLATES_FOLDER_REQUEST_CODES.contains(request)) {
                controller.onFolderSelected(request, result, data)
            }
        }
    }

    fun initWorkspace() {
        Workspace.initializeWorkspace(this)
        var showWelcome = true  // set to false if no need to show welcome wizard
        do {
            if (Workspace.workdocfile.isDirectory) {    // i.e. is the workspace still accessable
                controller.updateStories()  // We already have a workspace folder
                showWelcome = false
                break
            }
            if (!isUriStorageMounted(Workspace.workdocfile.uri)) {
                // TODO: Replace 'media not mounted' toast message with a better Startup Wizard
                Toast.makeText(this, R.string.workspace_not_mounted, Toast.LENGTH_LONG).show()
                showWelcome = true
                break
            }
            val workspaceCreatedMsg = Workspace.createAppSpecificWorkspace()
            if (workspaceCreatedMsg.isNotEmpty()) {
                controller.updateStories()  // Update after a new app-specific workspace was created
                Toast.makeText(this, workspaceCreatedMsg, Toast.LENGTH_LONG).show()
                showWelcome = false
                break
            }
        } while (false)

        if (showWelcome) {
            showWelcomeDialog()
        }

    }

    private fun showWelcomeDialog() {
        startActivity(Intent(this, WelcomeDialogActivity::class.java))
//        finish()  // removed to keep back button working on MainActivity
    }

    fun updateTemplatesFolder() {
        controller.openDocumentTree(UPDATE_TEMPLATES_FOLDER)
    }

    override fun takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    override fun showMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // DKH - 05/12/2021
    // Issue #573: SP will hang/crash when submitting registration
    //
    // showRegistration Argument allows the caller in the current Activity to finish or
    // not finish the current Activity before starting the RegistrationActivity.
    // The MainActivity should set executeFinishActivity to false so that when the registration
    // is complete, there is a Story Producer activity that will control execution.
    // After the RegistrationActivity completes, control is returned to the MainActivity
    // where the list of story templates are displayed
    //
    // 06/14/2021 - DKH, Issue 407, Pull Request 561 - Merge into Latest sillsdev
    // Updated selectItem in DrawerItemclickListener to set Workspace.showRegistration
    // to true and then call showMain() instead of calling showRegistration.  This is equivalent
    // to calling showRegistration.  See selectItem for more detail.
    //
    // All showRegistration calls should be done through the MainActivity to
    // avoid hanging Story Producer.
    override fun showRegistration(executeFinishActivity: Boolean) {
        startActivity(Intent(this, RegistrationActivity::class.java))

        // If true, then this Activity will finish and exit
        if(executeFinishActivity) {
            finish()
        }
    }

    fun showWordLinksList() {
        startActivity(Intent(this, WordLinksListActivity::class.java))
//        finish()  // removed to keep back button working on MainActivity
    }

    override fun showReadingTemplatesDialog(controller: BaseController) {
        readingTemplatesDialog = AlertDialog.Builder(this)
                .setTitle(R.string.scanning_sp_templates)
                .setMessage("")
                .setNegativeButton(R.string.cancel) { _, _ -> controller.cancelUpdate() }
                .setCancelable(false)
                .create()

        readingTemplatesDialog?.show()
    }

    override fun showCancellingReadingTemplatesDialog() {
        cancellingReadingTemplatesDialog = AlertDialog.Builder(this)
                .setTitle(R.string.scanning_sp_templates)
                .setMessage(R.string.cancelling)
                .create()

        cancellingReadingTemplatesDialog?.show()
    }

    override fun updateReadingTemplatesDialog(current: Int, total: Int, currentTemplate: String) {
        readingTemplatesDialog?.setMessage("$current of $total templates\n\n$currentTemplate")
    }

    override fun hideReadingTemplatesDialog() {
        if (readingTemplatesDialog != null ) {
            if (readingTemplatesDialog?.isShowing!!)
                readingTemplatesDialog?.dismiss()
            readingTemplatesDialog = null
        }
        cancellingReadingTemplatesDialog?.dismiss()
        cancellingReadingTemplatesDialog = null
    }

    fun showSelectTemplatesFolderDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildSelectTemplatesTitle())
                .setMessage(buildSelectTemplatesMessage())
                .setPositiveButton(R.string.next) { _, _ -> updateTemplatesFolder() }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    private fun buildSelectTemplatesTitle(): Spanned {
        val title = "<b>${getString(R.string.select_workspace_folder)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildSelectTemplatesMessage(): Spanned {
        val message = getString(R.string.select_workspace_help_msg)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message)
        }
    }

    fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setView(buildAboutDialogView())
            .setPositiveButton(getString(R.string.ok), null)
            .create()
            .show()
    }

    fun showSettings() {
        startActivity(Intent(this,  SettingsActivity::class.java))
    }

    private fun buildAboutDialogView(): View {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        return layoutInflater.inflate(R.layout.dialog_about, null).apply {
            findViewById<TextView>(R.id.appVersion)
                    .setText(getString(R.string.app_version, versionName))
        }
    }

    fun showBLDownloadDialog(downloadIndex: Int) {
        val bldlIntent = Intent(this, BLDownloadActivity::class.java)
        bldlIntent.putExtra(BLOOM_DL_ACTIVITY_INDEX, downloadIndex)
        startActivity(bldlIntent)
    }

    /**
     * initializes the items that the drawer needs
     */
    protected fun setupDrawer() {

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        mDrawerList = findViewById(R.id.navList)
        mDrawerList!!.bringToFront()
        mDrawerLayout = findViewById(R.id.drawer_layout)
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        addDrawerItems()
        mDrawerList!!.onItemClickListener = DrawerItemClickListener(this)
        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.nav_open, R.string.nav_close) {

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
        mDrawerToggle!!.isDrawerIndicatorEnabled = true
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)
        mDrawerToggle!!.syncState()
    }

    /**
     * adds the items to the drawer from the array resources
     */
    private fun addDrawerItems() {
        val menuArray = resources.getStringArray(R.array.global_menu_array).toMutableList() //as MutableList<String>

        var wordLinksMenuPos = Workspace.wordLinksRemoveMenuPos(applicationContext)
        if (wordLinksMenuPos != -1)
            menuArray.removeAt(wordLinksMenuPos)    // remove the WordLinks menu item

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuArray)
        mDrawerList!!.adapter = mAdapter
    }

    class CustomPopupMenu(context: Context, anchor: View) :
        PopupMenu(context, anchor)
    {
        init {
            setForceShowIcon(true)
        }
    }

    protected fun showHelpContextMenu() {
        val anchorView = findViewById<View>(R.id.helpButton) // Provide the id of the help button
        val helpMenu = CustomPopupMenu(this, anchorView)
        helpMenu.inflate(R.menu.help_context_menu)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // check setting for removing dismiss help
        val enableDismissMenu = prefs.getBoolean("help_dismissal_menu", true)
        val menu = helpMenu.menu
        if (!enableDismissMenu) {
            menu.removeItem(R.id.action_popup_help_stop)
        }

        helpMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {    
                R.id.action_detailed_help -> {
                    // Handle detailed help option
                    showDetailedHelp()
                    true
                }
                R.id.action_popup_help_restart -> {
                    // Handle popup help option
                    restartShowingPopupHelp()
                    checkDownloadStoriesMessage()
                    true
                }
                R.id.action_popup_help_resume -> {
                    // Handle popup help option
                    resumeShowingPopupHelp()
                    checkDownloadStoriesMessage()
                    true
                }
                R.id.action_popup_help_stop -> {
                    // Handle popup help option
                    stopShowingPopupHelp()
                    checkDownloadStoriesMessage()
                    true
                }
                else -> false
            }
        }

        helpMenu.show()
    }

    override fun showDetailedHelp() {
    }

    override fun restartShowingPopupHelp() {
        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.restartShowingPopupHelp()
    }

    override fun resumeShowingPopupHelp() {
        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.resumeShowingPopupHelp()
    }

    override fun stopShowingPopupHelp() {

        if (mPopupHelpUtils != null) {
            mPopupHelpUtils?.stopShowingAllHelp()
        }
    }
    fun setBasePopupHelpUtils(popupHelp: PopupHelpUtils) {
        mPopupHelpUtils = popupHelp
    }

    fun stopAndDeletePopupMenus() {

        if (mPopupHelpUtils != null) {
            mPopupHelpUtils?.dismissPopup()
            mPopupHelpUtils = null
        }
    }

    override fun checkDownloadStoriesMessage() {

    }

}

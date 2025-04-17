package org.sil.storyproducer.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
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
import org.sil.storyproducer.App
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.BaseController
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.PopupHelpUtils
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.SelectTemplatesFolderController
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.SELECT_TEMPLATES_FOLDER_REQUEST_CODES
import org.sil.storyproducer.controller.SelectTemplatesFolderController.Companion.UPDATE_TEMPLATES_FOLDER
import org.sil.storyproducer.controller.SettingsActivity
import org.sil.storyproducer.controller.accuracycheck.AccuracyCheckFrag
import org.sil.storyproducer.controller.bldownload.BLDownloadActivity
import org.sil.storyproducer.controller.communitywork.CommunityWorkFrag
import org.sil.storyproducer.controller.export.FinalizeActivity
import org.sil.storyproducer.controller.export.ShareActivity
import org.sil.storyproducer.controller.learn.LearnActivity
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.controller.translaterevise.TranslateReviseFrag
import org.sil.storyproducer.controller.voicestudio.VoiceStudioFrag
import org.sil.storyproducer.controller.wordlink.WordLinksListActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.DrawerItemClickListener
import org.sil.storyproducer.tools.file.isUriStorageMounted
import org.sil.storyproducer.view.BaseActivityView
import timber.log.Timber
import java.util.Locale

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
            var lockScreenText = (lockTextView.text.toString() ?: return) as String
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

    // A helper singleton for setting language code and persistent setting
    object LanguageHelper {
        fun setLocale(context: Context, languageCode: String): Context {

            // create a new Locale object to use as the default language
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            // create a new Configuration for the new Locale
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)

            // Save selected language in preferences
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferences.edit().putString("language", languageCode).apply()

            // update language for resources
            resources.updateConfiguration(config, resources.displayMetrics)

            // update language for global resources
            App.appContext.resources.updateConfiguration(config, App.appContext.resources.displayMetrics)

            // store language code for early use before preferences are available
            App.languageCode = languageCode

            // return a new context that uses the new language configuration
            return context.createConfigurationContext(config)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // use the global default language in case preferences are not available
        var savedLanguage = App.languageCode
        try {
            // get the language from the default (setting activity) preferences
            var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(newBase)
            savedLanguage = sharedPreferences.getString("language", "") ?: ""
        } catch (ex: Exception) { }

        if (savedLanguage.isNotEmpty()) {
            val newContext = LanguageHelper.setLocale(newBase, savedLanguage) // Change to desired language code
            super.attachBaseContext(newContext) // use the new language context for all activities
        } else {
            super.attachBaseContext(newBase)    // default behaviour
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
        readingTemplatesDialog?.setMessage(baseContext.getString(R.string.update_reading_templates_dialog, current, total, currentTemplate))
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
        val grayOutPhaseVideos = !PopupHelpUtils.enableIndependentVideos && this is MainActivity  // can't play phase videos in main Story List if old way
        val parentMenu = helpMenu.menu.findItem(R.id.action_help_video_parent)
        if (parentMenu.hasSubMenu()) {
            var grayIcon: Drawable? = null
            val subMenu = parentMenu.subMenu
            var count = 1
            var found = false
            for (j in 0 until subMenu!!.size()) {
                val subItem = subMenu!!.getItem(j)
                if (!found && subItem.itemId == R.id.action_help_video_1_learn) {
                    found = true    // start graying out from lean phase video
                    if (grayOutPhaseVideos) {
                        grayIcon = subItem.icon?.mutate()   // create a reusable gray icon
                        grayIcon?.alpha = 128   //  make it grayed out on a black background
                    }
                }
                if (found) {
                    if (grayOutPhaseVideos && grayIcon != null) {
                        subItem.icon = grayIcon     // use the gray icon
                    }
                    subItem.title = "${count}. ${subItem.title}" // Prepend phase number
                    count++
                }
            }
        }

        helpMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {    
                R.id.action_help_detailed -> {
                    // Handle detailed help option
                    showDetailedHelp()
                    true
                }
                R.id.action_help_restart_btw -> {
                    // Handle restarting btw windows
                    restartShowingPopupHelp(true)   // true = skip videos
                    checkDownloadStoriesMessage()   // display snack bar message if needed
                    true
                }
                R.id.action_help_toggle_btw -> {
                    // Handle hide/show btw windows
                    toggleShowingPopupHelp()
                    checkDownloadStoriesMessage()   // display snack bar message if needed
                    true
                }
                R.id.action_help_video_overview -> {
                    // show video overview
                    if (PopupHelpUtils.enableIndependentVideos)
                        PopupHelpUtils.showIndependentHelpVideo(this, "html5/00_Introduction_en/00_Introduction_en.html")
                    else if (this !is MainActivity) {  // if this is not the main activity then exit story
                        val popupHelpUtils = PopupHelpUtils(this, MainActivity::class.java)
                        popupHelpUtils.resumeShowingPopupHelp(true, 0) // clear MainActivity flags so first video help will show later
                        finish()    // finish this story phase activity to reveal main activity
                    }
                    else {
                        // already showing main Story List activity
                        MainActivity.mainActivity?.resumeShowingPopupHelp(false, 0) // show first video now
                    }
                    true
                }
                R.id.action_help_video_main -> {
                    // show video overview
                    if (PopupHelpUtils.enableIndependentVideos)
                        PopupHelpUtils.showIndependentHelpVideo(this, "html5/0_MainScreen_en/0_MainScreen_en.html")
                    else if (this !is MainActivity) {  // if this is not the main activity then
                        val popupHelpUtils = PopupHelpUtils(this, MainActivity::class.java)
                        popupHelpUtils.resumeShowingPopupHelp(true, 1) // clear MainActivity flags so second video help will show later
                        finish()    // finish this story phase activity to reveal main activity
                    }
                    else
                        MainActivity.mainActivity?.resumeShowingPopupHelp(false, 1) // show second video now
                    true
                }
                // Switch case all phase videos
                R.id.action_help_video_1_learn,
                R.id.action_help_video_2_revise,
                R.id.action_help_video_3_community,
                R.id.action_help_video_4_accuracy,
                R.id.action_help_video_5_drama,
                R.id.action_help_video_6_create,
                R.id.action_help_video_7_share -> {
                    var cls: Class<*>? = null    // the class for the phase's persistent popup help flags
                    var phase: PhaseType? = null    // the phase type to switch to
                    var partialUrl: String? = null
                    when (menuItem.itemId) {
                        // set cls and phase for all phase videos
                        R.id.action_help_video_1_learn -> {cls = LearnActivity::class.java; phase = PhaseType.LEARN; partialUrl = "html5/1_Learn_en/1_Learn_en.html"}
                        R.id.action_help_video_2_revise -> {cls = TranslateReviseFrag::class.java; phase = PhaseType.TRANSLATE_REVISE; partialUrl = "html5/2_Record_en/2_Record_en.html"}
                        R.id.action_help_video_3_community -> {cls = CommunityWorkFrag::class.java; phase = PhaseType.COMMUNITY_WORK; partialUrl = "html5/3_Community_en/3_Community_en.html"}
                        R.id.action_help_video_4_accuracy -> {cls = AccuracyCheckFrag::class.java; phase = PhaseType.ACCURACY_CHECK; partialUrl = "html5/4_Accuracy_en/4_Accuracy_en.html"}
                        R.id.action_help_video_5_drama -> {cls = VoiceStudioFrag::class.java; phase = PhaseType.VOICE_STUDIO; partialUrl = "html5/5_Drama_en/5_Drama_en.html"}
                        R.id.action_help_video_6_create -> {cls = FinalizeActivity::class.java; phase = PhaseType.FINALIZE; partialUrl = "html5/6_Create_en/6_Create_en.html"}
                        R.id.action_help_video_7_share -> {cls = ShareActivity::class.java; phase = PhaseType.SHARE; partialUrl = "html5/7_Share_en/7_Share_en.html"}
                    }
                    if (PopupHelpUtils.enableIndependentVideos && partialUrl != null)
                        PopupHelpUtils.showIndependentHelpVideo(this, partialUrl)
                    else if (this !is MainActivity && cls != null && phase != null && partialUrl != null) {  // if this is not the main activity then show phase video help
                        if (Workspace.activePhase.phaseType == phase) {
                            // we are already on the selected video phase
                            Workspace.activeSlideNum = 0    // go to title slide

                            // restart the phase Intent so that the help video will reshow
                            val intent = Intent(this.applicationContext, Phase(phase).getTheClass())
                            intent.putExtra("storyname", Workspace.activeStory.title)
                            startActivity(intent)   // restart the same phase as this
                            finish()    // finish this activity

                            // clear phase activity flags so video help will show later
                            val popupHelpUtils = PopupHelpUtils(this, cls)
                            popupHelpUtils.resumeShowingPopupHelp(true, 0)  // show the first item (video)

                        } else {
                            // we need to switch phases
                            Workspace.activeSlideNum = 0    // go to title slide

                            (this as PhaseBaseActivity).jumpToPhase(Phase(phase))   // jump to the new help video phase

                            // clear new phase activity flags so video help will show later
                            val popupHelpUtils = PopupHelpUtils(this, cls)
                            popupHelpUtils.resumeShowingPopupHelp(true, 0)  // show the first item (video)
                        }
                    }
                    if (!PopupHelpUtils.enableIndependentVideos && this is MainActivity) {
                        // if this is the main activity then show toast message to select a story
                        Toast.makeText(baseContext, baseContext.getString(R.string.help_video_open_story_first), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        helpMenu.show()
    }

    override fun showDetailedHelp() {
    }

    override fun toggleShowingPopupHelp() {
        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.toggleShowingPopupHelp()
    }

    override fun restartShowingPopupHelp(skipVideos: Boolean) {
        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.restartShowingPopupHelp(skipVideos)
    }

    override fun resumeShowingPopupHelp(startLater: Boolean, helpIndex: Int) {
        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.resumeShowingPopupHelp(startLater, helpIndex)
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

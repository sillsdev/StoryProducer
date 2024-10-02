package org.sil.storyproducer.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.core.view.GravityCompat
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.storylist.StoryPageAdapter
import org.sil.storyproducer.controller.storylist.StoryPageTab
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.ConnectivityStatus
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.file.deleteWorkspaceFile
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

class MainActivity : BaseActivity(), Serializable {

    companion object {
        var mainActivity : MainActivity ? = null
    }

    lateinit var storyPageViewPager : ViewPager2
    lateinit var storyPageTabLayout : TabLayout

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!ConnectivityStatus.isConnected(context)) {
                Log.i("Connection Change", "no connection")

                VolleySingleton.getInstance(context).stopQueue()
            } else {
                Log.i("Connection Change", "Connected")

                VolleySingleton.getInstance(context).startQueue()
            }
        }
    }

    var globalChipMsgCount = 0
        get () {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val prefString = "PopupHelpGroup_chipMsgCountCount"
            field = prefs.getInt(prefString, 0)
            return field
        }
        set(value) {
            if (field != value) {
                val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                val prefString = "PopupHelpGroup_chipMsgCountCount"
                preferencesEditor.putInt(prefString, value)
                preferencesEditor.commit()
                field = value
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = this

        setContentView(R.layout.activity_main)

        if (mDrawerList == null) {
            setupDrawer()             // also added to onStart()
            setupStoryListTabPages()  // ditto
        } else {
            setupStoryListTabPages()    // Updates any 'in-progress' story markers
        }

        if (!Workspace.isInitialized) {
            initWorkspace()
        }

        if (Workspace.showRegistration and !Workspace.showRegistrationSkiped) {

            Workspace.showRegistrationSkiped = true

            // DKH - 05/12/2021
            // Issue #573: SP will hang/crash when submitting registration
            // This flag indicates that MainActivity should create the
            // RegistrationActivity and show the registration screen.
            // This is set in BaseController function onStoriesUpdated()
            Workspace.showRegistration = false

            // When starting the RegistrationActivity from the MainActivity, specify that
            // finish should not be called on the MainActivity.
            // This is done by setting executeFinishActivity to false.
            // After the RegistrationActivity is complete, MainActivity will then display
            // the story template list
            showRegistration(false)
        }
        // DKH - 07/10/2021 - Issue 407: Add filtering to SP's 'Story Templates' List
        // Updated while integrating pull request #561 into current sillsdev baseline
        // This was deleted in pull request #561.
        // It was added back in because it monitors the network connection for VolleySingleton
        // and is used by  for support of RemoteCheckFrag.java,
        // AudioUpload.java & BackTranslationUpload.java
        GlobalScope.launch {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this@MainActivity.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), RECEIVER_EXPORTED)
                } else {
                    this@MainActivity.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
                }
            }
        }
    }

    private fun addAndStartPopupTutorials() {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        mPopupHelpUtils = PopupHelpUtils(this)

        // assume a maximum of 9 stories can be viewed
        val totalVisibleStories = max(min(Workspace.Stories.count(), 9), 1)
        // so point (roughly) to end of first story
        val storyListViewPercent = (100f / totalVisibleStories).toInt()

        mPopupHelpUtils?.addHtml5HelpItem(R.id.activity_main, "html5/Introduction5/Introduction5.html")
        mPopupHelpUtils?.addHtml5HelpItem(R.id.activity_main, "html5/MainScreen2/Main Screen.html")

//        mPopupHelpUtils?.addFullScreenHelpItem(R.id.activity_main, R.drawable.intro1_welcome)
//        mPopupHelpUtils?.addFullScreenHelpItem(R.id.activity_main, R.drawable.intro2_translate_revise)
//        mPopupHelpUtils?.addFullScreenHelpItem(R.id.activity_main, R.drawable.intro3_review)
//        mPopupHelpUtils?.addFullScreenHelpItem(R.id.activity_main, R.drawable.intro4_create_vid)
//        mPopupHelpUtils?.addFullScreenHelpItem(R.id.activity_main, R.drawable.intro5_share)

        mPopupHelpUtils?.addPopupHelpItem(
            R.id.activity_main,
            -1, -1,
            R.string.welcome_to_story_producer, R.string.help_main_welcome_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.activity_main,
            -1, -1,
            R.string.help_main_drag_title, R.string.help_main_drag_body)
        mPopupHelpUtils?.addPopupHelpItem(
                R.id.toolbar,
                88, 80,
                R.string.help_main_help_title, R.string.help_main_help_body)
        /**mPopupHelpUtils?.addPopupHelpItem(
                R.id.activity_main,
                -1, -1,
                R.string.help_welcome_title2, R.string.help_welcome_body3)
        **/
        mPopupHelpUtils?.addPopupHelpItem(
                R.id.activity_main,
                -1, -1,
                R.string.help_main_guidance_title, R.string.help_main_guidance_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.story_list_view,
            -1, -1,
            R.string.help_main_screen_title, R.string.help_main_screen_body)
        /**mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            12, 80,
            R.string.help_main_menus_settings_title, R.string.help_main_menus_settings_body)
        **/
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            12, 80,
            R.string.help_main_downloads_title, R.string.help_main_downloads_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.story_list_view,
            50, -storyListViewPercent,  // negative percentY is a hint to point north
            R.string.help_main_story_title, R.string.help_main_story_body)

        mPopupHelpUtils?.showNextPopupHelp(R.id.activity_main)  // specify an anchor view id  [bugfix 847]
    }

    // If only one or two stories are (auto) installed then display short
    // message to user to explain how to download more bloom templates
    override fun checkDownloadStoriesMessage() {
        super.checkDownloadStoriesMessage()
        if (Workspace.storyFilesToScanOrUnzipOrMove().size <= 3) {
            if (mPopupHelpUtils == null || mPopupHelpUtils?.isShowingPopupWindow() != true) {
                SnackbarManager.show(
                    findViewById(R.id.drawer_layout),
                    getString(R.string.more_story_templates),
                    20 * 1000,   // display for 20 seconds
                    6
                )
            }
        } else if (Workspace.hasFilterToolbar()) {
            if (globalChipMsgCount < 2) {
                SnackbarManager.show(
                    findViewById(R.id.drawer_layout),
                    getString(R.string.filter_chips_feature),
                    20 * 1000,   // display for 20 seconds
                    7
                )
                globalChipMsgCount++
            }
        }
    }

    override fun onResume() {
        super.onResume()

        addAndStartPopupTutorials()

        checkDownloadStoriesMessage()
    }

    override fun onStart() {

        super.onStart()

        if (mDrawerList == null) {
            setupDrawer()
            setupStoryListTabPages()
        } else {
            setupStoryListTabPages()    // Updates any 'in-progress' story markers
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_with_help, menu)
        return true
    }

    /**
     * move to the chosen story
     */
    fun switchToStory(story: Story) {
        Workspace.activeStory = story
        val intent = Intent(this.applicationContext, Workspace.activePhase.getTheClass())
        startActivity(intent)
//        finish()  // removed to keep back button working on MainActivity
    }
    fun deleteStory(story: Story) {
        AlertDialog.Builder(this)
            .setTitle(R.string.del_story_title)
            .setMessage(getString(R.string.del_story_message, story.localTitle))
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteWorkspaceFile(this.applicationContext, story.title)
                controller.updateStories()  // refresh list of stories
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    override fun showDetailedHelp() {
        val wv = WebView(this)
        val iStream = assets.open(Phase.getHelpDocFile(PhaseType.STORY_LIST))
        val text = iStream.reader().use {
            it.readText() }

        wv.loadDataWithBaseURL(null, text,"text/html",null,null)
        val dialog = AlertDialog.Builder(this)
            .setTitle(baseContext.getString(R.string.title_activity_story_templates) + " " + baseContext.getString(R.string.help))
            .setView(wv)
            .setNegativeButton(baseContext.getString(R.string.nav_close)) { dialog, _ ->
                dialog!!.dismiss()
            }
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if(mDrawerLayout!!.isDrawerOpen(GravityCompat.START)){
                    mDrawerLayout!!.closeDrawer(GravityCompat.START)
                }else{
                    mDrawerLayout!!.openDrawer(GravityCompat.START)
                }
                true
            }
            R.id.helpButton -> {
                // Show the help context menu
                showHelpContextMenu()
                true
            }
//            R.id.helpResumeButton -> {
//                resumeShowingPopupHelp()
//                checkDownloadStoriesMessage()
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storyPageViewPager.unregisterOnPageChangeCallback(storyPageChangeCallback)
    }

    var storyPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Log.i("MainActivity Story Page", "Selected Tab: $position")
        }
    }

    fun setupStoryListTabPages() {
        storyPageViewPager = findViewById(R.id.storyPageViewPager)
        storyPageViewPager.offscreenPageLimit = StoryPageTab.values().size
        storyPageTabLayout = findViewById(R.id.tabLayout)

        val storyPageAdapter = StoryPageAdapter(this, StoryPageTab.values().size)
        storyPageViewPager.adapter = storyPageAdapter

        storyPageViewPager.registerOnPageChangeCallback(storyPageChangeCallback)

        // Sets the Tab Names from the list of StoryPageTabs
        TabLayoutMediator(storyPageTabLayout, storyPageViewPager) { tab, position ->
            tab.text = getString(StoryPageTab.values()[position].nameId)
        }.attach()
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(baseContext.getString(R.string.exit_application))
                .setMessage(baseContext.getString(R.string.exit_are_you_sure))
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    finishAffinity()
                    exitProcess(0)  // exit fully
                }.create()
        dialog.show()
    }

}


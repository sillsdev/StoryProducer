package org.tyndalebt.spadv.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.activities.BaseActivity
import org.tyndalebt.spadv.controller.storylist.StoryPageAdapter
import org.tyndalebt.spadv.controller.storylist.StoryPageTab
import org.tyndalebt.spadv.model.Phase
import org.tyndalebt.spadv.model.PhaseType
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.tools.Network.ConnectivityStatus
import org.tyndalebt.spadv.tools.Network.VolleySingleton
import java.io.Serializable

class MainActivity : BaseActivity(), Serializable {

    private var mDrawerLayout: DrawerLayout? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setupDrawer()
        setupStoryListTabPages()


        if (!Workspace.isInitialized) {
            initWorkspace()
        }

        if (Workspace.showRegistration) {
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
                this@MainActivity.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout!!.openDrawer(GravityCompat.START)
                true
            }
            R.id.helpButton -> {

                val wv = WebView(this)
                val iStream = assets.open(Phase.getHelpDocFile(PhaseType.STORY_LIST))
                val text = iStream.reader().use {
                        it.readText() }

                wv.loadDataWithBaseURL(null,text,"text/html",null,null)
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Story List Help")
                    .setView(wv)
                    .setNegativeButton("Close") { dialog, _ ->
                        dialog!!.dismiss()
                    }
                dialog.show()
                true
            }
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

    private fun setupStoryListTabPages() {
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

    /**
     * initializes the items that the drawer needs
     */
    private fun setupDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(::onNavigationItemSelected)
    }

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        mDrawerLayout?.closeDrawers()

        when (menuItem.itemId) {
            R.id.nav_workspace -> {
                showSelectTemplatesFolderDialog()
            }
            R.id.nav_demo -> {
                Workspace.addDemoToWorkspace(this)

                // Refresh all the stories in the story page tabs
                setupStoryListTabPages()
            }
            R.id.nav_word_link_list -> {
                showWordLinksList()
            }
            R.id.nav_more_templates -> {
                // DKH - 01/15/2022 Issue #571: Add a menu item for accessing templates from Google Drive
                // A new menu item was added that opens a URL for the user to download templates.
                // If we get here, the user wants to browse for more templates, so,
                // open the URL in a new activity
                Workspace.startDownLoadMoreTemplatesActivity(this)
            }
            R.id.nav_stories -> {
                // Current fragment
            }
            R.id.nav_registration -> {
                // DKH - 05/10/2021 Issue 573: SP will hang/crash when submitting registration
                // The MainActivity thread is responsible for displaying  story templates
                // and allowing the user to select  a registration update via this menu option.
                // So, when calling the RegistrationActivity from the MainActivity, specify that
                // finish should not be called.  This is done by setting executeFinishActivity to false.
                // After the RegistrationActivity is complete, MainActivity will then display
                // the story template list
                showRegistration(false)
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
        }

        return true
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
                .setTitle("Exit Application?")
                .setMessage("Are you sure you want to quit?")
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                }.create()
        dialog.show()
    }

}


package org.sil.storyproducer.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.sil.storyproducer.R
import org.sil.storyproducer.activity.BaseActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.ConnectivityStatus
import org.sil.storyproducer.tools.Network.VolleySingleton
import java.io.Serializable

class MainActivity : BaseActivity(), Serializable {
    private var mDrawerLayout: DrawerLayout? = null

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

        //Display a "progress bar" while loading all the template files.  Remove it when your done.
        val pb = findViewById<ProgressBar>(R.id.indeterminateBar)
        pb.visibility = View.VISIBLE

        GlobalScope.launch {
            if(!Workspace.isInitialized)
                Workspace.initializeWorskpace(this@MainActivity)
            runOnUiThread {
                pb.visibility = View.GONE
                supportFragmentManager.beginTransaction().add(R.id.fragment_container, StoryListFrag()).commit()
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout!!.openDrawer(GravityCompat.START)
                true
            }
            R.id.helpButton -> {

                val wv = WebView(this)
                val iStream = assets.open(Phase.getHelpName(PhaseType.STORY_LIST))
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
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout!!.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here
            val intent: Intent
            when (menuItem.itemId) {
                R.id.nav_workspace -> {
                    showSelectTemplatesFolderDialog()
                }
                R.id.nav_demo -> {
                    Workspace.addDemoToWorkspace(this)
                }

                R.id.nav_stories -> {
                    // Current fragment
                }
                R.id.nav_registration -> {
                    intent = Intent(this, RegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_license -> {
                    val version = packageManager.getPackageInfo(packageName, 0).versionName
                    val message = getString(R.string.license_body)
                    val dialog = AlertDialog.Builder(this)
                            .setTitle(getString(R.string.license_title))
                            .setMessage("version: $version\n\n$message")
                            .setPositiveButton(getString(R.string.ok)) { _, _ -> }.create()
                    dialog.show()
                }
            }

            true
        }
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

    private fun showSelectTemplatesFolderDialog() {
        AlertDialog.Builder(this)
                .setTitle(buildSelectTemplatesTitle())
                .setMessage(buildSelectTemplatesMessage())
                .setPositiveButton(R.string.ok) { _, _ -> updateTemplatesFolder() }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    private fun buildSelectTemplatesTitle(): Spanned {
        val title = "<b>${getString(R.string.update_workspace)}</b>"
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(title,0)
        } else {
            Html.fromHtml(title) }
    }

    private fun buildSelectTemplatesMessage(): Spanned {
        val message = getString(R.string.workspace_selection_help)
        return if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(message, 0)
        } else {
            Html.fromHtml(message) }
    }

}


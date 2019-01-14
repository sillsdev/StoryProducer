package org.sil.storyproducer.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.KeyTermListActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.ConnectivityStatus
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.StorySharedPreferences
import java.io.Serializable


class MainActivity : AppCompatActivity(), Serializable {
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

        StorySharedPreferences.init(applicationContext)

        setContentView(R.layout.activity_main)
        setupDrawer()

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, StoryListFrag()).commit()

        this.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        Workspace.updateStories(this)
        Workspace.importKeyterms(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_story_templates, menu)
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
                val alert = AlertDialog.Builder(this)
                alert.setTitle("Story List Help")

                val wv = WebView(this)
                val iStream = assets.open(Phase.getHelpName(PhaseType.STORY_LIST))
                val text = iStream.reader().use {
                        it.readText() }

                wv.loadData(text,"text/html",null)
                alert.setView(wv)
                alert.setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
                }
                alert.show()
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
                    intent = Intent(this, WorkspaceAndRegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_stories -> {
//                    intent = Intent(this.applicationContext, MainActivity::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    this.startActivity(intent)
//                    this.finish()
                }
                R.id.nav_keyterm_list -> {
                    intent = Intent(this, KeyTermListActivity::class.java)
                    this.startActivity(intent)
                }
                R.id.nav_registration -> {
                    intent = Intent(this, RegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_license -> {
                    val dialog = AlertDialog.Builder(this)
                            .setTitle(this.getString(R.string.license_title))
                            .setMessage(this.getString(R.string.license_body))
                            .setPositiveButton(this.getString(R.string.ok)) { _, _ -> }.create()
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
}


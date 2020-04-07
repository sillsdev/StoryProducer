package org.sil.storyproducer.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.Network.ConnectivityStatus
import org.sil.storyproducer.tools.Network.VolleySingleton
import java.io.Serializable

fun handleDrawerItemSelection(activity: Activity, menuItem: MenuItem, drawerLayout: DrawerLayout, currentItemId: Int?): Boolean {
    menuItem.isChecked = true
    drawerLayout.closeDrawers()

    if (currentItemId == menuItem.itemId) {
        return true
    }

    when (menuItem.itemId) {
        R.id.nav_workspace -> {
            val intent = Intent(activity, WorkspaceUpdateActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
        R.id.nav_stories -> {
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
        R.id.nav_registration -> {
            val intent = Intent(activity, RegistrationActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
        R.id.nav_license -> {
            val dialog = AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.license_title))
                    .setMessage(activity.getString(R.string.license_body))
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ -> }.create()
            dialog.show()
        }
        R.id.nav_set_rocc_url_prefix -> {
            val container = LinearLayout(activity)
            container.orientation = LinearLayout.VERTICAL
            container.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            val roccPrefixInput = EditText(activity)
            roccPrefixInput.setText(PreferenceManager.getDefaultSharedPreferences(activity).getString("ROCC_URL_PREFIX", "")
                    ?: "")
            container.addView(roccPrefixInput,
                    LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT))
            val webSocketsInput = EditText(activity)
            container.addView(webSocketsInput,
                    LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT))
            webSocketsInput.setText(PreferenceManager.getDefaultSharedPreferences(activity).getString("WEBSOCKETS_URL", "")
                    ?: "")
            val dialog = AlertDialog.Builder(activity)
                    .setTitle("ROCC URL Prefix")
                    .setMessage("Enter a string to prefix all requests to the remote consultant site")
                    .setView(container)
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                                .putString("ROCC_URL_PREFIX", roccPrefixInput.text.toString())
                                .putString("WEBSOCKETS_URL", webSocketsInput.text.toString()).apply()
                    }.create()
            dialog.show()
        }
        R.id.nav_clear_all_messages -> {
        }
        R.id.nav_forget_remote_story_id -> {
            Workspace.activeStory.remoteId = null
        }
    }

    return true
}

class MainActivity : AppCompatActivity(), Serializable {
    private lateinit var mDrawerLayout: DrawerLayout

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

        GlobalScope.async {
            if (!Workspace.isInitialized) Workspace.initializeWorkspace(this@MainActivity.applicationContext)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.helpButton -> {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("Story List Help")

                val wv = WebView(this)
                val iStream = assets.open("story_list.html")
                val text = iStream.reader().use {
                    it.readText()
                }

                wv.loadData(text, "text/html", null)
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
        val actionbar: ActionBar = supportActionBar!!
        actionbar.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val drawerLayout = mDrawerLayout
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerItemSelection(this, menuItem, drawerLayout, R.id.nav_stories)
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


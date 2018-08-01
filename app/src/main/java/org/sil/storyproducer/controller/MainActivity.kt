package org.sil.storyproducer.controller

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.tools.Network.ConnectivityStatus
import org.sil.storyproducer.tools.Network.VolleySingleton
import org.sil.storyproducer.tools.StorySharedPreferences

import org.sil.storyproducer.model.Workspace


import java.io.Serializable

import java.security.AccessController.getContext
import org.sil.storyproducer.controller.remote.RemoteCheckFrag.R_CONSULTANT_PREFS


class MainActivity : AppCompatActivity(), Serializable {

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
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, StoryListFrag()).commit()

        this.applicationContext.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        Workspace.updateStories(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_story_templates, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.menu_lang -> {
            }
            R.id.menu_registration -> {
                val intent = Intent(this@MainActivity, RegistrationActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_license -> {
                val dialog = AlertDialog.Builder(this)
                        .setTitle(this.getString(R.string.license_title))
                        .setMessage(this.getString(R.string.license_body))
                        .setPositiveButton(this.getString(R.string.ok)) { dialog, id -> }.create()
                dialog.show()
            }
        }//TODO remove this option.
        return super.onOptionsItemSelected(item)
    }

    /**
     * Upon language change, reload list of templates in that language
     * The actual language change is done within the FileSystem class
     */
    private fun reloadStories() {
        supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentById(R.id.fragment_container)).commit()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, StoryListFrag()).commit()

    }

    /**
     * move to the chosen story
     */
    fun switchToStory(story: Story) {
        Workspace.activeStory = story
        val intent = Intent(this.applicationContext, Workspace.activePhase.getTheClass())
        startActivity(intent)
    }
}


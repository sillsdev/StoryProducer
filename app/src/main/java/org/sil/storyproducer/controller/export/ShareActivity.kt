package org.sil.storyproducer.controller.export

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.StoryState
import org.sil.storyproducer.tools.StorySharedPreferences

import java.io.File
import java.util.ArrayList


/**
 * Created by annmcostantino on 10/1/2017.
 */
//TODO: Cleanup all the useless stuff here
class ShareActivity : PhaseBaseActivity() {

    private var mShareSection: LinearLayout? = null
    private var mNoVideosText: TextView? = null
    private var mVideosListView: ListView? = null

    private var videosAdapter: ExportedVideosAdapter? = null
    private var mStory: String? = null


    //accordion variables
    private val sectionIds = intArrayOf(R.id.share_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    private//make sure the file actually exists
    //If the file doesn't exist or we encountered it a second time in the list, remove it.
    val exportedVideosForStory: List<String>
        get() {
            val actualPaths = ArrayList<String>()
            val videoPaths = StorySharedPreferences.getExportedVideosForStory(mStory)
            for (path in videoPaths) {
                val file = File(path)
                if (file.exists() && !actualPaths.contains(path)) {
                    actualPaths.add(path)
                } else {
                    StorySharedPreferences.removeExportedVideoForStory(path, mStory)
                }
            }
            return actualPaths
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mStory = StoryState.getStoryName()     //needs to be set first because some of the views use it
        val phaseUnlocked = StorySharedPreferences.isApproved(mStory, this)
        setContentView(R.layout.activity_share)
        mStory = StoryState.getStoryName()
        setupViews()
        invalidateOptionsMenu()
        if (phaseUnlocked) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
        loadPreferences()
    }

    override fun onDestroy() {
        savePreferences()

        super.onDestroy()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.getItem(0)
        item.setIcon(R.drawable.ic_share)
        return true
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    //TODO: cleanup
    private fun setupViews() {

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = findViewById(sectionIds[i])
        }


        //share view
        mShareSection = findViewById(R.id.share_section)
        videosAdapter = ExportedVideosAdapter(this)
        mVideosListView = findViewById(R.id.videos_list)
        mVideosListView!!.adapter = videosAdapter
        mNoVideosText = findViewById(R.id.no_videos_text)
        setVideoAdapterPaths()

    }

    /**
     * sets the videos for the list adapter
     */
    private fun setVideoAdapterPaths() {
        val actualPaths = exportedVideosForStory
        if (actualPaths.size > 0) {
            mNoVideosText!!.visibility = View.GONE
        }
        videosAdapter!!.setVideoPaths(actualPaths)
    }

    /**
     * Save current configuration options to shared preferences.
     */
    //TODO PROB DONT NEED THESE ANYMORE
    private fun savePreferences() {
        val editor = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
        editor.apply()
    }

    /**
     * Load configuration options from shared preferences.
     */
    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    companion object {

        private val PREF_FILE = "Share_Config"
    }


}

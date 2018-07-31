package org.sil.storyproducer.controller.export

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.support.v4.provider.DocumentFile
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.getChildDocuments
import org.sil.storyproducer.tools.file.getStoryUri


/**
 * Created by annmcostantino on 10/1/2017.
 */
//TODO: Cleanup all the useless stuff here
class ShareActivity : PhaseBaseActivity() {

    private var mShareSection: LinearLayout? = null
    private var mNoVideosText: TextView? = null
    private var mVideosListView: ListView? = null

    private var videosAdapter: ExportedVideosAdapter? = null


    //accordion variables
    private val sectionIds = intArrayOf(R.id.share_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phaseUnlocked = StorySharedPreferences.isApproved(Workspace.activeStory.title, this)
        setContentView(R.layout.activity_share)
        setupViews()
        invalidateOptionsMenu()
        if (phaseUnlocked) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
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
        mVideosListView = findViewById(R.id.videos_list)!!
        mVideosListView!!.adapter = videosAdapter
        mNoVideosText = findViewById(R.id.no_videos_text)

        val exportedVideos = getChildDocuments(this,"${Workspace.activeStory.title}/$VIDEO_DIR")
        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)
    }
}

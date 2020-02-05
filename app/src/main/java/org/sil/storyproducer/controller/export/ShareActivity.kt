package org.sil.storyproducer.controller.export

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getChildDocuments


/**
 * Created by annmcostantino on 10/1/2017.
 */
//TODO: Cleanup all the useless stuff here
class ShareFragment : Fragment(), RefreshViewListener {

    private var mShareSection: LinearLayout? = null
    private var mNoVideosText: TextView? = null
    private var mVideosListView: ListView? = null

    private var videosAdapter: ExportedVideosAdapter? = null


    //accordion variables
    private val sectionIds = intArrayOf(R.id.share_section)
    private val sectionViews = arrayOfNulls<View>(sectionIds.size)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.activity_share, container, false)
        if (Workspace.activeStory.isApproved) {
            rootView.findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = rootView.findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }

        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = rootView.findViewById(sectionIds[i])
        }

        //share view
        mShareSection = rootView.findViewById(R.id.share_section)
        videosAdapter = ExportedVideosAdapter(context!!, this)
        mVideosListView = rootView.findViewById(R.id.videos_list)!!
        mVideosListView!!.adapter = videosAdapter
        mNoVideosText = rootView.findViewById(R.id.no_videos_text)

        val presentVideos = getChildDocuments(context!!, VIDEO_DIR)
        val exportedVideos: MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size) {
            if (presentVideos[i] in Workspace.activeStory.outputVideos) {
                exportedVideos.add(presentVideos[i])
            }
        }
        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)
        activity!!.runOnUiThread {
            //This allows the video file to write if it just did
            val handler = Handler()
            handler.postDelayed({
                refreshViews()
            }, 3000)
        }

        return rootView
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    //TODO: cleanup
    override fun refreshViews() {

        if (context == null) {
            return
        }

        val presentVideos = getChildDocuments(context!!, VIDEO_DIR)
        val exportedVideos: MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size) {
            if (presentVideos[i] in Workspace.activeStory.outputVideos) {
                exportedVideos.add(presentVideos[i])
            }
        }
        //If the file has been deleted, remove it.
        val toRemove = mutableListOf<Int>()
        for (i in 0 until Workspace.activeStory.outputVideos.size) {
            if (Workspace.activeStory.outputVideos[i] !in presentVideos) {
                toRemove.add(0, i) //add at beginning
            }
        }
        for (i in toRemove) {
            Workspace.activeStory.outputVideos.removeAt(i)
        }
        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        } else {
            mNoVideosText!!.visibility = View.VISIBLE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)
    }
}

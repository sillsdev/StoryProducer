package org.sil.storyproducer.controller.export

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.UriUtils
import org.sil.storyproducer.tools.file.getChildDocuments
import org.sil.storyproducer.tools.file.getWorkspaceUri


/**
 * Created by annmcostantino on 10/1/2017.
 */
//TODO: Cleanup all the useless stuff here
class ShareActivity : PhaseBaseActivity(), RefreshViewListener {

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
        setContentView(R.layout.activity_share)
        invalidateOptionsMenu()
        if (Workspace.activeStory.isApproved) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
        initView()
        runOnUiThread{
            //This allows the video file to write if it just did
            val handler = Handler()
            handler.postDelayed({
                refreshViews()
                //your code here
            }, 3000)
        }
    }

    private fun initView() {
        //Initialize sectionViews[] with the integer id's of the various LinearLayouts
        //Add the listeners to the LinearLayouts's header section.
        for (i in sectionIds.indices) {
            sectionViews[i] = findViewById(sectionIds[i])
        }

        //share view
        mShareSection = findViewById(R.id.share_section)
        videosAdapter = ExportedVideosAdapter(this, this)
        mVideosListView = findViewById(R.id.videos_list)!!
        mVideosListView!!.adapter = videosAdapter
        mNoVideosText = findViewById(R.id.no_videos_text)
        var mOpenVideoPath : Button = findViewById(R.id.open_videos_path)

        val presentVideos = getChildDocuments(this, VIDEO_DIR)
        val exportedVideos : MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size){
            if(presentVideos[i] in story.outputVideos){
                exportedVideos.add(presentVideos[i])
            }
        }
        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)

        mOpenVideoPath.setOnClickListener {

            val videoContentUri  = getWorkspaceUri("$VIDEO_DIR/")
            var videoFileUriStr = UriUtils.getPathFromUri(this, videoContentUri!!)
            var videoFileUri = Uri.parse(videoFileUriStr)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Using a file manager app, video files can be accessed at: ${videoFileUri.path}")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    /**
     * Get handles to all necessary views and add some listeners.
     */
    //TODO: cleanup
    override fun refreshViews() {

        val presentVideos = getChildDocuments(this, VIDEO_DIR)
        val exportedVideos : MutableList<String> = ArrayList()
        for (i in 0 until presentVideos.size){
            if(presentVideos[i] in story.outputVideos){
                exportedVideos.add(presentVideos[i])
            }
        }
        //If the file has been deleted, remove it.
        val toRemove = mutableListOf<Int>()
        for (i in 0 until story.outputVideos.size){
            if(story.outputVideos[i] !in presentVideos){
                toRemove.add(0, i) //add at beginning
            }
        }
        for (i in toRemove){
            story.outputVideos.removeAt(i)
        }
        if (exportedVideos.isNotEmpty()) {
            mNoVideosText!!.visibility = View.GONE
        }else{
            mNoVideosText!!.visibility = View.VISIBLE
        }
        videosAdapter!!.setVideoPaths(exportedVideos)
    }

}

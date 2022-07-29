package org.tyndalebt.spadv.controller.export

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.phase.PhaseBaseActivity
import org.tyndalebt.spadv.model.VIDEO_DIR
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.tools.file.UriUtils
import org.tyndalebt.spadv.tools.file.getChildDocuments
import org.tyndalebt.spadv.tools.file.getWorkspaceUri


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

            // At this point the videoFileUriStr will look something like this: /storage/emulated/0/
            // This is the actual path. However, it needs be changed to the SD Card (/sdcard/)
            // which is a symbolic link to the emulated storage path.
            // sdcard/: Is a symlink to...
            //      /storage/sdcard0 (Android 4.0+)
            // In Story Producer, the version will never be less than Android 4.0
            // We will instead show it as an optional [sdcard]
            // The below code will change: /storage/emulated/0/ to /storage/[sdcard]/
            videoFileUriStr = videoFileUriStr.replace(Regex("(storage\\/emulated\\/)\\d+"), "storage/[sdcard]")

            // Also, the SD-Card could show up as /storage/####-####/ where # is a hexidecimal value
            videoFileUriStr = videoFileUriStr.replace(Regex("(storage)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"), "storage/[sdcard]")

            var videoFileUri = Uri.parse(videoFileUriStr)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("${getString(R.string.view_video_folder_message)} ${videoFileUri.path}")
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

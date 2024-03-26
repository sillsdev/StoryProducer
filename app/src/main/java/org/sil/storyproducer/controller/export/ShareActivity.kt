package org.sil.storyproducer.controller.export

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.storage.StorageManager
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.hbisoft.pickit.Utils
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.controller.storylist.PopupHelpUtils
import org.sil.storyproducer.model.VIDEO_DIR
import org.sil.storyproducer.model.Workspace
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

        mOpenVideoPath.isEnabled = presentVideos.isNotEmpty() // disable video path button if no videos or folder
        mOpenVideoPath.setOnClickListener {

            var videoContentUri  = getWorkspaceUri("$VIDEO_DIR/") // 27A2-3AB6
//            var videoFileUriStr = UriUtils.getPathFromUri(this, videoContentUri!!)    // this old library call does not work for all cases
            var videoFileUriStr = Utils.getRealPathFromURI_API19(this, videoContentUri!!)  // using another third party library to get file path
            if (videoFileUriStr != null && videoFileUriStr.isNotEmpty()) {
                // At this point the videoFileUriStr will look something like this: /storage/emulated/0/ or
                // /storage/####-####/ where # is a hexadecimal value, this is the actual path.
                // However, it needs be changed to [Internal Storage]/... or [SD Card] or [USB Drive] etc.
                // This is so that the user can use a file manager app to easily locate the folder there
                val internalStorageRoot = "/storage/emulated/0"
                if (videoFileUriStr.startsWith(internalStorageRoot)) {
                    // The below code will change: /storage/emulated/0/ to [Internal Storage]/
                    videoFileUriStr = videoFileUriStr.replaceFirst(internalStorageRoot,
                        "[${getString(R.string.view_video_folder_internal_storage)}]")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Also, the SD-Card/USB-Drive could show up as /storage/####-####/ where # is a hexadecimal value
                    // Here we can use the storage service to get the short description of the external drive
                    var hexStorageRoot = ""
                    val regexStorageVol = "^\\/storage\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
                    if (Regex(regexStorageVol).find(videoFileUriStr) != null) {
                        hexStorageRoot = videoFileUriStr.substring(9, 18) // e.g. /storage/1234-5678
                    }
                    if (hexStorageRoot.isNotEmpty()) {
                        // we have found a external drive path and hex value, so now find its description
                        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
                        for (storageVolume in storageManager.storageVolumes) {
                            if (!storageVolume.isPrimary) {
                                val storageVolToString = storageVolume.toString()
                                if (storageVolToString.contains(hexStorageRoot)) {
                                    // Finding the hex root in the storage volume string seems to be the
                                    // most backwards compatible way of finding the matching volume
                                    val driveDesc = storageVolume.getDescription(this)
                                    if (driveDesc != null) {
                                        // we now have a volume/drive description so use it
                                        videoFileUriStr = videoFileUriStr.replace(
                                            Regex(regexStorageVol),"[$driveDesc]")
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
                // now display an alert dialog with a user friendly videos path in it
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setMessage("${getString(R.string.view_video_folder_message)}\n${videoFileUriStr}")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> }
                val alert: AlertDialog = builder.create()
                alert.show()
            }
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

    override fun onResume() {
        super.onResume()

        addAndStartPopupMenus()
    }

    private fun addAndStartPopupMenus() {

        if (mPopupHelpUtils != null)
            mPopupHelpUtils?.dismissPopup()

        mPopupHelpUtils = PopupHelpUtils(this)

        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            50, 75,
            R.string.help_share_phase_title, R.string.help_share_phase_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.videos_list,
            5, 5,
            R.string.help_share_play_video_title, R.string.help_share_play_video_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.videos_list,
            15, 5,
            R.string.help_share_share_video_title, R.string.help_share_share_video_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.videos_list,
            75, 5,
            R.string.help_share_name_video_title, R.string.help_share_name_video_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.videos_list,
            90, 5,
            R.string.help_share_ext_video_title, R.string.help_share_ext_video_body)
        mPopupHelpUtils?.addPopupHelpItem(
            R.id.toolbar,
            10, 50,
            R.string.help_share_story_list_title, R.string.help_share_story_list_body)

        mPopupHelpUtils?.showNextPopupHelp()

    }

}

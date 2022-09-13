package org.sil.storyproducer.controller.bldownload

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.databinding.ActivityBldownloadBinding
import org.sil.storyproducer.model.BLBook
import org.sil.storyproducer.model.bloomSourceAutoDLDir
import org.sil.storyproducer.model.bloomSourceZipExt
import org.sil.storyproducer.model.parseOPDSfile
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import java.io.File

// This class implements the code for the BLDownloadActivity which shows the UI to download a story
// automatically from the Bloom Library.  This UI consists of a list of CardView widgets each
// detailing an individual story available for download
class BLDownloadActivity : AppCompatActivity() {

    // based on the example: https://www.tutorialspoint.com/how-to-register-a-broadcast-receiver-programmatically-in-android-using-kotlin
    // This sub class will act as a receiver for a message send by the DownloadManager used to download
    // stories in the background
    class BLBroadCastReceiver : BroadcastReceiver() {
        companion object {
            private val TAG = "BLDownloadActivity.BLBroadCastReceiver"
        }

        // process a message from the DownloadManager to see if it has completed one of our story files
        override fun onReceive(context: Context?, intent: Intent?) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent!!.action) {
                var moreDownloads = 0;
                val downloadedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, BLDataModel.NOT_DOWNLOADED)
                for (i in 0 until data.size) {
                    val model = data.get(i)
                    if (model.downloadId != BLDataModel.NOT_DOWNLOADED && model.downloadId == downloadedId) {
                        model.downloadId = BLDataModel.ALREADY_DOWNLOADED   // no longer need to check for this download id
                    }
                    else if (model.downloadId != BLDataModel.NOT_DOWNLOADED &&
                        model.downloadId != BLDataModel.ALREADY_DOWNLOADED) {
                        moreDownloads++ // we have other ids that are still being downloaded
                    }
                }
                if (moreDownloads == 0) {
                    // Show download complete toast message
                    Toast.makeText(context, R.string.bloom_lib_download_complete, Toast.LENGTH_LONG).show()
                    // close BL download activity so we can initiate and see the templates being processed by the MainActivity
                    // this onBackPressed() override also unregisters the broadcast receiver and calls updateStories()
                    bldlActivity.onBackPressed()
                }
            }
        }
    }

    companion object {
        private val TAG = "BLDownloadActivity"

        lateinit var bldlActivity: BLDownloadActivity
        lateinit var adapter: RecyclerView.Adapter<BLCustomAdapter.BLViewHolder?>
        var data: ArrayList<BLDataModel> = ArrayList()
        lateinit var recyclerView: RecyclerView
        lateinit var downloadManager: DownloadManager
    }

    lateinit var blOnClickListener: BLOnClickListener
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var binding: ActivityBldownloadBinding
    private val blBroadCastReceiver: BroadcastReceiver = BLBroadCastReceiver()
    private var blReceiverRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bldlActivity = this

        binding = ActivityBldownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        supportActionBar?.setDisplayShowHomeEnabled(true);
        binding.toolbar.title = title

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager;

        // FAB Icon from:
        // <a href="https://www.flaticon.com/free-icons/download" title="download icons">Download icons created by Becris - Flaticon</a>
        // Now setup a listener, for the BL Download cloud FAB button click, here using a lambda
        binding.bldlFab.setOnClickListener { view ->
            var numQueued = 0
            var numAlreadyDownloaded = 0
            for (i in 0 until data.size)
            {
                var dataItem = data.get(i)
                if (dataItem.isChecked &&
                        !dataItem.isInWorkspace &&
                        dataItem.downloadId == BLDataModel.NOT_DOWNLOADED)
                {   // user is initiating a download of this title from bloom library
                    val request = DownloadManager.Request(Uri.parse(dataItem.downloadUri))
                    request.setTitle(dataItem.title)
                    request.setDescription(getString(R.string.bloom_lib_download))
                    request.setDestinationInExternalFilesDir(this,
                            Environment.DIRECTORY_DOWNLOADS, dataItem.title + bloomSourceZipExt())
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    // queue the download
                    dataItem.downloadId = downloadManager.enqueue(request);
                    numQueued++
                }
                else if (dataItem.isChecked &&
                        dataItem.isInWorkspace &&
                        dataItem.downloadId == BLDataModel.ALREADY_DOWNLOADED)
                {   // download of this title has already been completed
                    numAlreadyDownloaded++
                }
            }

            if (numQueued > 0) {
                Snackbar.make(view, getString(R.string.bloom_lib_download_started), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            else if (numAlreadyDownloaded > 0) {
                Toast.makeText(this, getString(R.string.bloom_lib_already_downloaded),
                    Toast.LENGTH_LONG).show()
                bldlActivity.unregisterBLBroadcastReceiver(blBroadCastReceiver) // bug fix for crash in updateStories()
                // close BL download activity so we can see the templates being processed by the MainActivity
                // this onBackPressed() override also unregisters the broadcast receiver and calls updateStories()
                bldlActivity.onBackPressed()
            }
        }   // FAB binding.bldlFab.setOnClickListener() lambda ends here

        // Now continue with OnCreate() implementation

        // now follows code adapted from:
        // https://www.digitalocean.com/community/tutorials/android-recyclerview-android-cardview-example-tutorial
        recyclerView = binding.bldlCardRecyclerView
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()

        val fileDownloadDir = bloomSourceAutoDLDir()    // get download folder for checking if already downloaded

        // Get the definitive list of available Bloom Library story templates
        val blDataList = parseOPDSfile();

        // Find or add BLDataModel items to our data list and update any members for the known state
        var lastItemCheckedTitle = blFindOrAddBookItems(blDataList, fileDownloadDir)
        var lastItemClicked = -1    // last checked item - used for toggling
        if (lastItemCheckedTitle.isNotEmpty()) {
            lastItemClicked = data.indexOfFirst { it -> it.title == lastItemCheckedTitle }
        }
        // listen for clicks to check/uncheck item
        blOnClickListener = BLOnClickListener(this, lastItemClicked)

        adapter = BLCustomAdapter(data)
        recyclerView.setAdapter(adapter)
    }

    private fun blFindOrAddBookItems(
        blDataList: MutableList<BLBook>,
        fileDownloadDir: String
    ): String {
        var lastItemCheckedTitle = ""   // for automatically unchecking items if more than one clicked on
        for (i in 0 until blDataList.size) {
            val blItem = blDataList[i]
            // Check if the file has already been downloaded
            val isFileDownloaded =
                File(fileDownloadDir + "/" + blItem.Title + bloomSourceZipExt()).exists()
            // Check if the title is already in the workspace
            val isInWorkspace = isFileDownloaded or workspaceRelPathExists(this, blItem.Title)
            val blItemFound = data.find { it -> it.title == blItem.Title }
            if (blItemFound != null) {
                if (isFileDownloaded)
                    blItemFound.isChecked = true    // downloaded and ready for installing
                else if (isInWorkspace)
                    blItemFound.isChecked = false   // already installed
                blItemFound.isInWorkspace = isInWorkspace
                if (isFileDownloaded)
                    blItemFound.downloadId =
                        BLDataModel.ALREADY_DOWNLOADED // is file was downloaded outside of the BLDownloadActivity running
                if (blItemFound.isChecked && !blItemFound.isInWorkspace)
                    lastItemCheckedTitle =
                        blItemFound.title    // make a note in case we need to uncheck it
            } else {   // we need to add this title to out data list
                data.add(
                    BLDataModel(
                        blItem.Title,
                        blItem.LangCode,
                        if (blItem.Title == "001 The Widow’s Offering")
                            R.drawable.temp_001_widdows_offering_thumbnail // TODO: example image - just testing appearance
                        else
                            R.drawable.temp_sp_logo_book,
                        blItem.BloomSourceURL,
                        isInWorkspace,      // makes it grayed out
                        isFileDownloaded,   // makes it checked
                        if (isFileDownloaded) BLDataModel.ALREADY_DOWNLOADED else BLDataModel.NOT_DOWNLOADED
                    )
                )
            }
        }
        return lastItemCheckedTitle
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerBLBroadcastReceiver(blBroadCastReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterBLBroadcastReceiver(blBroadCastReceiver)
    }

    fun registerBLBroadcastReceiver(blBroadCastReceiver: BroadcastReceiver, filter: IntentFilter) {
        if (!blReceiverRegistered) {
            registerReceiver(blBroadCastReceiver, filter)
            blReceiverRegistered = true
        }
    }
    fun unregisterBLBroadcastReceiver(blBroadCastReceiver: BroadcastReceiver) {
        if (blReceiverRegistered) {
            unregisterReceiver(blBroadCastReceiver)
            blReceiverRegistered = false
        }
    }

    override fun onBackPressed() {
        // all files have been downloaded so install them
        bldlActivity.unregisterBLBroadcastReceiver(blBroadCastReceiver) // bug fix for crash in updateStories()

        var downloadDir = bloomSourceAutoDLDir()
        var firstExists = data.indexOfFirst { it -> File(downloadDir + "/" + it.title + bloomSourceZipExt()).exists() }
        if (firstExists != -1) {
            // one or more files have been downloaded so install them
            MainActivity.mainActivity.controller.updateStories() // process the downloaded templates
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        // this onBackPressed() override also unregisters the broadcast receiver and calls updateStories()
        onBackPressed()
        return true
    }

    // Click listener for clicks on individual cards in the recycler list of BL downloads
    class BLOnClickListener internal constructor(private val context: Context, var lastSelectedItem: Int = -1) : View.OnClickListener {

        // make true to allow multiple items to be checked for download
        var allowMultipleSelection: Boolean = false

        override fun onClick(v: View) {
            val selectedItemPosition: Int = recyclerView.getChildLayoutPosition(v)

            val isEnabled = toggleSelected(selectedItemPosition)
            if (isEnabled and !allowMultipleSelection) {
                // remove previously selected card
                if ((lastSelectedItem != -1) and (lastSelectedItem != selectedItemPosition)) {
                    toggleSelected(lastSelectedItem) // uncheck previous
                    lastSelectedItem = selectedItemPosition
                }
                else if (lastSelectedItem == selectedItemPosition) {
                    lastSelectedItem = -1   // toggled off
                }
                else
                    lastSelectedItem = selectedItemPosition // remember last selected position
            }
        }

        private fun toggleSelected(i: Int) : Boolean {
            val viewHolder: RecyclerView.ViewHolder? = recyclerView.findViewHolderForLayoutPosition(i)
            val cardTemplate = viewHolder?.itemView?.findViewById(R.id.card_view) as BLCheckableCardView
            val imageViewCheckBox = viewHolder?.itemView?.findViewById(R.id.imageViewCheckBox) as ImageView
            var blViewHolder = viewHolder as BLCustomAdapter.BLViewHolder

            var selectedDataItem = data.get(i)
            if (!selectedDataItem.isInWorkspace)
                selectedDataItem.isChecked = !selectedDataItem.isChecked;   // toggle the checked state

            // toggle checked icon visible or not using the alpha (transparency) property
            imageViewCheckBox.alpha = if (selectedDataItem.isChecked) 1.0F else 0.0F
            return !selectedDataItem.isInWorkspace
        }
    }
}

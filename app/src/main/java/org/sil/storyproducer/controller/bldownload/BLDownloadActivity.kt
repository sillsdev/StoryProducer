package org.sil.storyproducer.controller.bldownload

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import org.sil.storyproducer.model.parseOPDSfile
import org.sil.storyproducer.tools.file.copyToWorkspacePath
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import java.io.File

// https://www.tutorialspoint.com/how-to-register-a-broadcast-receiver-programmatically-in-android-using-kotlin
class BLBroadCastReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "BLBroadCastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent!!.action) {
            var moreDownloads = 0;
            val downloadedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            for (i in 0 until BLDownloadActivity.data.size) {
                val model = BLDownloadActivity.data.get(i)
                if (model.downloadId == downloadedId) {
                    var downloadedUri = BLDownloadActivity.downloadManager.getUriForDownloadedFile(model.downloadId)
//                    copyToWorkspacePath(BLDownloadActivity.bldlActivity, downloadedUri, model.title + ".bloomSource")
//                    if (BLDownloadActivity.bldlActivity.contentResolver.delete(downloadedUri, null, null) != 1)
//                        Log.w(TAG, "Unable to delete: " + model.title + ".bloomSource")
                    model.downloadId = -1;
                    Toast.makeText(context, "'" + model.title + "' download complete", Toast.LENGTH_LONG).show()
                }
                else if (model.downloadId != -1L && model.downloadId != -2L) {
                    moreDownloads++
                }
            }
            if (moreDownloads == 0) {
                // all files have been downloaded so install them
                // close BL download activity so we can see the templates being processed
//                  bldlActivity.finish()
                BLDownloadActivity.bldlActivity.onBackPressed()
                // some files have already been downloaded so install them
                MainActivity.mainActivity.controller.updateStories() // process the downloaded templates
            }
        }
    }
}

class BLDownloadActivity : AppCompatActivity() {

    companion object {
        private val TAG = "BLDownloadActivity"

        lateinit var bldlActivity: BLDownloadActivity
        lateinit var adapter: RecyclerView.Adapter<BLCustomAdapter.BLViewHolder?>
        var data: ArrayList<BLDataModel> = ArrayList<BLDataModel>()
        lateinit var recyclerView: RecyclerView
        lateinit var downloadManager: DownloadManager
    }

    lateinit var blOnClickListener: BLOnClickListener
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var binding: ActivityBldownloadBinding
    private val blBroadCastReceiver: BroadcastReceiver = BLBroadCastReceiver()

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

        binding.bldlFab.setOnClickListener { view ->
            var numQueued = 0
            var numAlreadyDownloaded = 0
            var fileDownloadDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            for (i in 0 until data.size)
            {
                var dataItem = data.get(i)
                if (dataItem.isChecked && !dataItem.isInWorkspace) // && dataItem.downloadId == -1L)
                {   // user is initiating a download of this title from bloom library
                    val request = DownloadManager.Request(Uri.parse(dataItem.downloadUri))
                    request.setTitle(dataItem.title)
                    request.setDescription("'" + dataItem.title + "' download")
                    request.setDestinationInExternalFilesDir(this,
                            Environment.DIRECTORY_DOWNLOADS, dataItem.title + ".bloomSource")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    dataItem.downloadId = downloadManager.enqueue(request);
                    numQueued++
                }
                else if (dataItem.isChecked && dataItem.isInWorkspace && dataItem.downloadId == -2L)
                {   // download of this title has already been made so copy it to templates folder ready to install it
                    var dlFile = File( fileDownloadDir?.path + "/" + dataItem.title + ".bloomSource")
                    var downloadedUri = Uri.fromFile(dlFile)
//                    copyToWorkspacePath(bldlActivity, downloadedUri, dataItem.title + ".bloomSource")
//                    if (!dlFile.delete())
//                        Log.w(TAG, "Unable to delete: " + dataItem.title + ".bloomSource")
                    numAlreadyDownloaded++
                }
            }

            if (numQueued > 0) {
                Snackbar.make(view, "Download Started", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            else if (numAlreadyDownloaded > 0) {
                Snackbar.make(view, "Already downloaded", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                // close BL download activity so we can see the templates being processed
//                bldlActivity.finish()
                bldlActivity.onBackPressed()
                // some files have already been downloaded so install them
                MainActivity.mainActivity.controller.updateStories() // process the downloaded templates
            }

        }

        // now follows code adapted from:
        // https://www.digitalocean.com/community/tutorials/android-recyclerview-android-cardview-example-tutorial

        // Icons from:
        // <a href="https://www.flaticon.com/free-icons/download" title="download icons">Download icons created by Becris - Flaticon</a>

        recyclerView = binding.bldlCardRecyclerView
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()

        var lastItemClickedTitle = ""
        // get download folder for checking if already downloaded
        var fileDownloadDir = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) as File
        val blDataList = parseOPDSfile();
        for (i in 0 until blDataList.size) {
            val blItem = blDataList.get(i)
            val isFileDownloaded = File(fileDownloadDir.path + "/" + blItem.Title + ".bloomSource").exists()
            var isInWorkspace = isFileDownloaded or workspaceRelPathExists(this, blItem.Title) // already in workspace
            val blItemFound = data.find { it -> it.title == blItem.Title }
            if (blItemFound != null)
            {
                if (isFileDownloaded)
                    blItemFound.isChecked = true
                else if (isInWorkspace)
                    blItemFound.isChecked = false
                blItemFound.isInWorkspace = isInWorkspace
                if (isFileDownloaded)
                    blItemFound.downloadId = -2 // -2 is file downloaded outside of BLDownloadActivity running
                if (blItemFound.isChecked && !blItemFound.isInWorkspace)
                    lastItemClickedTitle = blItemFound.title
            }
            else
            {
                data.add(
                    BLDataModel(
                        blItem.Title,
                        blItem.LangCode,
                        if (i == 0) R.drawable.temp_001_widdows_offering_thumbnail else R.drawable.temp_sp_logo_book, // example image on 0 - JUST TESTING
                        blItem.BloomSourceURL,
                        isInWorkspace,
                        isFileDownloaded,   // is checked
                        if (isFileDownloaded) -2 else -1) // download id; -1 = not downloaded; -2 = already downloaded
                )
            }
        }
        var lastItemClicked = -1
        if (!lastItemClickedTitle.isEmpty()) {
            lastItemClicked = data.indexOfFirst { it -> it.title == lastItemClickedTitle }
        }
        blOnClickListener = BLOnClickListener(this, lastItemClicked)

        adapter = BLCustomAdapter(data)
        recyclerView.setAdapter(adapter)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(blBroadCastReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(blBroadCastReceiver)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class BLOnClickListener internal constructor(private val context: Context, var lastSelectedItem: Int = -1) : View.OnClickListener {

        var allowMultipleSelection: Boolean = false

        fun toggleSelected(i: Int) : Boolean {
            val viewHolder: RecyclerView.ViewHolder? = recyclerView.findViewHolderForLayoutPosition(i)
            val cardTemplate = viewHolder?.itemView?.findViewById(R.id.card_view) as BLCheckableCardView
            val imageViewCheckBox = viewHolder?.itemView?.findViewById(R.id.imageViewCheckBox) as ImageView
            var blViewHolder = viewHolder as BLCustomAdapter.BLViewHolder

            var selectedDataItem = data.get(i)
            if (!selectedDataItem.isInWorkspace)
                selectedDataItem.isChecked = !selectedDataItem.isChecked;
            imageViewCheckBox.alpha = if (selectedDataItem.isChecked) 1.0F else 0.0F
            return !selectedDataItem.isInWorkspace
        }

        override fun onClick(v: View) {
            selectItemForDownload(v)
        }

        private fun selectItemForDownload(v: View) {
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
    }
}

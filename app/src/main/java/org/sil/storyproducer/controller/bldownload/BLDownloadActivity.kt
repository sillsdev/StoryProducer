package org.sil.storyproducer.controller.bldownload

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.sil.bloom.reader.CommonUtilities
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.databinding.ActivityBldownloadBinding
import org.sil.storyproducer.model.BLBook
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.bloomSourceAutoDLDir
import org.sil.storyproducer.model.bloomSourceZipExt
import org.sil.storyproducer.model.parseOPDSfile
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import java.io.File
import java.util.Locale

// This class implements the code for the BLDownloadActivity which shows the UI to download a story
// automatically from the Bloom Library.  This UI consists of a list of CardView widgets each
// detailing an individual story available for download
class BLDownloadActivity : AppCompatActivity() {

    // based on the example: https://www.tutorialspoint.com/how-to-register-a-broadcast-receiver-programmatically-in-android-using-kotlin
    // This sub class will act as a receiver for a message send by the DownloadManager used to download
    // stories in the background
    class BLBroadCastReceiver(bldata: ArrayList<BLDataModel>) : BroadcastReceiver() {
        companion object {
            private val TAG = "BLDownloadActivity.BLBroadCastReceiver"
        }

        var data: ArrayList<BLDataModel>
        init {
            data = bldata
        }
        // process a message from the DownloadManager to see if it has completed one of our story files
        override fun onReceive(context: Context?, intent: Intent?) {
            // check that we have received a download complete action
            if (context != null && intent != null && DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
                // process the download complete action - installing any downloads if necessary
                var moreDownloadsToComplete = 0
                var recognisedDownloads = 0
                val downloadCompleteId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, // get the downloaded id
                                                    BLDataModel.DOWNLOAD_NOT_REQUESTED) // default
                val fileDownloadDir = bloomSourceAutoDLDir()
                for (i in 0 until data.size) {
                    val model = data[i]
                    // Check if the file has already been downloaded or removed somehow since last checked
                    val fileLang = fileLangExt(model.lang)
                    model.isInBLDLDir = File(fileDownloadDir + "/" + convertToSafeFilename(model.title) + fileLang + bloomSourceZipExt()).exists()
                    if (model.isInBLDLDir && !model.isInWorkspace) {
                        recognisedDownloads++
                    }
                    if (downloadCompleteId >= 0 && model.downloadId == downloadCompleteId) {
                        model.downloadId = BLDataModel.DOWNLOADED_COMPLETE   // no longer need to check for this download id

                        // Log a successful download with Firebase analytics
                        val query = DownloadManager.Query().setFilterById(downloadCompleteId)
                        val cursor = downloadManager.query(query)
                        cursor?.use {
                            if (cursor.moveToNext()) {
                                val uri = CommonUtilities.getStringFromCursor(cursor, DownloadManager.COLUMN_URI)
                                val downloadStatus = CommonUtilities.getIntFromCursor(cursor, DownloadManager.COLUMN_STATUS)
                                Workspace.logDownloadEvent(context, Workspace.DOWNLOAD_TEMPLATE_TYPE.BIBLE_STORY, Workspace.DOWNLOAD_EVENT.COMPLETE,
                                    downloadCompleteId, downloadStatus, uri, "", model.lang)
                            }
                        }

                    }
                    else if (model.downloadId >= 0) {
                        moreDownloadsToComplete++ // we have other ids in the list that are still being downloaded
                    }
                }

                if (moreDownloadsToComplete == 0 && recognisedDownloads > 0) {
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
        private fun convertToSafeFilename(downloadUri: String): String {
            return downloadUri.replace('?', '_')
                    .replace('/', '_')
                    .replace('|', '_')
                    .replace('\\', '_')
                    .replace('*', '_')
                    .replace('<', '_')
                    .replace(':', '_')
                    .replace('>', '_')
                    .replace('\n', '_')
                    .replace('\u00a0', ' ')
                    .replace(Regex("\\s+"), " ")

        }

        // gets the primary (first) language from a space separated list
        fun primaryLang(langs : String) : String {
            val trimmedLangs = langs.trim()
            if (trimmedLangs.isEmpty())
                return ""
            return trimmedLangs.split(" ").first().trim()
        }

        // gets the primary language file extension string
        fun fileLangExt(langs : String) : String {
            val selLang = primaryLang(langs)
            var fileLang = ""
            if (selLang.isNotEmpty())
                fileLang = ".lang_$selLang";
            return fileLang
        }

        private val TAG = "BLDownloadActivity"

        lateinit var bldlActivity: BLDownloadActivity
        lateinit var adapter: RecyclerView.Adapter<BLCustomAdapter.BLViewHolder?>
        var data: Array<ArrayList<BLDataModel>> = Array<ArrayList<BLDataModel>>(2) { ArrayList<BLDataModel>() }
        lateinit var recyclerView: RecyclerView
        lateinit var downloadManager: DownloadManager
        var selectedLangFilterIndex = 0
        private var selectedLangFilter = ""
        var lastItemClicked = -1    // last checked story download item - used for toggling
        var lastItemCheckedTitle = ""   // for automatically unchecking items if more than one clicked on
    }

    lateinit var blOnClickListener: BLOnClickListener
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var binding: ActivityBldownloadBinding
    private lateinit var blBroadCastReceiver: BroadcastReceiver
    private var blReceiverRegistered: Boolean = false
    private var bldlActivityIndex: Int = -1
    private val bldlImageId = intArrayOf(R.drawable.ic_launcher_foreground, R.drawable.temp_bloom_logo_transparent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bldlActivity = this

        binding = ActivityBldownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.title = title

        bldlActivityIndex = intent.getIntExtra(BaseActivity.BLOOM_DL_ACTIVITY_INDEX, -1)
        blBroadCastReceiver = BLBroadCastReceiver(data[bldlActivityIndex])

        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        // Add listener for when 'Floating Action Button' (FAB) Cloud download button is clicked
        // Icon from: <a href="https://www.flaticon.com/free-icons/download" title="download icons">Download icons created by Becris - Flaticon</a>
        // Now setup a listener, for FAB button click, here using a lambda
        binding.bldlFab.setOnClickListener { view ->
            var numQueued = 0
            var numDownloadsCompleted = 0
            var numAlreadyDownloaded = 0
            var numAlreadyInstalled = 0
            val fileDownloadDir = bloomSourceAutoDLDir()
            for (i in 0 until  data[bldlActivityIndex].size)
            {
                val dataItem = data[bldlActivityIndex][i]
                // Check if the file has already been downloaded or removed somehow since last checked
                dataItem.isInBLDLDir = File(fileDownloadDir + "/" + convertToSafeFilename(dataItem.title) + bloomSourceZipExt()).exists()
                // find out which action(s) or message needs processing
                if (dataItem.isChecked && dataItem.isEnabled)
                {
                    // user is initiating a download of this title from bloom library
                    val request = DownloadManager.Request(Uri.parse(dataItem.downloadUri))

                    // add properties for this download request
                    request.setTitle(dataItem.title)
                    request.setDescription(getString(R.string.bloom_lib_download))

                    val destDir = File(this.getExternalFilesDir(null), Environment.DIRECTORY_DOWNLOADS);

                    val fileLang = fileLangExt(dataItem.lang)
                    val destFile = File(destDir, convertToSafeFilename(dataItem.title) + fileLang + bloomSourceZipExt())
                    request.setDestinationUri(Uri.fromFile(destFile))

                    request.setAllowedOverMetered(true)     // set here until we have a setting for it
                    request.setAllowedOverRoaming(true)     // set here until we have a setting for it
                    //request.setRequiresDeviceIdle(false)  // this property requires N (Nougat)
                    //request.setRequiresCharging(false)    // this property requires N (Nougat)

                    // set download visibility in the DownloadManager
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    // queue the download
                    dataItem.downloadId = downloadManager.enqueue(request)
                    val dmStatus =
                        if (dataItem.downloadId == -1L) DownloadManager.STATUS_FAILED else DownloadManager.STATUS_RUNNING
                    Workspace.logDownloadEvent(view.context, Workspace.DOWNLOAD_TEMPLATE_TYPE.BIBLE_STORY, Workspace.DOWNLOAD_EVENT.START,
                        dataItem.downloadId, dmStatus, dataItem.downloadUri, "", dataItem.lang)

                    adapter.notifyItemChanged(i)    // update the display of this card to be grayed out

                    numQueued++
                }
                else if (dataItem.isInBLDLDir &&
                    !dataItem.isInWorkspace &&
                    (dataItem.downloadId >= 0 || dataItem.downloadId == BLDataModel.DOWNLOADED_COMPLETE))
                {   // download of this queued title has already been completed
                    numDownloadsCompleted++
                }
                else if (dataItem.isInBLDLDir &&
                    !dataItem.isInWorkspace)
                {   // download of this forgotten queued title (possibly due to SP app being closed down)
                    // has already been completed outside of BL Download Activity
                    numAlreadyDownloaded++
                }
                else if (dataItem.isInBLDLDir &&
                    dataItem.isInWorkspace)
                {   // Already download somehow but also already installed
                    numAlreadyInstalled++
                }
            }

            if (numQueued > 0) {
                // tell user that download(s) have started
                Snackbar.make(view, getString(R.string.bloom_lib_download_started), 60 * 1000)
                    .setAction("Action", null).show()
            }
            else if (numAlreadyDownloaded > 0 || numDownloadsCompleted > 0) {
                // a toast message is used so that it is visible in the Main Activity after this activity ends
                Toast.makeText(this,
                        if (numDownloadsCompleted > 0)
                            getString(R.string.bloom_lib_download_complete)
                        else
                            getString(R.string.bloom_lib_already_downloaded),
                    Toast.LENGTH_LONG).show()
                // close BL download activity so we can see the templates being processed by the MainActivity
                // this onBackPressed() override also unregisters the broadcast receiver and calls updateStories()
                bldlActivity.onBackPressed()
            }
            else if (numAlreadyInstalled > 0) {
                // warn the user 'already installed' and do nothing
                Snackbar.make(view, getString(R.string.bloom_lib_already_installed), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            else {
                // warn the user 'nothing selected' and do nothing
                Snackbar.make(view, getString(R.string.bloom_lib_nothing_selected), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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

        // Get the definitive list of available Bloom Library story templates
        val blDataList = parseOPDSfile(bldlActivityIndex)
        if (blDataList != null)
        {
            onDownloadXmlBloomCatalogSuccess(false) // hide the loading messages
            // Find or add BLDataModel items to our data list and update any members for the known state
            lastItemClicked = blFindOrAddBookItems(blDataList)

            blShowLangFilter(blDataList)

        }
    }

    // When called for the first time it adds new BLDataModel items to the 'data' model list
    // To match each OPDS file BLBook item in the parsed xml file
    // Subsequent calls will update the BLDataModel data list items with new information on
    // if the downloaded file exists or is in the workspace.
    // It will also add new BLDataModel items to the end of the data list to match any newly found
    // BLBook items that may have been added to the source xml file
    private fun blFindOrAddBookItems(
        blDataList: MutableList<BLBook>
    ): Int {
        val fileDownloadDir = bloomSourceAutoDLDir()
        for (i in 0 until blDataList.size) {
            val blItem = blDataList[i]
            // Check if the file has already been downloaded
            val isFileDownloaded =
                File(fileDownloadDir + "/" + convertToSafeFilename(blItem.Title) + bloomSourceZipExt()).exists()
            // Check if the title is already in the workspace
            var isInWorkspace = workspaceRelPathExists(this, convertToSafeFilename(blItem.Title))
            if (!isInWorkspace)
                isInWorkspace = workspaceRelPathExists(this, convertToSafeFilename(blItem.Title + ".lang_${primaryLang(blItem.LangCode)}"))
            val blItemFound = data[bldlActivityIndex].find { it -> it.title == blItem.Title }
            if (blItemFound != null) {
                blItemFound.isInWorkspace = isInWorkspace
                blItemFound.isInBLDLDir = isFileDownloaded
                if (isInWorkspace) {
                    blItemFound.isChecked = false   // already installed
                    if (isFileDownloaded)
                        blItemFound.downloadId = BLDataModel.ALREADY_DOWNLOADED
                    else
                        blItemFound.downloadId = BLDataModel.DOWNLOAD_NOT_REQUESTED
                }
                else if (isFileDownloaded) {
                    blItemFound.isChecked = true    // downloaded and ready for installing
                    if (blItemFound.downloadId >= 0) {
                        // file was downloaded outside of the BLDownloadActivity running
                        blItemFound.downloadId = BLDataModel.DOWNLOADED_COMPLETE
                    }
                    else if (blItemFound.downloadId != BLDataModel.DOWNLOADED_COMPLETE) {
                        // file was downloaded after SP app has been restarted
                        blItemFound.downloadId = BLDataModel.ALREADY_DOWNLOADED
                    }
                } else if (blItemFound.downloadId == BLDataModel.DOWNLOADED_COMPLETE) {
                    blItemFound.downloadId = BLDataModel.DOWNLOAD_NOT_REQUESTED
                }
                if (blItemFound.isChecked && blItemFound.isEnabled)
                    lastItemCheckedTitle =
                        blItemFound.title    // make a note in case we need to uncheck it
            } else {   // we need to add this title to our data list
                data[bldlActivityIndex].add(
                    // Add a new data model item passing in the correct values to the constructor
                    BLDataModel(
                        blItem.Title,
                        blItem.LangCode,
//                        if (blItem.Title == "001 The Widow’s Offering")
//                            R.drawable.temp_001_widdows_offering_thumbnail // TODO: example image - just testing appearance
//                        else
                        bldlImageId[bldlActivityIndex],
                        blItem.BloomSourceURL,
                        isInWorkspace,      // makes it grayed out
                        isFileDownloaded,   // remember if already downloaded
                        isFileDownloaded,   // makes it checked
                        if (isFileDownloaded) BLDataModel.ALREADY_DOWNLOADED else BLDataModel.DOWNLOAD_NOT_REQUESTED
                    )
                )
            }
        }

        var lastItemClicked = -1
        if (lastItemCheckedTitle.isNotEmpty()) {
            val filteredData = data[bldlActivityIndex].filter { selectedLangFilter.isEmpty() || primaryLang(it.lang) == selectedLangFilter }
            lastItemClicked = filteredData.indexOfFirst { it -> it.title == lastItemCheckedTitle }
        }
        lastItemCheckedTitle = ""
        return lastItemClicked
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

    private fun registerBLBroadcastReceiver(blBroadCastReceiver: BroadcastReceiver, filter: IntentFilter) {
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
        bldlActivity.unregisterBLBroadcastReceiver(blBroadCastReceiver) // bug fix for crash in updateStories()

        // if any downloaded files exists from a previous action, process them now
        val downloadDir = bloomSourceAutoDLDir()
        val firstExists = data[bldlActivityIndex].indexOfFirst {
            File(downloadDir + "/" + convertToSafeFilename(it.title) + fileLangExt(it.lang) + bloomSourceZipExt()).exists()
        }
        if (firstExists != -1) {
            // one or more files have been downloaded so install them
            if (MainActivity.mainActivity != null) {
                MainActivity.mainActivity!!.controller.updateStories() // process the downloaded templates
            }
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        // this onBackPressed() override also unregisters the broadcast receiver and calls updateStories()
        onBackPressed()
        return true
    }

    // load the BL books in the the CardView if loadBooks is enabled
    // then hide the loading messages
    fun onDownloadXmlBloomCatalogSuccess(loadBooks : Boolean = true) {
        if (loadBooks) {
            val bookList = parseOPDSfile(bldlActivityIndex)
            if (bookList != null) {
                lastItemClicked = blFindOrAddBookItems(bookList)

                blShowLangFilter(bookList)

            }
            adapter.notifyDataSetChanged()
        }
        binding.textViewLoading.visibility = View.INVISIBLE  // hide loading messages
        binding.textViewWait.visibility = View.INVISIBLE
    }

    // get the UI display name for a bloom language codes if known by Android
    // TODO: use a more comprehensive display language name in the future if more codes used
    private fun nativeLanguageName(languageCode: String): String {
        val codes = languageCode.split("-")
        if (codes.size >= 2 && codes[0].trim().isNotEmpty() && codes[1].trim().isNotEmpty())
            return Locale(codes[0].trim(), codes[1].trim()).displayName
        return Locale(languageCode).displayName
    }

    // shows the language filter dropdown combo
    private fun blShowLangFilter(bookList: MutableList<BLBook>) {

        val langCodes: MutableList<Pair<String, String>> = mutableListOf()
        bookList.forEach { it ->
            val plang = primaryLang(it.LangCode)
            if (plang.isNotEmpty()) {
                if (langCodes.find { it.first == plang } == null) {
                    langCodes.add(Pair(plang, nativeLanguageName(plang)))
                }
            }
        }
        langCodes.add(Pair("", getString(R.string.lang_codes_all_languages)))
        langCodes.sortBy { it.second }
        val langFilter = findViewById<Spinner>(R.id.bldl_lang_choices)
        // Create an ArrayAdapter using the list and a default spinner layout
        val langAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            langCodes.map { it.second }
        )
        // Specify the layout to use when the list of choices appears
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        langFilter.adapter = langAdapter

            // get language filter initial selection from persisted settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastLangFilterCode = prefs.getString("bldl_filter_lang", "")
        val lastLangFilterIndex = langCodes.indexOfFirst { it.first == lastLangFilterCode }
        selectedLangFilterIndex = if (lastLangFilterIndex == -1) 0 else lastLangFilterIndex
        langFilter.setSelection(selectedLangFilterIndex)

        // Set an OnItemSelectedListener to the Spinner
        langFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // The selected item position is 'position'
                val selectedItem = langCodes[position].first
                if (selectedItem != selectedLangFilter) {
                    lastItemClicked = -1
                    lastItemCheckedTitle = ""
                    data[bldlActivityIndex].forEach { it.isChecked = false }
                }
                selectedLangFilter = selectedItem
                selectedLangFilterIndex = position

                    // persist the language filter code for next time
                val prefs = PreferenceManager.getDefaultSharedPreferences(view?.context)
                val editor = prefs.edit()
                editor.putString("bldl_filter_lang", selectedLangFilter)
                editor.apply()

                blOnClickListener = BLOnClickListener(data[bldlActivityIndex], selectedLangFilter, lastItemClicked)

                adapter = BLCustomAdapter(data[bldlActivityIndex], selectedItem)
                recyclerView.adapter = adapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        blOnClickListener = BLOnClickListener(data[bldlActivityIndex], selectedLangFilter, lastItemClicked)

        adapter = BLCustomAdapter(data[bldlActivityIndex], selectedLangFilter)
        recyclerView.setAdapter(adapter)

    }

    // On error display the error messages and set error color
    fun onDownloadXmlBloomCatalogFailure(errorMessage : String) {
        binding.textViewLoading.text = getString(R.string.bloom_lib_loading_failed)
        binding.textViewWait.text = errorMessage
        binding.textViewWait.setTextColor(Color.parseColor("#ff4000"))  // A more visible red color
    }


    // Click listener for clicks on individual cards in the recycler list of BL downloads
    class BLOnClickListener internal constructor(var bldata: ArrayList<BLDataModel>, val selectedLangFilter : String, var lastSelectedItem: Int = -1) : View.OnClickListener {

        // make true to allow multiple items to be checked for download
        private var allowMultipleSelection: Boolean = false

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
            val filteredDataSet: ArrayList<BLDataModel> = bldata.filter {
                selectedLangFilter.isEmpty() || primaryLang(it.lang) == selectedLangFilter
            } as ArrayList<BLDataModel>

            val selectedDataItem = filteredDataSet[i]
            if (selectedDataItem.isEnabled)
                selectedDataItem.isChecked = !selectedDataItem.isChecked   // toggle the checked state

            adapter.notifyItemChanged(i)    // update the check mark on the display of this card

            // toggle checked icon visible or not using the alpha (transparency) property
            return selectedDataItem.isEnabled
        }
    }
}

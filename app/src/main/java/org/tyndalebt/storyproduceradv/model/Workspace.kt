package org.tyndalebt.storyproduceradv.model

import WordLinksCSVReader
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.Settings.Secure
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.tyndalebt.storyproduceradv.BuildConfig
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.activities.DownloadActivity
import org.tyndalebt.storyproduceradv.model.messaging.Approval
import org.tyndalebt.storyproduceradv.model.messaging.MessageROCC
import org.tyndalebt.storyproduceradv.tools.file.deleteWorkspaceFile
import org.tyndalebt.storyproduceradv.tools.file.getChildOutputStream
import org.tyndalebt.storyproduceradv.tools.file.wordLinkListFromJson
import org.tyndalebt.storyproduceradv.tools.file.workspaceRelPathExists
import java.io.*
import java.net.URI
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal const val SLIDE_NUM = "CurrentSlideNum"
internal const val DEMO_FOLDER = "000 Unlocked demo story Storm"
internal const val PHASE = "Phase"

internal const val WORD_LINKS_DIR = "wordlinks"
internal const val WORD_LINKS_CSV = "wordlinks.csv"
internal const val WORD_LINKS_JSON_FILE = "wordlinks.json"
internal const val WORD_LINKS_CLICKED_TERM = "ClickedTerm"
internal const val WORD_LINKS_SLIDE_NUM = "CurrentSlideNum"
// DKH - 11/19/2021 Issue #611 Allow CSV file to have different names
// Use Regex expression when looking for CSV word links file
internal val WORK_LINKS_CSV_REGEX_STRING = "wordlinks.*csv"
internal val WORD_LINKS_CSV_REGEX = WORK_LINKS_CSV_REGEX_STRING.toRegex()


object Workspace {
    // DKH - 01/15/2022 Issue #571 Add a menu item for accessing templates from Google Drive
    // The strings.xml file contains the "Welcome Screen" html in the following string:
    // <string name="welcome_screen_select_template_folder">).  Embedded in the string is
    // the URL to the the location of the shared drive that contains the templates.
    // Instead of duplicating the same string, remove the reference from the strings.xml file and
    // update the "Welcome Screen" html with the URL before display of the "Welcome Screen"
    // ( WelcomeDialogActivity.kt).  We will have different strings.xml file for each
    // language and by having the URL defined here, we never have to update the different strings.xml
    // files.
    // DKH - 5/3/2022 Issue OR14 - Update to use secure SPadv website
//    Download templates from ROCC server
//    const val URL_FOR_TEMPLATES = "https://sites.google.com/view/spadv-website"
    const val URL_FOR_WEBSITE = "https://sites.google.com/view/spadv-website/home?authuser=2"
    // These are the place holder strings in the "Welcome Screen" html.  Before displaying the
    // "Welcome Screen", replace this place holder strings with the URL_FOR_TEMPLATES
//    const val URL_FOR_TEMPLATES_PLACE_HOLDER = "URL_FOR_TEMPLATES_PLACE_HOLDER"
    // End Issue #571

    // destination folder for bloom files downloaded from a server (to then be parsed)
    lateinit var bloomFolder:File
    // parseLanguage is used to store the language folder name from the server to eventually be stored in the story.json file as it is parsed
    lateinit var parseLanguage: String

    var workdocfile = DocumentFile.fromFile(File(""))
        set(value) {
            field = value
            prefs?.edit()?.putString("workspace", field.uri.toString())?.apply()
        }
    val Stories: MutableList<Story> = mutableListOf()
    var registration: Registration = Registration()
    var phases: List<Phase> = ArrayList()
    val approvalList: MutableList<Approval> = mutableListOf()
    var activePhaseIndex: Int = -1
    var isInitialized = false
    var prefs: SharedPreferences? = null
    var startedMain = false
    var InternetConnection = true
    // DKH - 05/12/2021
    // Issue #573: SP will hang/crash when submitting registration
    // This flag indicates whether MainActivity should call the RegistrationActivity to allow
    // the user to update the registration
    // This is set in BaseController function onStoriesUpdated()
    var showRegistration = false
    // DBH 9/14/22
    // Issue #73  Show registration screen whenever more templates is selected, until registration has been submitted
    // This flag marks whether main should go to "More Templates" or to the template list
    var showMoreTemplates = false
    // To track if force quit is happening or just changing between activities
    var LastActivityEvent: String = ""

    // word links
    lateinit var activeWordLink: WordLink
    var termToWordLinkMap: MutableMap<String, WordLink> = mutableMapOf()
    var termFormToTermMap: MutableMap<String, String> = mutableMapOf()
    var WLSTree = WordLinkSearchTree()
    val approvalChannel = BroadcastChannel<Approval>(30)

    var activeStory: Story = emptyStory()
    set(value){
        field = value
        //You are switching the active story.  Recall the last phase and slide.
        activePhase = Phase(value.lastPhaseType)
        activeSlideNum = value.lastSlideNum
        // DKH - Updated 06/03/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
        // Record time when the story was last run - this will show up in the story.json file
        // This is mainly used for debugging a story.json file that has a parse error
        value.storyToJasonTimeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }
    var activePhase: Phase = Phase(PhaseType.LEARN)
        set(value){
            field = value
            activePhaseIndex = -1
            for((i,p) in phases.withIndex()){
                if(p.phaseType == value.phaseType) activePhaseIndex = i
            }
        }
    val activeDirRoot: String
    get() {
        return if (activePhase.phaseType == PhaseType.WORD_LINKS) {
            WORD_LINKS_DIR
        } else {
            activeStory.title
        }
    }

    val activeDir: String
    get() {
        return if (activePhase.phaseType == PhaseType.WORD_LINKS) {
            activeWordLink.term
        } else {
            PROJECT_DIR
        }
    }

    val activeFilenameRoot: String
    get() {
        return if(activePhase.phaseType == PhaseType.WORD_LINKS) {
            activeWordLink.term
        } else {
            return "${activePhase.getFileSafeName()}${ activeSlideNum }"
        }
    }

    var activeSlideNum: Int = -1
    set(value){
        field = 0
        if(value >= 0 && value < activeStory.slides.size){
            if(activePhase.checkValidDisplaySlideNum(value))
                field = value
        }
    }
    val activeSlide: Slide?
    get(){
        if(activeStory.title == "") return null
        return activeStory.slides[activeSlideNum]
    }

    val messages = ArrayList<MessageROCC>()
    val queuedMessages = ArrayDeque<MessageROCC>()
    val messageChannel = BroadcastChannel<MessageROCC>(30)
    val toSendMessageChannel = Channel<MessageROCC>(100)
    var messageClient: MessageWebSocketClient? = null
    var lastReceivedTimeSent = Timestamp(0)

    fun getRoccUrlPrefix(context: Context): String {
        return if (BuildConfig.ENABLE_IN_APP_ROCC_URL_SETTING) {
            PreferenceManager.getDefaultSharedPreferences(context).getString("ROCC_URL_PREFIX", BuildConfig.ROCC_URL_PREFIX)
                ?: BuildConfig.ROCC_URL_PREFIX
        } else {
            BuildConfig.ROCC_URL_PREFIX
        }
    }

    fun getRoccWebSocketsUrl(context: Context): String {
        if (InternetConnection == false) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context,
                    context.getString(R.string.remote_check_msg_no_connection),
                    Toast.LENGTH_LONG).show()
            }
        }
        var baseUrl = if (BuildConfig.ENABLE_IN_APP_ROCC_URL_SETTING) {
            PreferenceManager.getDefaultSharedPreferences(context).getString("WEBSOCKETS_URL", BuildConfig.ROCC_WEBSOCKETS_PREFIX)
                ?: BuildConfig.ROCC_WEBSOCKETS_PREFIX
        } else {
            BuildConfig.ROCC_WEBSOCKETS_PREFIX
        }
        val projectId = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
        Log.e("@pwhite", "Getting websocket URL: $baseUrl/phone/$projectId")
        return "$baseUrl/phone/$projectId"
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    val WORKSPACE_KEY = "org.tyndalebt.storyproduceradv.model.workspace"

    fun initializeWorkspace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        setupWorkspacePath(context, Uri.parse(prefs!!.getString("workspace", "")))
        isInitialized = true
        parseLanguage = ""
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        startedMain = false
        Log.e("@pwhite", "about to create socket client ${getRoccWebSocketsUrl(context)}")
        GlobalScope.launch {
            for (message in messageChannel.openSubscription()) {
                messages.add(message)
                if (message.timeSent > lastReceivedTimeSent) {
                    lastReceivedTimeSent = message.timeSent
                }
            }
        }
        GlobalScope.launch {
            for (approval in approvalChannel.openSubscription()) {
                approvalList.add(approval)
                // If workspace not built yet, let main do this later
                if (Workspace.startedMain)
                    processReceivedApprovals()
            }
        }
        GlobalScope.launch {
            for (message in toSendMessageChannel) {
                try {
                    val js = messageToJson(message)
                    messageClient!!.send(js.toString(2))
                } catch (e: Exception) {
                    queuedMessages.add(message)
                }
            }
        }
        GlobalScope.launch {
            var hasSentCatchupMessage = false
            val reconnect: () -> Unit = {
                hasSentCatchupMessage = false
                val oldClient = messageClient
                if (oldClient == null || !oldClient.isOpen) {
                    oldClient?.close()
                    Log.e("@pwhite", "Restarting websocket.")
                    val newClient = MessageWebSocketClient(URI(getRoccWebSocketsUrl(context)))
                    newClient.connectBlocking()
                    if (newClient.isOpen == false) {
                        InternetConnection = false
                    } else {
                        InternetConnection = true
                    }
                    messageClient = newClient
                }
            }
            while (true) {
                try {
                    if (messageClient?.isOpen != true) {
                        reconnect()
                        delay(5000)
                    }
                    if (!hasSentCatchupMessage) {
                        val js = JSONObject()
                        js.put("type", "catchup")
                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        js.put("since", df.format(lastReceivedTimeSent))
                        messageClient!!.send(js.toString(2))
                        hasSentCatchupMessage = true
                    }
                    val nextQueuedMessage = queuedMessages.peek()
                    if (nextQueuedMessage != null) {
                        val js = messageToJson(nextQueuedMessage)
                        messageClient!!.send(js.toString(2))
                        queuedMessages.remove()
                    }
                    delay(500) // pause 1/2 second between checks
                } catch (ex: Exception) {
                    Log.e("@pwhite", "websocket iteration failed: ${ex} ${ex.message}. Closing old websocket.")
                    ex.printStackTrace();
                    reconnect()
                    delay(5000)
                }
            }
        }
    }

    fun messageToJson(m: MessageROCC): JSONObject {
        val js = JSONObject()
        js.put("type", "text")
        js.put("isTranscript", m.isTranscript)
        js.put("slideNumber", m.slideNumber)
        js.put("storyId", m.storyId)
        js.put("text", m.message)
        return js
    }

    fun setupWorkspacePath(context: Context, uri: Uri) {
        try {
            // Issue 539 - Reset Story info to detach from current Story, if any
            activeStory = emptyStory()

            // Initiate new workspace path
            workdocfile = DocumentFile.fromTreeUri(context, uri)!!
            bloomFolder = File(workdocfile.uri.path)
            registration.load(context)

            // load in the Word Links
            importWordLinks(context)
        } catch (e: Exception) {
            Log.e("setupWorkspacePath", "Error setting up new workspace path!", e)
        }
    }
    // DKH - 01/26/2022 Issue #571: Add a menu item for accessing templates from Google Drive
    // A new menu item was added that opens a URL for the user to download templates.
    // This is used in both the MainActivity menu (Story Templates display) and the Phase menus
    fun startDownLoadMoreTemplatesActivity(context: Context){
        val token = FirebaseMessaging.getInstance().token
        // if Registration has not been submitted yet, then show Registration screen first, then "Download More Templates"
        if (!Workspace.registration.complete && !Workspace.showMoreTemplates) {
            if (context is BaseActivity)
            {
                // Mark that going to registration came from Download more templates, so that it goes there after registration is done
                Workspace.showMoreTemplates = true
                val activity = context as BaseActivity
                activity.showRegistration(false)
            }
        }
        else {
            Workspace.showMoreTemplates = false
            startActivity(context, Intent(context, DownloadActivity::class.java), null)
        }
    }

    private fun importWordLinks(context: Context) {
        var wordLinksDir = workdocfile.findFile(WORD_LINKS_DIR)
        var csvFileName : String? = null  // default is no csv file, later on, create one if none found

        // DKH - 04/13/2022 Issue 625 WordLinks list is incorrectly copied when you select a new SP Templates folder
        // Before we load a new Word Links file, clear any previous Word Links data from memory.
        if(termToWordLinkMap.isNotEmpty()){
            // If the termToWorkLinkMap is not empty, it means the user has selected a new
            // workspace that will contain a different set of word links terms, so,
            // create an empty link search tree for the newly selected workspace
            WLSTree = WordLinkSearchTree()
            // Clear out all term data
            termToWordLinkMap.clear()
            termFormToTermMap.clear()
        }
        
        if (wordLinksDir == null) { // check to see if word links directory exists
            // DKH - 11/19/2021 Issue #611 Create Word Links CSV directory if it does not exist
            wordLinksDir = workdocfile.createDirectory(WORD_LINKS_DIR)
        }else{
            // DKH - 12/07/2021 Issue #611 Use the first CSV file that starts with "worklinks" & ends with "csv"

            // The WordLinks Directory exists, so,  see if we can find a csv file
            // CSV files start with the substring "wordlinks" and ends with ".csv"
            // scan all the files in the wordlinks directory looking for a valid csv file
            // If we have a valid CSV file, we don't have to create the default one
            for (filename in wordLinksDir!!.listFiles()) {
                // look for a wordlinks csv file
                if((filename.name)?.contains(WORD_LINKS_CSV_REGEX)!!){
                    csvFileName = filename.name  // found csv file
                    break
                }
            }
        }

        if (wordLinksDir != null) {
            // DKH - 11/19/2021 Issue #613 Create Word Links CSV file if it does not exist
            if(csvFileName == null) addWordLinksCSVFileToWorkspace(context, WORD_LINKS_CSV)
            // Process the CSV file
            importWordLinksFromCSV(context, wordLinksDir!!)
            importWordLinksFromJsonFiles(context, wordLinksDir!!)
            mapTermFormsToTerms()
            buildWLSTree()
        }else{
            Log.e("workspace", "Failed to create word links directory: $WORD_LINKS_CSV")
        }
    }

    private fun importWordLinksFromCSV(context: Context, wordLinksDir: DocumentFile){
        // DKH - 11/19/2021 Issue #611 Allow CSV file to have different names
        // Check for minor error condition in having multiple files that can be used for the
        // Word Links CSV file.  If we have more than one, we use the first one and print an
        // error message to the user
        var wordLinksFile : DocumentFile? = null  //initialize to file not found
        val wordLinksList = wordLinksDir.listFiles() // grab a list of files in the word links directory
        for (f in wordLinksList){ // iterate through the list of files
            if(f.name!!.contains(WORD_LINKS_CSV_REGEX)){ // Use Regex when looking for CSV file
                if(wordLinksFile != null){  // if true, we have multiple matches, so print an error
                    // print the error
                    Toast.makeText(context,
                        context.getString(R.string.wordlinks_multiple_csv_files) + wordLinksFile.name,
                            Toast.LENGTH_LONG).show()

                    break // stop looking and use the first match
                }
                wordLinksFile = f  // first match is the file that we use
            }
        }
        if (wordLinksFile != null) {  // check for nothing found
            try {
                // open a raw file descriptor to access data under the URI
                context.contentResolver.openFileDescriptor(wordLinksFile.uri, "r").use { pfd ->
                    ParcelFileDescriptor.AutoCloseInputStream(pfd).use { inputStream ->
                        InputStreamReader(inputStream).use { streamReader ->
                            WordLinksCSVReader(streamReader).use { wordLinkCSVReader ->
                                val wordLinks = wordLinkCSVReader.readAll()
                                wordLinks.forEach { wl ->
                                    termToWordLinkMap[wl.term] = wl
                                }
                            }
                        }
                    }
                }
            }
            catch (exception: Exception) {
                Toast.makeText(context, R.string.wordlinks_csv_read_error, Toast.LENGTH_SHORT).show()
            }
        }else{
            // let user know there was no valid CSV file
            Toast.makeText(context, R.string.wordlinks_no_csv_file, Toast.LENGTH_LONG).show()
        }
    }

    private fun importWordLinksFromJsonFiles(context: Context, wordLinksDir: DocumentFile){
        if(wordLinksDir.findFile(WORD_LINKS_JSON_FILE) != null) {
            try {
                wordLinkListFromJson(context)?.wordLinks?.forEach { wl ->
                    if (termToWordLinkMap.containsKey(wl.term)) {
                        termToWordLinkMap[wl.term]?.wordLinkRecordings = wl.wordLinkRecordings
                        termToWordLinkMap[wl.term]?.chosenWordLinkFile = wl.chosenWordLinkFile
                    } else {
                        termToWordLinkMap[wl.term] = wl
                    }
                }
            }
            catch(exception: Exception) {
                Toast.makeText(context, R.string.wordlinks_json_read_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mapTermFormsToTerms() {
        for (wl in termToWordLinkMap.values) {
            val term = wl.term
            termFormToTermMap[term.toLowerCase()] = term
            for (termForm in wl.termForms) {
                termFormToTermMap[termForm.toLowerCase()] = term
            }
        }
    }

    private fun buildWLSTree() {
        for (termForm in termFormToTermMap.keys) {
            WLSTree.insertTerm(termForm)
        }
    }

    fun addDemoToWorkspace(context: Context) {
        //check if the demo folder already exists in the Workspace.  If not, add the demo
        if (!workspaceRelPathExists(context,DEMO_FOLDER)) {
            val assetManager = context.assets
            var files: MutableList<String>? = null
            try {
                files = assetManager.list(DEMO_FOLDER)!!.toMutableList()
                files.add("$PROJECT_DIR/$PROJECT_FILE")
            } catch (e: IOException) {
                Log.e("workspace", "Failed to get demo assets.", e)
                return
            }
            for (filename in files) {
                try {
                    val instream = assetManager.open("$DEMO_FOLDER/$filename")
                    val outstream = getChildOutputStream(context, "$DEMO_FOLDER/$filename")
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (instream.read(buffer).also { read = it } != -1) {
                        outstream!!.write(buffer, 0, read)
                    }
                    outstream?.close()
                    instream.close()
                } catch (e: Exception) {
                    Log.e("workspace", "Failed to copy demo asset file: $filename", e)
                }
            }

            pathOf(DEMO_FOLDER)?.also { path ->
                buildStory(context, path)?.also { story ->
                    Stories.add(story)
                    sortStoriesByTitle()
                }
            }
        }
    }
    fun addWordLinksCSVFileToWorkspace(context: Context, csvFileName: String) {
        // DKH - 11/19/2021 Issue #613 Create Word Links CSV file if it does not exist
        // During compile time, the file app/src/main/assets/wordlinks.csv is compiled into
        // the APK.  This routine extracts that file and places that file in the
        // Worklinks directory.
        // This routine is called anytime a ".csv" file cannot be found in the workdlinks directory
        val assetManager = context.assets

        try {
            // open wordlinks.csv located in the APK
            val instream = assetManager.open(csvFileName)
            // Create the worklinks.csv file in the wordlinks directory
            val outstream = getChildOutputStream(context, "$WORD_LINKS_DIR/$csvFileName")
            val buffer = ByteArray(1024)
            var read: Int
            // copy input to output 1024 bytes at a time
            while (instream.read(buffer).also { read = it } != -1) {
                outstream!!.write(buffer, 0, read)
            }
            outstream?.close() // close output stream
            instream.close() // close input stream
        } catch (e: Exception) {
            Log.e("workspace", "Failed to copy wordlinks CSV asset file: $csvFileName", e)
        }

    }
    fun pathOf(name: String): DocumentFile? {
        return workdocfile.listFiles().find { it.name == name }
    }

    fun clearWorkspace(){
        workdocfile = DocumentFile.fromFile(File(""))
    }

    fun storyFiles(): List<DocumentFile> {
        return storyDirectories().plus(storyBloomFiles())
    }

    private fun storyDirectories(): List<DocumentFile> {
        // Improperly counting worklinks or video folder as a template. remove from list
        var storyList = workdocfile.listFiles().toMutableList()
        var idx: Int = 0
        var tmpSize = storyList.size - 1
        for (idx in tmpSize downTo 0)
        {
            if (storyList[idx].name == WORD_LINKS_DIR || storyList[idx].name == VIDEO_DIR) {
                storyList.removeAt(idx)
            }
        }
        return storyList.filter { it.isDirectory }
    }

    private fun storyBloomFiles(): List<DocumentFile> {
        return  workdocfile.listFiles().filter { isZipped(it.name) }
    }

    fun buildStory(context: Context, storyPath: DocumentFile): Story? {
        return unzipIfZipped(context, storyPath, workdocfile.listFiles())
                ?.let { storyFolder -> pathOf(storyFolder) }
                ?.let { storyPath -> parseStoryIfPresent(context, storyPath) }
                ?.let { story -> migrateStory(context, story) }
    }

    fun buildPhases(): List<Phase> {
        //update phases based upon registration selection
        return when(registration.getString("consultant_location_type")) {
            "Remote" -> Phase.getRemotePhases()
            else -> Phase.getLocalPhases()
        }
    }

    fun deleteVideo(context: Context, path: String){
        activeStory.outputVideos.remove(path)
        deleteWorkspaceFile(context, "$VIDEO_DIR/$path")
    }

    fun isLocalCreditsChanged(context: Context) : Boolean {
        val orgLCText = context.getString(R.string.LC_starting_text)
        return activeStory.localCredits != orgLCText
    }

    fun getSongFilename() : String{
        for (s in activeStory.slides){
            if(s.slideType == SlideType.LOCALSONG){
                if(s.chosenVoiceStudioFile != "") return s.chosenVoiceStudioFile
                if(s.chosenTranslateReviseFile != "") return s.chosenTranslateReviseFile
            }
        }
        return ""
    }

    fun goToNextPhase() : Boolean {
        if(activePhaseIndex == -1) return false //phases not initialized
        if(activePhaseIndex >= phases.size - 1) {
            activePhaseIndex = phases.size - 1
            return false
        }
        activePhaseIndex++
        activePhase = phases[activePhaseIndex]
        //there was a successful phase change!
        return true
    }

    fun goToPreviousPhase() : Boolean {
        if(activePhaseIndex == -1) return false //phases not initialized
        if(activePhaseIndex <= 0) {
            activePhaseIndex = 0
            return false
        }
        activePhaseIndex--
        activePhase = phases[activePhaseIndex]
        //there was a successful phase change!
        return true
    }

    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle()){
        params.putString("phone_id", Secure.getString(context.contentResolver,
                Secure.ANDROID_ID))
        params.putString("story_number", activeStory.titleNumber)
        params.putString("ethnolog", registration.getString("ethnologue", " "))
        params.putString("lwc", registration.getString("lwc", " "))
        params.putString("translator_email", registration.getString("translator_email", " "))
        params.putString("trainer_email", registration.getString("trainer_email", " "))
        params.putString("consultant_email", registration.getString("consultant_email", " "))
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun saveLogToFile(context: Context, pText: String) {
        val filePath = "logFile.txt" // location of file
        val folder:File? = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val logFile = File(folder?.absolutePath, filePath)
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        val buf: BufferedWriter = BufferedWriter(FileWriter(logFile, true))

        val currentDateTimeString: String? = DateFormat.getDateTimeInstance().format(Date())
        buf.append("$currentDateTimeString ")
        buf.append(pText)
        buf.newLine()
        buf.close()
    }

    fun sortStoriesByTitle() {
        Stories.sortBy { it.title }
    }

    fun checkForInternet(context: Context): Boolean {
        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    fun processStoryApproval() {
        // Approve story if all slides in the story have been approved.
        var allApproved = true
        var story = Workspace.activeStory
        for (slide in story.slides) {
            if ((slide.slideType == SlideType.FRONTCOVER ||
                        slide.slideType == SlideType.NUMBEREDPAGE ||
                        slide.slideType == SlideType.LOCALSONG) &&
                !slide.isApproved
            ) {
                allApproved = false
                break  // found at least one so need to keep looking
            }
        }
        story.isApproved = allApproved
    }

    fun processReceivedApprovals() {
        // This could be being built while processed, so remove elements from front one by one as
        // elements may be added on the back end
        while (approvalList.size > 0) {
            var approval = approvalList.removeAt(0)
            for (story in Stories) {
                if (story.remoteId == approval.storyId && approval.slideNumber >= 0 && approval.slideNumber < story.slides.size) {
                    story.slides[approval.slideNumber].isApproved = approval.approvalStatus
                    break
                }
            }
            if (approval.timeSent > lastReceivedTimeSent) {
                lastReceivedTimeSent = approval.timeSent
            }
            processStoryApproval()
        }
    }
}
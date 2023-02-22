package org.sil.storyproducer.model

import WordLinksCSVReader
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings.Secure
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.analytics.FirebaseAnalytics
import org.sil.storyproducer.App
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.getFreeExternalMemorySize
import org.sil.storyproducer.tools.getFreeInternalMemorySize
import org.sil.storyproducer.tools.getMaxFreeExternalMemoryFile
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


internal const val SLIDE_NUM = "CurrentSlideNum"
internal const val DEMO_FOLDER = "000 Unlocked demo"
internal const val AUDIO_FOLDER = "audio"
internal const val PHASE = "Phase"

internal const val WORD_LINKS_DIR = "wordlinks"
internal const val WORD_LINKS_JSON_FILE = "wordlinks.json"
internal const val WORD_LINKS_CLICKED_TERM = "ClickedTerm"
internal const val WORD_LINKS_SLIDE_NUM = "CurrentSlideNum"
// DKH - 11/19/2021 Issue #611 Allow CSV file to have different names
// Use Regex expression when looking for CSV word links file
internal val WORK_LINKS_CSV_REGEX_STRING = "(?i)wordlinks.*\\.csv"
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
    const val URL_FOR_TEMPLATES = "https://drive.google.com/drive/folders/1CxpggJUJ6QPnNgb3Veh9r7SWiLfPKCDj?usp=sharing"
    // These are the place holder strings in the "Welcome Screen" html.  Before displaying the
    // "Welcome Screen", replace this place holder strings with the URL_FOR_TEMPLATES
    const val URL_FOR_TEMPLATES_PLACE_HOLDER = "URL_FOR_TEMPLATES_PLACE_HOLDER"
    // End Issue #571

    val NUM_BYTES_NEEDED_FOR_DEMO_STORY = 1024 * 1024 * 10L;
    val SP_TEMPLATES_FOLDER_NAME = "SP Templates"

    var workdocfile = DocumentFile.fromFile(File(""))
        set(value) {
            field = value
            prefs?.edit()?.putString("workspace", field.uri.toString())?.apply()
        }
    val Stories: MutableList<Story> = mutableListOf()   // the main list of Stories
    val asyncAddedStories: MutableList<Story> = mutableListOf() // Used for adding Stories in a background thread
    var registration: Registration = Registration()
    var phases: List<Phase> = ArrayList()
    var activePhaseIndex: Int = -1
    var isInitialized = false
    var prefs: SharedPreferences? = null
    // DKH - 05/12/2021
    // Issue #573: SP will hang/crash when submitting registration
    // This flag indicates whether MainActivity should call the RegistrationActivity to allow
    // the user to update the registration
    // This is set in BaseController function onStoriesUpdated()
    var showRegistration = false

    // set if user skipped the registration process - so that they are not nagged too much
    // when updateStories() is called by the BL Download Activity
    var showRegistrationSkiped = true // now always skip startup registration dialog (was: false)

    // word links
    lateinit var activeWordLink: WordLink
    var termToWordLinkMap: MutableMap<String, WordLink> = mutableMapOf()
    var termFormToTermMap: MutableMap<String, String> = mutableMapOf()
    var WLSTree = WordLinkSearchTree()



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

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorkspace(activity: Activity) {
        //first, see if there is already a workspace in shared preferences
        prefs = activity.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        if (setupWorkspacePath(activity, Uri.parse(prefs!!.getString("workspace", ""))))
            isInitialized = true
        firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
    }

    fun createAppSpecificWorkspace(): Boolean {
        // Create an App-specific internal or external storage folder for an initial demo
        // https://developer.android.com/training/data-storage
        // https://developer.android.com/training/data-storage/app-specific
        var appSpecificDocsDir: File? = null
        var appSpecificTemplateDir : File? = null
        // Check external memory first then internal memory for enough free space for demo
        val freeExtFile = getMaxFreeExternalMemoryFile()    // may be hosted on the same storage volume as internal
        val freeExtMem = getFreeExternalMemorySize()        // may also be hosted on the same storage volume as internal
        val freeIntMem = getFreeInternalMemorySize()
        if (freeExtFile != null && freeExtFile.freeSpace!! >= NUM_BYTES_NEEDED_FOR_DEMO_STORY) {
            appSpecificDocsDir = freeExtFile
        } else if (freeExtMem >= NUM_BYTES_NEEDED_FOR_DEMO_STORY) {
            appSpecificDocsDir = App.appContext.getExternalFilesDir(null)
        } else if (freeIntMem >= NUM_BYTES_NEEDED_FOR_DEMO_STORY) {
            appSpecificDocsDir = App.appContext.filesDir
        }
        if (appSpecificDocsDir != null)
            appSpecificTemplateDir = File(appSpecificDocsDir, SP_TEMPLATES_FOLDER_NAME)
        if (appSpecificTemplateDir != null) {
            // we have space on this volume for a demo story
            appSpecificTemplateDir.mkdirs()
            if (appSpecificTemplateDir.isDirectory) {
                // if we managed to create an app-specific workspace folder
                if (setupWorkspacePath(App.appContext, Uri.fromFile(appSpecificTemplateDir))) {
                    isInitialized = true  // app-specific workspace successful
                    // Add the demo for new users
                    addDemoToWorkspace(App.appContext)
                    return true // created ok
                }
            }
        }

        return false;
    }

    fun setupWorkspacePath(context: Context, uri: Uri) : Boolean {
        if (uri.toString().isEmpty())
            return false

        try {
            // Issue 539 - Reset Story info to detach from current Story, if any
            activeStory = emptyStory()

            // Initiate new workspace path
            workdocfile = getDocumentFileFromUri(context, uri)

            // Load the registration info
            registration.load(context)

            // load in the Word Links database
            importWordLinks(context)

            return true;

        } catch (e: Exception) {
            Log.e("setupWorkspacePath", "Error setting up new workspace path!", e)
        }

        return false
    }
    // DKH - 01/26/2022 Issue #571: Add a menu item for accessing templates from Google Drive
    // A new menu item was added that opens a URL for the user to download templates.
    // This is used in both the MainActivity menu (Story Templates display) and the Phase menus
    fun startDownLoadMoreTemplatesActivity(context: Context){
        val openURL = Intent(Intent.ACTION_VIEW)
        openURL.data = Uri.parse(Workspace.URL_FOR_TEMPLATES)
        context.startActivity(openURL)
    }

    private fun importWordLinks(context: Context) {
        var wordLinksDir = workdocfile.findFile(WORD_LINKS_DIR)
        var csvFileName : String? = null  // csv file name if found

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
            for (filename in wordLinksDir.listFiles()) {
                // look for a wordlinks csv file
                if((filename.name)?.contains(WORD_LINKS_CSV_REGEX)!!){
                    csvFileName = filename.name  // found csv file
                    break
                }
            }
        }

        if (wordLinksDir != null) {
            // DKH - 11/19/2021 Issue #613 Create Word Links CSV file if it does not exist
            // BW 2/21/2022 #622 There is no default CSV file or LWC.
            // In a future version, the correct CSV would be based on the user's selection
                // of the LWC for this workspace.
            // if(csvFileName == null)
            //     addWordLinksCSVFileToWorkspace(context, csvFileNameForSelectedLWC)

            if(csvFileName != null) {
                // Process the CSV file, read the Json file, and map the terms
                importWordLinksFromCSV(context, wordLinksDir)
                importWordLinksFromJsonFiles(context, wordLinksDir)
                mapTermFormsToTerms()
                buildWLSTree()
            }
        }else{
            Log.e("workspace", "Failed to create word links directory: $WORD_LINKS_DIR")
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
            var files: MutableList<String>
            var audiofiles: List<String>
            try {
                files = assetManager.list(DEMO_FOLDER)!!.toMutableList()
                files.remove(PROJECT_DIR)
                files.remove(AUDIO_FOLDER)
                files.add("$PROJECT_DIR/$PROJECT_FILE")
                audiofiles = assetManager.list("$DEMO_FOLDER/$AUDIO_FOLDER")!!.toList()
                for(filename in audiofiles)
                    files.add("$AUDIO_FOLDER/$filename")
            } catch (e: IOException) {
                Log.e("workspace", "SP::Failed to get demo assets.", e)
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
                    Log.e("workspace", "SP::Failed to copy demo asset file: $filename", e)
                }
            }

//            pathOf(DEMO_FOLDER)?.also { path ->
//                buildStory(context, path)?.also { story ->
//                    Stories.add(story)
//                    sortStoriesByTitle()
//                }
//            }
        }
    }
    fun addWordLinksCSVFileToWorkspace(context: Context, csvFileNameForSelectedLWC: String) {
        // DKH - 11/19/2021 Issue #613 Create Word Links CSV file if it does not exist
        // BW 2/21/2022 #622 There is no default CSV file or LWC.
        // At compile time, files such as app/src/main/assets/WordLinks - English.csv
        // may be compiled into the APK.  This routine extracts the given file and writes that
        // file in the Worklinks directory.
        val assetManager = context.assets

        try {
            // open wordlinks.csv located in the APK
            val instream = assetManager.open(csvFileNameForSelectedLWC)
            // Create the worklinks.csv file in the wordlinks directory
            val outstream = getChildOutputStream(context, "$WORD_LINKS_DIR/$csvFileNameForSelectedLWC")
            val buffer = ByteArray(1024)
            var read: Int
            // copy input to output 1024 bytes at a time
            while (instream.read(buffer).also { read = it } != -1) {
                outstream!!.write(buffer, 0, read)
            }
            outstream?.close() // close output stream
            instream.close() // close input stream
        } catch (e: Exception) {
            Log.e("workspace", "Failed to copy wordlinks asset file: $csvFileNameForSelectedLWC", e)
        }

    }
    fun pathOf(name: String): DocumentFile? {
        return workdocfile.listFiles().find { it.name == name }
    }

    fun clearWorkspace(){
        workdocfile = DocumentFile.fromFile(File(""))
    }

    fun storyFilesToScanOrUnzip(): List<DocumentFile> {
        // made up of already installed stories +
        //      story archives downloaded in story template dir +
        //      story archives downloaded in external app storage download dir
        // while checking that the story does not already exist
        return storyDirectories()
            .run { this.plus(storyDownloadedBloomFiles(this)) }
            .run { this.plus(storyBloomFiles(this)) }
    }

    private fun storyDirectories(): List<DocumentFile> {
        // We don't want the videos and wordlinks folders included in the list of story folders
        val non_story_folders = arrayOf(VIDEO_DIR, WORD_LINKS_DIR)
        return workdocfile.listFiles().filter {
            it.isDirectory && (!non_story_folders.contains(it.name))
        }
    }

    private fun storyBloomFiles(current : List<DocumentFile>): List<DocumentFile> {
        var installStories = workdocfile.listFiles().filter { isZipped(it.name) }
        var installFilesList : MutableList<DocumentFile> = ArrayList()
        for (i in 0 until installStories.size) {
            val installFilename = installStories[i].name
            val installBaseName = installFilename?.substringBeforeLast('.')
            // don't add if already in the workspace
            if (current.find { (isZipped(it.name) && it.name?.substringBeforeLast('.') == installBaseName) ||
                        it.name == installBaseName } == null)
                installFilesList.add(installStories[i])
        }
        return installFilesList
    }

    private fun storyDownloadedBloomFiles(current : List<DocumentFile>): List<DocumentFile> {
        var fileDownloadDir = File(bloomSourceAutoDLDir())
        var blExt = bloomSourceZipExt()
        val dlFiles = fileDownloadDir.listFiles()?.filter { it.name.endsWith(blExt) }
        var dlFilesList : MutableList<DocumentFile> = ArrayList()
        for (i in 0 until dlFiles?.size!!) {
            val installFilename = dlFiles[i].name
            val installBaseName = installFilename.substringBeforeLast('.')
            // don't add if already in the workspace
            if (current.find { (isZipped(it.name) && it.name?.substringBeforeLast('.') == installBaseName) ||
                                    it.name == installBaseName } == null)
                dlFilesList.add(DocumentFile.fromFile(dlFiles[i]))
        }
        return dlFilesList
    }

    fun buildStory(context: Context, storyPath: DocumentFile): Story? {
        return unzipIfZipped(context, storyPath, workdocfile.listFiles())
                ?.let { storyFolder -> pathOf(storyFolder) }
                ?.let { storyPath1 -> parseStoryIfPresent(context, storyPath1) }
                ?.let { story -> migrateStory(context, story) }
        /*  Daniel March and BW figured this is what the lambdas are doing...
        var unzipped = unzipIfZipped(context, storyPath, workdocfile.listFiles());
        if(unzipped != null)
        {
            var pathUnzipped = pathOf(unzipped);
            if(pathUnzipped != null)
            {
                var story = parseStoryIfPresent(context, pathUnzipped)
                if (story != null)
                    return migrateStory(context, story)
            }
        }
        return null
        */
    }

    fun buildPhases(): List<Phase> {
        //update phases based upon registration selection
        return when(registration.getString("consultant_location_type")) {
            "remote" -> Phase.getRemotePhases()
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

    fun sortStoriesByTitle() {
        Stories.sortBy { it.title }
    }

}



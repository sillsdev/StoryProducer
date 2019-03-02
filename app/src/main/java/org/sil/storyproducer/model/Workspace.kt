package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v4.provider.DocumentFile
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.deleteStoryFile
import java.io.File
import java.io.InputStreamReader
import java.util.*

internal const val KEYTERMS_DIR = "keyterms"
internal const val KEYTERMS_CSV_FILE = "keyterms.csv"
internal const val KEYTERMS_JSON_FILE = "keyterms.json"
internal const val PHASE = "Phase"
internal const val CLICKED_TERM = "ClickedTerm"

object Workspace{
    var workspace: DocumentFile = DocumentFile.fromFile(File(""))
        set(value) {
            field = value
            prefs?.edit()?.putString("workspace", field.uri.toString())?.apply()
            storiesUpdated = false
        }
    val Stories: MutableList<Story> = mutableListOf()
    var storiesUpdated = false
    var registration: Registration = Registration()
    var phases: List<Phase> = ArrayList()
    var activePhaseIndex: Int = -1
        private set
    var isInitialized = false
    var prefs: SharedPreferences? = null

    var activeStory: Story = emptyStory()
    set(value){
        field = value
        //You are switching the active story.  Recall the last phase and slide.
        activePhase = Phase(value.lastPhaseType)
        activeSlideNum = value.lastSlideNum
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
    get(){
        return if(activePhase.phaseType == PhaseType.KEYTERM)
            KEYTERMS_DIR
        else
            activeStory.title
    }

    val activeDir: String
    get(){
        return if(activePhase.phaseType == PhaseType.KEYTERM)
            activeKeyterm.term
        else
            PROJECT_DIR
    }
    val activeFilenameRoot: String
    get() {
        return if(activePhase.phaseType == PhaseType.KEYTERM){
            Workspace.activeKeyterm.term
        }else {
            "${activePhase.getShortName()}${ Workspace.activeSlideNum }"
        }
    }

    lateinit var activeKeyterm: Keyterm
    var activeSlideNum: Int = -1
    set(value){
        if(value >= 0 && value < activeStory.slides.size) field = value
    }
    val activeSlide: Slide?
    get(){
        if(activeStory.title == "") return null
        return activeStory.slides[activeSlideNum]
    }
    var termToKeyterm: MutableMap<String, Keyterm> = mutableMapOf()
    var termFormToTerm: MutableMap<String, String> = mutableMapOf()
    var keytermSearchTree = KeytermSearchTree()

    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        setupWorkspacePath(context,Uri.parse(prefs!!.getString("workspace","")))
        isInitialized = true
    }

    fun setupWorkspacePath(context: Context, uri: Uri){
        try {
            workspace = DocumentFile.fromTreeUri(context, uri)!!
            registration.load(context)
        } catch ( e : Exception) {}
        updateStories(context)
        importKeyterms(context)
    }

    fun clearWorkspace(){
        workspace = DocumentFile.fromFile(File(""))

    }

    private fun updateStories(context: Context) {
        //Iterate external files directories.
        //for all files in the workspace, see if they are folders that have templates.
        if(storiesUpdated) return
        if(workspace.isDirectory){
            //find all stories
            Stories.removeAll(Stories)
            for (storyPath in workspace.listFiles()) {
                //TODO - check storyPath.name against titles.
                if (storyPath.isDirectory) {
                    val story = parseStoryIfPresent(context,storyPath)
                    if (story != null) {
                        Stories.add(story)
                    }
                }
            }
        }
        //sort by title.
        Stories.sortBy{it.title}
        //update phases based upon registration selection
        phases = when(registration.getString("consultant_location_type")) {
            "remote" -> Phase.getRemotePhases()
            else -> Phase.getLocalPhases()
        }
        activePhaseIndex = 0
        updateStoryLocalCredits(context)
        storiesUpdated = true
    }

    fun deleteAudioFileFromList(context: Context, name: String, position: Int) {
        val filenames = activePhase.getRecordedAudioFiles(activeSlideNum)!!
        filenames.removeAt(position)
        if (activePhase.phaseType == PhaseType.KEYTERM) {
            activeKeyterm.keytermRecordings.removeAt(position)
        }
        deleteStoryFile(context, "$activeDir/$name")
    }

    private fun importKeyterms(context: Context) {
        val keytermsDirectory = workspace.findFile(KEYTERMS_DIR)

        if(keytermsDirectory != null) {
            importKeytermsFromCsvFile(context, keytermsDirectory)
            importKeytermsFromJsonFiles(context, keytermsDirectory)
            mapTermFormsToTerms()
            buildKeytermSearchTree()
        }
    }

    private fun importKeytermsFromCsvFile(context: Context, keytermsDirectory: DocumentFile){
        val keytermsFile = keytermsDirectory.findFile(KEYTERMS_CSV_FILE)
        if(keytermsFile != null) {
            try {
                context.contentResolver.openFileDescriptor(keytermsFile.uri, "r").use{ pfd ->
                    ParcelFileDescriptor.AutoCloseInputStream(pfd).use{ inputStream ->
                        InputStreamReader(inputStream).use{ streamReader ->
                            KeytermCsvReader(streamReader).use { keytermCsvReader ->
                                val keyterms = keytermCsvReader.readAll()
                                keyterms.forEach { keyterm ->
                                    termToKeyterm[keyterm.term] = keyterm
                                }
                            }
                        }
                    }
                }
            }
            catch(exception: Exception) {
                Toast.makeText(context, "Parsing keyterm CSV file failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importKeytermsFromJsonFiles(context: Context, keytermsDirectory: DocumentFile){
        if(keytermsDirectory.findFile(KEYTERMS_JSON_FILE) != null) {
            try {
                val keytermList = keytermListFromJson(context)
                keytermList?.keyterms?.forEach { keyterm ->
                    if (termToKeyterm.containsKey(keyterm.term)) {
                        termToKeyterm[keyterm.term]?.keytermRecordings = keyterm.keytermRecordings
                        termToKeyterm[keyterm.term]?.chosenKeytermFile = keyterm.chosenKeytermFile
                    } else {
                        termToKeyterm[keyterm.term] = keyterm
                    }
                }
            }
            catch(exception: Exception) {
                Toast.makeText(context, "Parsing keyterm JSON file failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mapTermFormsToTerms(){
        for(keyterm in termToKeyterm.values){
            val term = keyterm.term
            termFormToTerm[term.toLowerCase()] = term
            for(termForm in keyterm.termForms){
                termFormToTerm[termForm.toLowerCase()] = term
            }
        }
    }

    private fun buildKeytermSearchTree(){
        for(termForm in termFormToTerm.keys){
            keytermSearchTree.insertTerm(termForm)
        }
    }

    fun updateStoryLocalCredits(context: Context) {
        for(story in Stories){
            for(slide in story.slides){
                if(slide.slideType == SlideType.LOCALCREDITS) { //local credits
                    if(slide.translatedContent == ""){
                        slide.translatedContent = context.getString(R.string.LC_starting_text)
                    }
                }
            }
        }
    }

    fun goToNextPhase() : Boolean {
        if(activePhaseIndex == -1) return false //phases not initizialized
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
        if(activePhaseIndex == -1) return false //phases not initizialized
        if(activePhaseIndex <= 0) {
            activePhaseIndex = 0
            return false
        }
        activePhaseIndex--
        activePhase = phases[activePhaseIndex]
        //there was a successful phase change!
        return true
    }
}



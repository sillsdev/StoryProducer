package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

import android.support.v4.provider.DocumentFile
import java.io.File
import java.io.FileReader
import java.util.*
import org.sil.storyproducer.R

internal const val KEYTERMS_DIR = "keyterms"
internal const val KEYTERMS_FILE = "keyterms.csv"
internal const val KEYTERMS_JSON_FILE = "keyterms.json"

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
    var keyterms: MutableList<Keyterm> = mutableListOf()
    var termsToKeyterms: MutableMap<String, Keyterm> = mutableMapOf()

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

    fun updateStories(context: Context) {
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

    fun importKeyterms(context: Context) {
        val keytermsDirectory = workspace.findFile(KEYTERMS_DIR)

        if(keytermsDirectory != null) {
            importKeytermsFromCsvFile(context, keytermsDirectory)
            importKeytermsFromJsonFiles(context, keytermsDirectory)
        }
    }

    private fun importKeytermsFromCsvFile(context: Context, keytermsDirectory: DocumentFile){
        val keytermsFile = keytermsDirectory.findFile(KEYTERMS_FILE)
        if(keytermsFile != null) {
            val fileDescriptor = context.contentResolver.openFileDescriptor(keytermsFile.uri, "r")?.fileDescriptor
            val fileReader = FileReader(fileDescriptor)
            val keytermCsvReader = KeytermCsvReader(fileReader)

            keyterms = keytermCsvReader.readAll().toMutableList()
            termsToKeyterms = mapTermsToKeyterms(keyterms)
        }
    }

    private fun mapTermsToKeyterms(keyterms: List<Keyterm>): MutableMap<String, Keyterm>{
        val termsToKeyterms: MutableMap<String, Keyterm> = mutableMapOf()

        for(keyterm: Keyterm in keyterms) {
            termsToKeyterms[keyterm.term] = keyterm
            for (termForm in keyterm.termForms) {
                termsToKeyterms[termForm] = keyterm
            }
        }

        return termsToKeyterms
    }

    private fun importKeytermsFromJsonFiles(context: Context, keytermsDirectory: DocumentFile){
        for (keytermItem in keytermsDirectory.listFiles()) {
            val keyterm = parseKeytermIfPresent(context, keytermItem)
            if (keyterm != null) {
                if(termsToKeyterms.containsKey(keyterm.term)) {
                    termsToKeyterms[keyterm.term]?.backTranslations = keyterm.backTranslations
                    termsToKeyterms[keyterm.term]?.chosenKeytermFile = keyterm.chosenKeytermFile
                }
                else{
                    keyterms.add(keyterm)
                    termsToKeyterms[keyterm.term] = keyterm
                    for (termForm in keyterm.termForms) {
                        termsToKeyterms[termForm] = keyterm
                    }
                }
            }
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



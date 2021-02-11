package org.sil.storyproducer.model

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Secure
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.analytics.FirebaseAnalytics
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.deleteWorkspaceFile
import org.sil.storyproducer.tools.file.getChildOutputStream
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import java.io.File
import java.io.IOException
import java.util.*


internal const val SLIDE_NUM = "CurrentSlideNum"
internal const val DEMO_FOLDER = "000 Unlocked demo story Storm"

object Workspace{
    var workdocfile = DocumentFile.fromFile(File(""))
        set(value) {
            field = value
            prefs?.edit()?.putString("workspace", field.uri.toString())?.apply()
        }
    val Stories: MutableList<Story> = mutableListOf()
    var registration: Registration = Registration()
    var phases: List<Phase> = ArrayList()
    var activePhaseIndex: Int = -1
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
    get(){return activeStory.title }

    val activeDir: String = PROJECT_DIR
    val activeFilenameRoot: String
    get() {
        return "${activePhase.getFileSafeName()}${ Workspace.activeSlideNum }"
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
        setupWorkspacePath(activity, Uri.parse(prefs!!.getString("workspace", "")))
        isInitialized = true
        firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
    }

    fun setupWorkspacePath(context: Context, uri: Uri) {
        try {
            // Issue 539 - Reset Story info to detach from current Story, if any
            activeStory = emptyStory()

            // Initiate new workspace path
            workdocfile = DocumentFile.fromTreeUri(context, uri)!!
            registration.load(context)
        } catch (e: Exception) {
            Log.e("setupWorkspacePath", "Error setting up new workspace path!", e)
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
        return workdocfile.listFiles().filter { it.isDirectory }
    }

    private fun storyBloomFiles(): List<DocumentFile> {
        return workdocfile.listFiles().filter { isZipped(it.name) }
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



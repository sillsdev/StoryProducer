package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor

import java.io.File
import java.util.*
import android.support.v4.provider.DocumentFile
import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.file.LogFiles
import java.io.InputStream
import java.io.OutputStream

object Workspace{
    var workspace: DocumentFile = DocumentFile.fromFile(File(""))
        set(value) {
            if (value.isDirectory) {
                field = value
                val tempPrefs = prefs
                if (tempPrefs != null)
                    tempPrefs.edit().putString("workspace", field.toString()).apply()
            }
        }
    val Stories: MutableList<Story> = ArrayList()
    var registration: Registration = Registration()
    var phases: List<Phase> = ArrayList()
    var activePhaseIndex: Int = -1
        private set
    var isInitialized = false
    var prefs: SharedPreferences? = null

    var activeStory: Story = emptyStory()
    set(value){
        field = value
        activePhase = Phase(PhaseType.LEARN)
        //TODO do we want to save slide number in prefernces?
        activeSlideNum = 0
    }
    var activePhase: Phase = Phase(PhaseType.LEARN)
        set(value){
            field = value
            activeSlideNum = -1
            for((i,p) in phases.withIndex()){
                if(p.phaseType == value.phaseType) activeSlideNum = i
            }
        }
    var activeSlideNum: Int = -1
    set(value){
        if(value >= 0 && value < activeStory.slides.size) field = value
    }
    val activeSlide: Slide?
    get(){
        if(activeStory.title == "") return null
        return activeStory.slides[activeSlideNum]
    }

    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        //get registration from the json file, if there is any.
        //TODO get workspace from preferences - after a "change workspace" option has been implemented.
        registration.load(context)
        updateStories(context)
        LogFiles.init(context)
        isInitialized = true
    }


    fun completeRegistration(context: Context){
        registration.save(context)
        //update phases based upon registration selection
        phases = when(registration.getString("consultant_location_type")) {
            "remote" -> Phase.getRemotePhases()
            else -> Phase.getLocalPhases()
        }
        activePhaseIndex = 0
    }

    fun updateStories(context: Context) {
        //Iterate external files directories.
        //for all files in the workspace, see if they are folders that have templates.
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

    fun goToNextSlide() : Boolean {
        if(activeStory.title == "") return false
        if(activeSlide == null) return false
        if(activeSlideNum < 0) return false
        if(activeSlideNum >= activeStory.slides.size - 1) return false
        activeSlideNum++
        return true
    }
}



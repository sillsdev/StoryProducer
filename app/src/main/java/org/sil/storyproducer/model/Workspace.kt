package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Secure
import android.support.v4.content.res.TypedArrayUtils.getString
import android.support.v4.provider.DocumentFile
import android.util.Log;
import com.android.volley.Request
import com.google.firebase.analytics.FirebaseAnalytics
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.deleteWorkspaceFile
import org.sil.storyproducer.tools.Network.paramStringRequest
import java.io.File
import java.util.*
import kotlin.math.max

internal const val SLIDE_NUM = "CurrentSlideNum"
internal const val PHASE_TYPE = "CurrentPhaseType"

object Workspace {
    var workspace: DocumentFile = DocumentFile.fromFile(File(""))
        set(value) {
            field = value
            prefs?.edit()?.putString("workspace", field.uri.toString())?.apply()
            storiesUpdated = false
        }
    val Stories: MutableList<Story> = mutableListOf()
    var storiesUpdated = false
    var registration: Registration = Registration()
    var phases: List<PhaseType> = ArrayList()
    var activePhaseIndex: Int = -1
        private set
    var isInitialized = false
    var prefs: SharedPreferences? = null

    var activeStory: Story = emptyStory()
        set(value) {
            field = value
            //You are switching the active story.  Recall the last phase and slide.
            activePhase = value.lastPhaseType
            activeSlideNum = value.lastSlideNum
        }
    var activePhase = PhaseType.LEARN
        set(value) {
            field = value
            activePhaseIndex = -1
            for ((i, p) in phases.withIndex()) {
                if (p == value) activePhaseIndex = i
            }
        }
    val activeDirRoot: String
        get() {
            return activeStory.title
        }

    val activeDir: String = PROJECT_DIR
    val activeFilenameRoot: String
        get() {
            return "${activePhase.getShortName()}${Workspace.activeSlideNum}"
        }

    var activeSlideNum: Int = 0
        set(x) {
            field = if (x >= 0 && x < activeStory.slides.size && activePhase.checkValidDisplaySlideNum(x)) {
                x
            } else {
                0
            }
        }

    val activeSlide: Slide?
        get() {
            if (activeStory.title == "") return null
            return activeStory.slides[activeSlideNum]
        }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private const val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        setupWorkspacePath(context, Uri.parse(prefs!!.getString("workspace", "")))
        isInitialized = true
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle()) {
        params.putString("phone_id", Secure.getString(context.contentResolver,
                Secure.ANDROID_ID))
        params.putString("story_number", activeStory.titleNumber)
        params.putString("ethnolog", registration.projectEthnoCode)
        params.putString("lwc", registration.projectMajorityLanguage)
        params.putString("translator_email", registration.translatorEmail)
        params.putString("trainer_email", registration.trainerEmail)
        params.putString("consultant_email", registration.consultantEmail)
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun setupWorkspacePath(context: Context, uri: Uri) {
        try {
            workspace = DocumentFile.fromTreeUri(context, uri)!!
            registration.load(context)
        } catch (e: Exception) {
        }
        updateStories(context)
    }

    fun clearWorkspace() {
        workspace = DocumentFile.fromFile(File(""))

    }

    private fun updateStories(context: Context) {
        //Iterate external files directories.
        //for all files in the workspace, see if they are folders that have templates.
        if (storiesUpdated) return
        if (workspace.isDirectory) {
            //find all stories
            Stories.removeAll(Stories)
            for (storyPath in workspace.listFiles()) {
                //TODO - check storyPath.name against titles.
                if (storyPath.isDirectory) {
                    val story = parseStoryIfPresent(context, storyPath)
                    if (story != null) {
                        Stories.add(story)
                    }
                }
            }
        }
        //sort by title.
        Stories.sortBy { it.title }
        //update phases based upon registration selection
        phases = when (registration.consultantLocationType) {
            "Remote" -> PhaseType.getRemotePhases()
            else -> PhaseType.getLocalPhases()
        }
        activePhaseIndex = 0
        updateStoryLocalCredits(context)
        storiesUpdated = true
    }

    fun deleteVideo(context: Context, path: String) {
        activeStory.outputVideos.remove(path)
        deleteWorkspaceFile(context, "$VIDEO_DIR/$path")
    }

    fun updateStoryLocalCredits(context: Context) {
        for (story in Stories) {
            for (slide in story.slides) {
                if (slide.slideType == SlideType.LOCALCREDITS) { //local credits
                    if (slide.translatedContent == "") {
                        slide.translatedContent = context.getString(R.string.LC_starting_text)
                    }
                }
            }
        }
    }

    fun isLocalCreditsChanged(context: Context): Boolean {
        var isChanged = false
        val orgLCText = context.getString(R.string.LC_starting_text)
        for (slide in activeStory.slides) {
            if (slide.slideType == SlideType.LOCALCREDITS) { //local credits
                if (slide.translatedContent != orgLCText) {
                    isChanged = true
                }
            }
        }
        return isChanged
    }

    fun getSongFilename(): String? {
        val songSlide = activeStory.slides.firstOrNull { it.slideType == SlideType.LOCALSONG }
        return (songSlide?.dramatizationRecordings?.selectedFile
                ?: songSlide?.draftRecordings?.selectedFile)?.fileName
    }

    fun goToNextPhase(): Boolean {
        if (activePhaseIndex == -1) return false // phases not initizialized
        if (activePhaseIndex >= phases.size - 1) {
            activePhaseIndex = phases.size - 1
            return false
        }
        activePhaseIndex++
        activePhase = phases[activePhaseIndex]
        return true
    }

    fun goToPreviousPhase(): Boolean {
        if (activePhaseIndex == -1) return false
        if (activePhaseIndex <= 0) {
            activePhaseIndex = 0
            return false
        }
        activePhaseIndex--
        activePhase = phases[activePhaseIndex]
        return true
    }


}



package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.app.Activity

import java.io.File
import java.util.*
import android.net.Uri

object Workspace {
    var workspacePath: Uri = Uri.EMPTY
        set(value) {
            if (!Uri.EMPTY.equals(value)) {
                field = value
                if (prefs != null)
                    prefs!!.edit().putString("workspacePath", field.toString()).apply()
            }
        }
    val Stories: MutableList<Story> = ArrayList()
    var isInitialized = false
    var prefs: SharedPreferences? = null

    val SPPROJECT_DIR = "spproject"
    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(activity: Activity) {
        //first, see if there is already a workspace in shared preferences
        prefs = activity.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        isInitialized = true
    }

    fun findStories() {
        //Iterate external files directories.
        //for all files in the workspace, see if they are folders that have templates.
        for (path in workspacePath.list()) {
            val story: Story? = parseStoryIfPresent(File(workspacePath,path))
            if (story != null) Stories.add(story)
        }
    }
}


package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.app.Activity
import android.content.Intent

import java.io.File
import java.util.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.PermissionChecker.checkCallingOrSelfPermission

object Workspace {
    var workspacePath: File = File("")
        set(value) {
            if (value.isDirectory) {
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
        chooseWorkspacePath(activity)
        isInitialized = true
    }

    private fun chooseWorkspacePath(activity: Activity) {
        var wsTemp = ""
        //TODO re-enable this code when you can change the workspace path.
        //if (prefs != null)
        //    wsTemp = prefs!!.getString("workspacePath", "")
        if (wsTemp == "") {
            //There is no worskpace path stored
            //check if there is external permission granted
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            val res = checkCallingOrSelfPermission(activity, permission)
            if (res == PackageManager.PERMISSION_GRANTED) {
                //use the storage access framework

            } else {
                //We have no permissions - set to app space
                workspacePath = activity.cacheDir
            }
            //commit the path chosen
        } else {
            //There is a worskpace path stored.  Set it.
            workspacePath = File(wsTemp)
        }
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


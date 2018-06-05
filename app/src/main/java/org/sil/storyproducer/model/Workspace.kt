package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences

import java.io.File
import java.util.*
import android.support.v4.provider.DocumentFile


object Workspace {
    var workspace: DocumentFile = DocumentFile.fromFile(File(""))
        set(value) {
            if (value.isDirectory) {
                field = value
                if (prefs != null)
                    prefs!!.edit().putString("workspace", field.toString()).apply()
            }
        }
    val Stories: MutableList<Story> = ArrayList()
    var isInitialized = false
    var prefs: SharedPreferences? = null

    val SPPROJECT_DIR = "spproject"
    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        updateWorkspace(context)
        isInitialized = true
    }

    fun updateWorkspace(context: Context) {
        //Iterate external files directories.
        //for all files in the workspace, see if they are folders that have templates.
        if(workspace.isDirectory){
            for (storyPath in workspace.listFiles()) {
                if (storyPath.isDirectory) {
                    val story = parseStoryIfPresent(context,storyPath)
                    if (story != null) Stories.add(story)
                }
            }
        }
    }
}


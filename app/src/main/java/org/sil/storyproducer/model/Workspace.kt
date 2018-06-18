package org.sil.storyproducer.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract

import java.io.File
import java.util.*
import android.support.v4.provider.DocumentFile
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream


object Workspace{
    var workspace: DocumentFile = DocumentFile.fromFile(File(""))
        set(value) {
            if (value.isDirectory) {
                field = value
                if (prefs != null)
                    prefs!!.edit().putString("workspace", field.toString()).apply()
            }
        }
    val Stories: MutableList<Story> = ArrayList()
    val activeStory: Story? = null
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

    fun getImage(context: Context, slideNum: Int, sampleSize: Int = 1, story: Story? = activeStory): Bitmap? {
        if(story == null) return null
        val imName = story.slides[slideNum].imageFile
        val iStream = getChildInputStream(context,imName,story.title)
        if(iStream != null){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeStream(iStream, null, options)
        }
        return null
    }

    fun getText(context: Context, relPath: String, storyTitle: String = "") : String? {
        val iStream = getChildInputStream(context, relPath, storyTitle)
        if (iStream != null)
            return iStream.reader().use {
                        it.readText() }
        return null
    }

    fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "", storyTitle: String = "") : OutputStream? {
        var iTitle = storyTitle
        if (iTitle== ""){
            if(activeStory == null) return null
            iTitle = activeStory.title
        }
        if (!workspace.isDirectory) return null
        //build the document tree if it is needed
        val segments = relPath.split("/")
        var df = workspace.findFile(iTitle)
        var df_new : DocumentFile?
        for (i in 0 .. segments.size-2){
            df_new = df.findFile(segments[i])
            when(df_new == null){
                true ->  df = df.createDirectory(segments[i])
                false -> df = df_new
            }
        }
        //create the file if it is needed
        df_new = df.findFile(segments.last())
        when(df_new == null){
            true -> {
                //find the mime type by extension
                var mType = mimeType
                if(mType == "") {
                    when (File(df.uri.path).extension) {
                        "json" -> mType = "application/json"
                        "mp3"  -> mType = "audio/x-mp3"
                        "wav" -> mType = "audio/w-wav"
                        "txt" -> mType = "plain/text"
                        else -> mType = "*/*"
                    }
                }
                df = df.createFile(mType,segments.last())
            }
            false -> df = df_new
        }
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(df.uri,"w")
        if (pfd == null) return null
        return ParcelFileDescriptor.AutoCloseOutputStream(pfd)
    }

    fun getChildInputStream(context: Context, relPath: String, storyTitle: String? = "") : InputStream? {
        var iTitle = storyTitle
        if (iTitle== ""){
            if(activeStory == null) return null
            iTitle = activeStory.title
        }
        val childUri = Uri.parse(workspace.uri.toString() +
                Uri.encode("/$iTitle/$relPath"))
        //check if the file exists by checking for permissions
        try {
            //TODO Why is DocumentsContract.isDocument not working right?
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(
                    childUri, "r") ?: return null
            return ParcelFileDescriptor.AutoCloseInputStream(pfd)
        } catch (e: Exception) {
            //The file does not exist.
            return null
        }
        return null
    }
}



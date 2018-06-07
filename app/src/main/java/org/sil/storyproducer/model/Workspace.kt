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
        val iStream = getChildInputStream(context,imName,story)
        if(iStream != null){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeStream(iStream, null, options)
        }
        return null
    }

    fun getText(context: Context, relPath: String, story: Story? = activeStory) : String? {
        if(story == null) return null
        val iStream = getChildInputStream(context, relPath, story)
        if (iStream != null) {
            return iStream.reader().use { it.readText() }
        }
        return null
    }


    fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "", story: Story? = activeStory) : OutputStream? {
        if (story == null) return null
        if (!workspace.isDirectory) return null
        //build the document tree if it is needed
        val segments = relPath.split("/")
        var df = workspace.findFile(story.title)
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

    fun getChildInputStream(context: Context, relPath: String, story: Story? = activeStory) : InputStream? {
        if (story == null) return null
        val childUri = Uri.withAppendedPath(workspace.uri,story.title+ "/" + relPath)
        if(File(childUri.path).exists()) {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(childUri, "r")
            if (pfd == null) return null
            return ParcelFileDescriptor.AutoCloseInputStream(pfd)
        }
        return null
    }
}



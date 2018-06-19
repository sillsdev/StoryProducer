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
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

val REGISTRATION_FILENAME = "registration.json"

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
    private var registration: JSONObject = JSONObject()
    val activeStory: Story? = null
    var isInitialized = false
    var prefs: SharedPreferences? = null

    val SPPROJECT_DIR = "spproject"
    val WORKSPACE_KEY = "org.sil.storyproducer.model.workspace"

    fun initializeWorskpace(context: Context) {
        //first, see if there is already a workspace in shared preferences
        prefs = context.getSharedPreferences(WORKSPACE_KEY, Context.MODE_PRIVATE)
        //get registration from the json file, if there is any.
        initRegistration(context)
        updateStories(context)
        isInitialized = true
    }

    fun initRegistration(context: Context) {
        if(workspace.isDirectory) {
            val regString: String? = getText(context,REGISTRATION_FILENAME)
            if(regString != null) {
                try {
                    registration = JSONObject(regString)
                } catch (e: JSONException) {
                    registration = JSONObject()
                }
            }
        }
    }

    fun putRegString(name: String, value: String){registration.put(name,value)}
    fun putRegBoolean(name: String, value: Boolean){registration.put(name, value)}

    fun getRegString(name: String, default: String = "") : String {
        var regString = default
        try {
            regString = registration.getString(name)
        } catch (e: JSONException) { }
        return regString
    }


    fun getRegBoolean(name: String, default: Boolean = false) : Boolean {
        var regVal = default
        try {
            regVal = registration.getBoolean(name)
        } catch (e: JSONException) { }
        return regVal
    }

    fun saveRegistration(context: Context){
        val oStream = Workspace.getChildOutputStream(context,REGISTRATION_FILENAME,"")
        if(oStream != null) {
            oStream.write(registration.toString(1).toByteArray(Charsets.UTF_8))
            oStream.close()
        }
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

    fun getImage(context: Context, slideNum: Int, sampleSize: Int = 1, story: Story? = activeStory): Bitmap? {
        if(story == null) return null
        val imName = story.slides[slideNum].imageFile
        val iStream = getStoryChildInputStream(context,imName,story.title)
        if(iStream != null){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeStream(iStream, null, options)
        }
        return null
    }

    fun getStoryChildOutputStream(context: Context, relPath: String, mimeType: String = "", storyTitle: String = "") : OutputStream? {
        var iTitle = storyTitle
        if (iTitle== ""){
            if(activeStory == null) return null
            iTitle = activeStory.title
        }
        return getChildOutputStream(context, iTitle + "/" + relPath, mimeType)
    }

    fun getStoryText(context: Context, relPath: String, storyTitle: String = "") : String? {
        val iStream = getStoryChildInputStream(context, relPath, storyTitle)
        if (iStream != null)
            return iStream.reader().use {
                it.readText() }
        return null
    }

    fun getStoryChildInputStream(context: Context, relPath: String, storyTitle: String? = "") : InputStream? {
        var iTitle = storyTitle
        if (iTitle== ""){
            if(activeStory == null) return null
            iTitle = activeStory.title
        }
        return getChildInputStream(context,iTitle + "/" + relPath)
    }

    fun getText(context: Context, relPath: String) : String? {
        val iStream = getChildInputStream(context, relPath)
        if (iStream != null)
            return iStream.reader().use {
                it.readText() }
        return null
    }

    fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "") : OutputStream? {
        if (!workspace.isDirectory) return null
        //build the document tree if it is needed
        val segments = relPath.split("/")
        var df = workspace
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
        val childUri = Uri.parse(workspace.uri.toString() +
                Uri.encode("/$relPath"))
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
    }
}



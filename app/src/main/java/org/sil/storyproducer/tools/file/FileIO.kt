@file:JvmName("FileIO")
package org.sil.storyproducer.tools.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor

import java.io.File
import android.support.v4.provider.DocumentFile
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.*
import java.io.InputStream
import java.io.OutputStream

fun getImage(context: Context, slideNum: Int, sampleSize: Int = 1, story: Story? = Workspace.activeStory): Bitmap? {
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
    var iTitle: String? = storyTitle
    if (iTitle== ""){
        iTitle = Workspace.activeStory?.title
    }
    if (iTitle == null) return null
    return getChildOutputStream(context, iTitle + "/" + relPath, mimeType)
}

fun storyRelPathExists(context: Context, relPath: String, storyTitle: String? = "") : Boolean{
    if(getChildInputStream(context, relPath, storyTitle) == null)
        return false
    return true
}

fun getStoryUri(relPath: String, storyTitle: String = "") : Uri {
    var iTitle: String? = storyTitle
    if (iTitle== ""){
        iTitle = Workspace.activeStory?.title
    }
    return Uri.parse(Workspace.workspace.uri.toString() +
            Uri.encode("/$iTitle/$relPath"))
}

fun getStoryText(context: Context, relPath: String, storyTitle: String = "") : String? {
    val iStream = getStoryChildInputStream(context, relPath, storyTitle)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getStoryChildInputStream(context: Context, relPath: String, storyTitle: String = "") : InputStream? {
    var iTitle: String? = storyTitle
    if (iTitle== ""){
        iTitle = Workspace.activeStory?.title
    }
    if (iTitle == null) return null
    return getChildInputStream(context, iTitle + "/" + relPath)
}

fun getText(context: Context, relPath: String) : String? {
    val iStream = getChildInputStream(context, relPath)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "") : OutputStream? {
    if (!Workspace.workspace.isDirectory) return null
    //build the document tree if it is needed
    val segments = relPath.split("/")
    var df = Workspace.workspace
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
    val childUri = Uri.parse(Workspace.workspace.uri.toString() +
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



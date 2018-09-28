@file:JvmName("FileIO")
package org.sil.storyproducer.tools.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider

import java.io.File
import android.support.v4.provider.DocumentFile
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.*
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream


fun copyToStoryPath(context: Context, sourceUri: Uri, destRelPath: String){
//    var iStream: AutoCloseInputStream = null
    try {
        //TODO Why is DocumentsContract.isDocument not working right?
        val ipfd = context.contentResolver.openFileDescriptor(
                sourceUri, "r")
        val iStream = ParcelFileDescriptor.AutoCloseInputStream(ipfd)
        val opfd = getStoryPFD(context, destRelPath,"","w")
        val oStream = ParcelFileDescriptor.AutoCloseOutputStream(opfd)
        oStream.write(iStream.readBytes())
        iStream.close()
        iStream.close()
    } catch (e: Exception) {}
}

fun getStoryImage(context: Context, slideNum: Int = Workspace.activeSlideNum, sampleSize: Int = 1, story: Story = Workspace.activeStory): Bitmap {
    if(story.title == "") return genDefaultImage()
    return getStoryImage(context,story.slides[slideNum].imageFile,sampleSize,story)
}

fun getStoryImage(context: Context, relPath: String, sampleSize: Int = 1, story: Story = Workspace.activeStory): Bitmap {
    val iStream = getStoryChildInputStream(context,relPath,story.title) ?: return genDefaultImage()
    if(iStream.available() == 0) return genDefaultImage() //something is wrong, just give the default image.
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    return BitmapFactory.decodeStream(iStream, null, options)
}

fun genDefaultImage(): Bitmap {
    val pic = Bitmap.createBitmap(1500,1125,Bitmap.Config.ARGB_8888)
    pic!!.eraseColor(Color.DKGRAY)
    return pic
}
fun getStoryChildOutputStream(context: Context, relPath: String, mimeType: String = "", storyTitle: String = Workspace.activeStory.title) : OutputStream? {
    if (storyTitle == "") return null
    return getChildOutputStream(context, "$storyTitle/$relPath", mimeType)
}

fun storyRelPathExists(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : Boolean{
    if(relPath == "") return false
    //if we can get the type, it exists.
    context.contentResolver.getType(getStoryUri(relPath,storyTitle)) ?: return false
    return true
}

fun getStoryUri(relPath: String, storyTitle: String = Workspace.activeStory.title) : Uri? {
    if (storyTitle == "") return null
    return Uri.parse(Workspace.workspace.uri.toString() +
            Uri.encode("/$storyTitle/$relPath"))
}

fun getStoryText(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : String? {
    val iStream = getStoryChildInputStream(context, relPath, storyTitle)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getStoryChildInputStream(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : InputStream? {
    if (storyTitle == "") return null
    return getChildInputStream(context, "$storyTitle/$relPath")
}

fun getText(context: Context, relPath: String) : String? {
    val iStream = getChildInputStream(context, relPath)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "") : OutputStream? {
    val pfd = getPFD(context, relPath, mimeType,"w")
    return ParcelFileDescriptor.AutoCloseOutputStream(pfd)
}

fun getStoryFileDescriptor(context: Context, relPath: String, mimeType: String = "", mode: String = "r", storyTitle: String = Workspace.activeStory.title) : FileDescriptor? {
    return getStoryPFD(context,relPath,mimeType,mode,storyTitle)?.fileDescriptor
}

fun getStoryPFD(context: Context, relPath: String, mimeType: String = "", mode: String = "r", storyTitle: String = Workspace.activeStory.title) : ParcelFileDescriptor? {
    if (storyTitle == "") return null
    return getPFD(context, "$storyTitle/$relPath", mimeType, mode)
}

fun getChildDocuments(context: Context,relPath: String) : MutableList<String>{
    //build a query to look for the child documents
    //This is actually the easiest and fastest way to get a list of child documents, believe it or not.
    val childDocs: MutableList<String> = ArrayList()
    try {
        val cursor = context.contentResolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(
                        Workspace.workspace.uri,
                        DocumentsContract.getDocumentId(
                                Uri.parse(Workspace.workspace.uri.toString() +
                                        Uri.encode("/$relPath"))
                        ))
                , arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)
        //You have a handle to the data structure (as if in SQL).  walk through the elements and add them to the list.
        cursor.moveToFirst()
        do {
            childDocs.add(VIDEO_DIR + "/" + cursor.getString(0))
            cursor.moveToNext()
        } while ((!cursor.isAfterLast))
    } catch (e: Exception) { return ArrayList() }
    return childDocs
}

fun getPFD(context: Context, relPath: String, mimeType: String = "", mode: String = "r") : ParcelFileDescriptor? {
    if (!Workspace.workspace.isDirectory) return null
    //build the document tree if it is needed
    val segments = relPath.split("/")
    var uri = Workspace.workspace.uri
    for (i in 0 .. segments.size-2){
        //TODO make this faster.
        val newUri = Uri.parse(uri.toString() + Uri.encode("/${segments[i]}"))
        val isDirectory = context.contentResolver.getType(newUri)?.
                contains(DocumentsContract.Document.MIME_TYPE_DIR) ?: false
        if(!isDirectory){
            DocumentsContract.createDocument(context.contentResolver,uri,
                    DocumentsContract.Document.MIME_TYPE_DIR,segments[i])
        }
        uri = newUri
    }
    //create the file if it is needed
    val newUri = Uri.parse(uri.toString() + Uri.encode("/${segments.last()}"))
    //TODO replace with custom exists thing.
    if(context.contentResolver.getType(newUri) == null){
        //find the mime type by extension
        var mType = mimeType
        if(mType == "") {
            mType = when (File(uri.path).extension) {
                "json" -> "application/json"
                //todo - use m4p.
                "mp3"  -> "audio/x-mp3"
                "wav" -> "audio/w-wav"
                "txt" -> "plain/text"
                else -> "*/*"
            }
        }
        DocumentsContract.createDocument(context.contentResolver,uri,mType,segments.last())
    }
    return context.contentResolver.openFileDescriptor(newUri,mode)
}

fun getChildInputStream(context: Context, relPath: String) : InputStream? {
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

fun deleteStoryFile(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : Boolean {
    if(storyRelPathExists(context, relPath, storyTitle)){
        val uri = getStoryUri(relPath,storyTitle)
        return DocumentsContract.deleteDocument(context.contentResolver,uri)
    }
    return false
}

fun renameStoryFile(context: Context, relPath: String, newFilename: String, storyTitle: String = Workspace.activeStory.title) : Boolean {
    if(storyRelPathExists(context, relPath, storyTitle)){
        val uri = getStoryUri(relPath,storyTitle)
        val newUri = DocumentsContract.renameDocument(context.contentResolver,uri,newFilename)
        if(newUri != null) return true
    }
    return false
}



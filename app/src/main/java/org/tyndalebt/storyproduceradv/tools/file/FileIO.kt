@file:JvmName("FileIO")
package org.tyndalebt.storyproduceradv.tools.file

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.LruCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.model.WORD_LINKS_DIR
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.File
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min


fun copyToWorkspacePath(context: Context, sourceUri: Uri, destRelPath: String){
//    var iStream: AutoCloseInputStream = null
    try {
        //TODO Why is DocumentsContract.isDocument not working right?
        val ipfd = context.contentResolver.openFileDescriptor(
                sourceUri, "r")
        val iStream = ParcelFileDescriptor.AutoCloseInputStream(ipfd)
        val opfd = getPFD(context, destRelPath,"","w")
        val oStream = ParcelFileDescriptor.AutoCloseOutputStream(opfd)
        val bArray = ByteArray(100000)
        var bytesRead = iStream.read(bArray)
        while(bytesRead > 0){ //eof not reached
            oStream.write(bArray,0,bytesRead)
            bytesRead = iStream.read(bArray)
        }

        iStream.close()
        // 10/21/2021 - DKH: Espresso test fail for Android 10 and 11 #594
        // Found this typo bug which closed iStream.close() twice.  Change the second
        // "istream.close()" to "ostream.close()". Previously, for every file that was created,
        // the output stream hung around - not a good use of resources
        oStream.close()
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}


fun getStoryImage(context: Context, slideNumber: Int = Workspace.activeStory.lastSlideNum, sampleSize: Int = 1, story: Story = Workspace.activeStory): Bitmap? {
    return if (story.title == "" || slideNumber == story.slides.size) {
        null
    } else {
        getStoryImage(context, story.slides[slideNumber].imageFile, sampleSize, false, story)
    }
}

var storyImageCache = LruCache<String, Bitmap>(30)
var storyImageCacheState = HashSet<String>()

fun getStoryImage(context: Context, relPath: String, sampleSize: Int = 1, useAllPixels: Boolean = false, story: Story = Workspace.activeStory): Bitmap? {
    val cacheString = "${story.title}:$useAllPixels:$sampleSize:$relPath"
    var bmp = storyImageCache.get(cacheString)
    if (!storyImageCacheState.contains(cacheString)) {
        storyImageCacheState.add(cacheString)
        val iStream = getStoryChildInputStream(context, relPath, story.title)
        if (iStream == null || iStream.available() == 0) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        if (useAllPixels) options.inTargetDensity = 1
        bmp = BitmapFactory.decodeStream(iStream, null, options)!!
        if (useAllPixels) bmp.density = Bitmap.DENSITY_NONE
        storyImageCache.put(cacheString, bmp)
    }
    return bmp
}

fun genDefaultImage(): Bitmap {
    val pic = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
    pic!!.eraseColor(Color.DKGRAY)
    return pic
}

fun copyToFilesDir(context: Context, sourceUri: Uri, destFile: File){
    try {
        //TODO Why is DocumentsContract.isDocument not working right?
        val ipfd = context.contentResolver.openFileDescriptor(
                sourceUri, "r")
        val iStream = ParcelFileDescriptor.AutoCloseInputStream(ipfd)
        val oStream = destFile.outputStream()
        val bArray = ByteArray(100000)
        var bytesRead = iStream.read(bArray)
        while(bytesRead > 0){ //eof not reached
            oStream.write(bArray,0,bytesRead)
            bytesRead = iStream.read(bArray)
        }
        //oStream.flush()
        iStream.close()

        // 10/04/2021 - DKH: Espresso test fail for Android 10 and 11 #594
        // Found this typo bug which closed iStream.close() twice.  Change the second
        // "istream.close()" to "ostream.close()". Previously, for every file that was created,
        // the output stream hung around - not a good use of resources
        oStream.close()
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}

fun getDownsample(context: Context, relPath: String,
                         dstWidth: Int = DEFAULT_WIDTH, dstHeight: Int = DEFAULT_HEIGHT,
                         story: Story = Workspace.activeStory): Int{

    // DKH - Updated 03/13/2021 to fix Issue 548: In Android 11 Story Publisher Adv crashes in Finalize
    //                         phase and no video is produced
    // This routine is called for every slide in a story during FINALIZE.  "relPath" is the name of
    // the image file for the slide (e.g., "1.jpg") but some slides images are optional such as the title
    // slide and the song slide.  "relPath" for a slide without an image is passed as "" (empty string).
    // Which  means open the root directory in getStoryChildInputStream
    // Before the fix, iStream.available was called on an empty string file.  Previous to Android 11,
    // iStream.available() return a zero when called on an empty string file.
    // For Android 11, iStream.available() on an empty string file throws an exception which
    // was not caught by this routine.
    // For issue 548, check for an empty string and return
    // a default value of 1 if there is an empty string.  According to the documentation,
    // iStream.available() can throw an exception, so a try/catch was added.
    // restructure routine for better flow
    var ds:Int = 1
    if(relPath != "") { // If empty string: return ds default value
        val iStream = getStoryChildInputStream(context, relPath, story.title)
        if(iStream != null) { // If null: could not assign an iStream to the file, return ds default value
            try {
                if (iStream.available() != 0) {  // if a throw or file is empty, return default value
                    // got data in the file, so process it
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(iStream, null, options)
                    ds = max(1, min(options.outHeight / dstHeight, options.outWidth / dstWidth))
                }
            } catch (e: Exception) {
                // iStream.available can throw, so catch it here
                // return ds default value
            }
        }
    }
    return ds
}


fun getStoryChildOutputStream(context: Context, relPath: String, mimeType: String = "", dirRoot: String = Workspace.activeDirRoot) : OutputStream? {
    if (dirRoot == "") return null
    // DKH-Updated 03/04/2021 to fix Issue #549: Lost data during story creation for Android 10 & 11
    // Specify "truncate file" on open which will start with zero data in the file.
    // Previously, data was not truncated which would leave garbage characters at the EOF when
    // writing a smaller file.  Garbage data caused story.json file corruption.
    // FIX - Added the ability to pass mode to getChildOutputStream.  Pass mode of write/truncate ("wt")
    return getChildOutputStream(context, "$dirRoot/$relPath", mimeType,"wt")
}

fun getWordLinksChildOutputStream(context: Context, relPath: String, mimeType: String = "") : OutputStream? {
    return getChildOutputStream(context, "$WORD_LINKS_DIR/$relPath", mimeType, "wt")
}

fun storyRelPathExists(context: Context, relPath: String, dirRoot: String = Workspace.activeDirRoot) : Boolean{
    if(relPath == "") return false
    val uri = getStoryUri(relPath,dirRoot) ?: return false
    var myGetType = context.contentResolver.getType(uri)
    if ((myGetType == null) && Workspace.isUnitTest) 
    {
        // RK - 03-20-2023
        // The unit tests use a ShadowContextResolver from the context
        // which gives different result from resolver.getType()
        // This fix is intended to compensate for that difference in the unit test
        // See TestParsePhotoStory.parsePhotoStoryTest()
        val file = File(uri.path)
        return file.exists() && file.isDirectory()
    }
    myGetType ?: return false;
    return true
}

fun workspaceRelPathExists(context: Context, relPath: String) : Boolean{
    if(relPath == "") return false
    //if we can get the type, it exists.
    val uri: Uri = getWorkspaceUri(relPath) ?: return false
    context.contentResolver.getType(uri) ?: return false
    return true
}

fun getStoryUri(relPath: String, dirRoot: String = Workspace.activeDirRoot) : Uri? {
    if (dirRoot == "") return null
    return Uri.parse(Workspace.workdocfile.uri.toString() +
            Uri.encode("/$dirRoot/$relPath"))
}

fun getWorkspaceUri(relPath: String) : Uri? {
    return Uri.parse(Workspace.workdocfile.uri.toString() + Uri.encode("/$relPath"))
}

fun getStoryText(context: Context, relPath: String, dirRoot: String = Workspace.activeDirRoot) : String? {
    val iStream = getStoryChildInputStream(context, relPath, dirRoot)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getStoryChildInputStream(context: Context, relPath: String, dirRoot: String = Workspace.activeDirRoot) : InputStream? {
    if (dirRoot == "" || relPath == "") return null
    return getChildInputStream(context, "$dirRoot/$relPath")
}

fun getText(context: Context, relPath: String) : String? {
    val iStream = getChildInputStream(context, relPath)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

// DKH-Updated 03/04/2021 to fix Issue #549: Lost data during story creation for Android 10 & 11
// Expose mode to calling interfaces.  This allows caller to truncate the target (relPath) on an open
// Before change, mode was hardwired to "w".  On a "w" open, all the data from the previous writes to
// the file is still present. If the new file is smaller in size, there are garbage characters left
// from the previous file open/write/close operations.
// Other calling interfaces will see no change since the default to mode is "w" (as before).
// To get file truncation, use "wt" as the mode argument.
fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "", mode: String = "w") : OutputStream? {
    val pfd = getPFD(context, relPath, mimeType, mode)
    var oStream: OutputStream? = null
    try {
        oStream = ParcelFileDescriptor.AutoCloseOutputStream(pfd)
    } catch (e:java.lang.Exception) {}

    return oStream
}

fun getStoryFileDescriptor(context: Context, relPath: String, mimeType: String = "", mode: String = "r", dirRoot: String = Workspace.activeDirRoot) : FileDescriptor? {
    return getStoryPFD(context,relPath,mimeType,mode,dirRoot)?.fileDescriptor
}

fun getStoryPFD(context: Context, relPath: String, mimeType: String = "", mode: String = "r", dirRoot: String = Workspace.activeDirRoot) : ParcelFileDescriptor? {
    if (dirRoot == "") return null
    return getPFD(context, "$dirRoot/$relPath", mimeType, mode)
}

fun getChildDocuments(context: Context,relPath: String) : MutableList<String>{
    //build a query to look for the child documents
    //This is actually the easiest and fastest way to get a list of child documents, believe it or not.
    var childDocs: MutableList<String> = ArrayList()
    val cursor: Cursor
    try {
        cursor = context.contentResolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(
                        Workspace.workdocfile.uri,
                        DocumentsContract.getDocumentId(
                                Uri.parse(Workspace.workdocfile.uri.toString() +
                                        Uri.encode("/$relPath"))
                        ))
                , arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)!!
        //You have a handle to the data structure (as if in SQL).  walk through the elements and add them to the list.
        cursor.moveToFirst()
        do {
            childDocs.add(cursor.getString(0))
            cursor.moveToNext()
        } while ((!cursor.isAfterLast))
        cursor.close()
    } catch (e: Exception) { childDocs = ArrayList() }

    return childDocs
}

fun getPFD(context: Context, relPath: String, mimeType: String = "", mode: String = "r") : ParcelFileDescriptor? {
    if (!Workspace.workdocfile.isDirectory) return null
    //build the document tree if it is needed
    val segments = relPath.split("/")
    var uri = Workspace.workdocfile.uri
    try {
        for (i in 0..segments.size - 2) {
            //TODO make this faster.
            val newUri = Uri.parse(uri.toString() + Uri.encode("/${segments[i]}"))
            val isDirectory = context.contentResolver.getType(newUri)?.contains(DocumentsContract.Document.MIME_TYPE_DIR)
                    ?: false
            if (!isDirectory) {
                DocumentsContract.createDocument(context.contentResolver, uri,
                        DocumentsContract.Document.MIME_TYPE_DIR, segments[i])
            }
            uri = newUri
        }
    } catch (e: Exception){
        FirebaseCrashlytics.getInstance().recordException(e)
        return null
    }
    //create the file if it is needed
    val newUri = Uri.parse(uri.toString() + Uri.encode("/${segments.last()}"))
    //TODO replace with custom exists thing.
    if(context.contentResolver.getType(newUri) == null){
        //find the mime type by extension
        var mType = mimeType
        if(mType == "") {
            mType = when (File(uri.path ?: "").extension) {
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
    var pfd: ParcelFileDescriptor? = null
    try{
        pfd = context.contentResolver.openFileDescriptor(newUri,mode)
    }catch(e:java.lang.Exception){}
    return pfd
}

fun getChildInputStream(context: Context, relPath: String) : InputStream? {
    val childUri = Uri.parse(Workspace.workdocfile.uri.toString() +
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

fun deleteStoryFile(context: Context, relPath: String, dirRoot: String = Workspace.activeDirRoot) : Boolean {
    if(storyRelPathExists(context, relPath, dirRoot)){
        val uri: Uri = getStoryUri(relPath,dirRoot) ?: return false
        return DocumentsContract.deleteDocument(context.contentResolver,uri)
    }
    return false
}

fun deleteWorkspaceFile(context: Context, relPath: String) : Boolean {
    if(workspaceRelPathExists(context, relPath)){
        val uri: Uri  = getWorkspaceUri(relPath) ?: return false
        return DocumentsContract.deleteDocument(context.contentResolver,uri)
    }
    return false
}

val DEFAULT_WIDTH: Int = 1500
val DEFAULT_HEIGHT: Int = 1125

fun goToURL(context: Context, url: String) {
    val openURL = Intent(Intent.ACTION_VIEW)
    openURL.data = Uri.parse(url)
    context.startActivity(openURL)
}

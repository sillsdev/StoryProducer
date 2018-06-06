package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v4.provider.DocumentFile
import com.squareup.moshi.Moshi
import java.io.*

fun Story.toJson(){
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    var projectPath = DocumentFile.fromTreeUri(context,Uri.withAppendedPath(storyUri, PROJECT_DIR))
    if(projectPath != null && projectPath.exists()) {
        val oStream = getChildOutputStream(PROJECT_DIR + "/" + PROJECT_FILE,"application/json")
        if(oStream != null) {
            oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
            oStream.close()
        }
    }
}

fun storyFromJson(context: Context, storyUri: Uri): Story?{
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    val projectUri = Uri.withAppendedPath(storyUri,PROJECT_DIR + "/" + PROJECT_FILE)
    if (File(projectUri.path).exists()) {
        val iStream = context.contentResolver.openInputStream(projectUri)
        val fileContents = iStream.reader().use { it.readText() }
        var story = adapter.fromJson(fileContents)
        if(story != null) {
            story.initializeContext(context)
            story.storyUri = storyUri
            return story
        }
    }
    return null
}

fun Story.getChildInputStream(relPath: String) : InputStream? {
    val childUri = Uri.withAppendedPath(storyUri,relPath)
    if(File(childUri.path).exists()) {
        val pfd: ParcelFileDescriptor? = cpc?.openFile(childUri, "r")
        if (pfd == null) return null
        return ParcelFileDescriptor.AutoCloseInputStream(pfd)
    }
    return null
}

fun Story.getChildOutputStream(relPath: String, mimeType: String) : OutputStream? {
    val childUri: Uri = Uri.withAppendedPath(storyUri,relPath)
    val childFile = File(childUri.path)
    if(!childFile.exists()){
        val parentUri = Uri.fromFile(childFile.parentFile)
        val parentDocument = DocumentFile.fromTreeUri(context,parentUri)
        parentDocument.createFile(mimeType,childUri.lastPathSegment)
    }
    val pfd: ParcelFileDescriptor? = cpc?.openFile(childUri,"w")
    if (pfd == null) return null
    return ParcelFileDescriptor.AutoCloseOutputStream(pfd)
}

fun Story.getImage(slideNum: Int, sampleSize: Int = 1): Bitmap? {
    val imName = slides[slideNum].imageFile
    val iStream = getChildInputStream(imName)
    if(iStream != null){
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        return BitmapFactory.decodeStream(iStream, null, options)
    }
    return null
}

fun Story.getText(relPath: String) : String? {
    val iStream = getChildInputStream(relPath)
    if (iStream != null) {
        return iStream.reader().use { it.readText() }
    }
    return null
}

fun parseStoryIfPresent(context: Context, storyPath: DocumentFile): Story? {
    var story: Story?
    //Check if path is path
    if(!storyPath.isDirectory) return null
    //make a project directory if there is none.
    if (storyPath.findFile(PROJECT_DIR) != null) {
        //parse the project file, if there is one.
        story = storyFromJson(context,storyPath.uri)
        //if there is a story from the file, do not try to read any templates, just return.
        if(story != null) return story
    } else {
        storyPath.createDirectory(PROJECT_DIR)
    }
    //TODO If not, See if there is an html bloom file there
    story = parsePhotoStoryXML(context, storyPath)
    //write the story (if it is not null) to json.
    if(story != null) return story
    return null
}

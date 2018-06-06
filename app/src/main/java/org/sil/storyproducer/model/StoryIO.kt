package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v4.provider.DocumentFile
import com.squareup.moshi.Moshi
import java.io.File
import java.io.InputStream

fun Story.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    var projectPath = DocumentFile.fromTreeUri(context,Uri.withAppendedPath(storyUri, PROJECT_DIR))
    if(!File(storyUri.path + "/" + PROJECT_DIR).exists()){
        projectPath = DocumentFile.fromTreeUri(context,storyUri).createDirectory(PROJECT_DIR)
    }
    if(projectPath != null && projectPath.exists()) {
        val projectFile = projectPath.createFile("application/json", PROJECT_FILE)
        if(projectFile != null){
            val oStream = context.contentResolver.openOutputStream(projectFile.uri);
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
    val iStream = context.contentResolver.openInputStream(projectUri)
    if (iStream != null) {
        val fileContents = iStream.reader().use { it.readText() }
        var story = adapter.fromJson(fileContents)
        if(story != null) {
            story.setContext(context)
            story.storyUri = storyUri
            return story
        }
    }
    return null
}

fun Story.getChildInputStream(relPath: String) : InputStream? {
    val childUri = Uri.withAppendedPath(storyUri,relPath)
    val pfd: ParcelFileDescriptor? = cpc?.openFile(childUri,"r")
    if (pfd == null) return null
    return ParcelFileDescriptor.AutoCloseInputStream(pfd)
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
    }
    storyPath.createDirectory(PROJECT_DIR)
    //TODO If not, See if there is an html bloom file there
    story = parsePhotoStoryXML(context, storyPath)
    //write the story (if it is not null) to json.
    if(story != null) return story
    return null
}
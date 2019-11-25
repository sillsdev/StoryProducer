package org.sil.storyproducer.model

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.crashlytics.android.Crashlytics
import com.squareup.moshi.Moshi
import org.sil.storyproducer.tools.file.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun Story.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    val oStream = getStoryChildOutputStream(context,
            "$PROJECT_DIR/$PROJECT_FILE","",this.title)
    if(oStream != null) {
        try {
            oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        }catch(e:java.lang.Exception){}
        finally{oStream.close()}
    }
}

fun storyFromJson(context: Context, storyTitle: String): Story?{
    try {
        val moshi = Moshi
                .Builder()
                .add(RectAdapter())
                .add(UriAdapter())
                .build()
        val adapter = Story.jsonAdapter(moshi)
        val fileContents = getStoryText(context, "$PROJECT_DIR/$PROJECT_FILE", storyTitle)
                ?: return null
        return adapter.fromJson(fileContents)
    } catch (e: Exception) {
        return null
    }
}

fun parseStoryIfPresent(context: Context, storyPath: androidx.documentfile.provider.DocumentFile): Story? {
    var story: Story?
    //Check if path is path
    if(!storyPath.isDirectory) return null
    //make a project directory if there is none.
    if (storyRelPathExists(context,PROJECT_DIR,storyPath.name!!)) {
        //parse the project file, if there is one.
        story = storyFromJson(context, storyPath.name!!)
        //if there is a story from the file, do not try to read any templates, just return.
        if(story != null) return story
    }
    try {
        story = parsePhotoStoryXML(context, storyPath)
    } catch (e : Exception){
        Crashlytics.logException(e)
        story = null
    }
    if(story == null){
        try {
            story = parseBloomHTML(context, storyPath)
        } catch (e : Exception){
            Crashlytics.logException(e)
            story = null
        }
    }
    //write the story (if it is not null) to json.
    if(story != null) {
        story.toJson(context)
        return story
    }
    return null
}

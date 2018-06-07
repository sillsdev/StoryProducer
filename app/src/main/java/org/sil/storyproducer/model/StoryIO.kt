package org.sil.storyproducer.model

import android.content.Context
import android.net.Uri
import android.support.v4.provider.DocumentFile
import com.squareup.moshi.Moshi
import java.io.*

fun Story.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    val oStream = Workspace.getChildOutputStream(context,PROJECT_DIR + "/" + PROJECT_FILE,"",this)
    if(oStream != null) {
        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        oStream.close()
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
        if(story != null) return story
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
    //TODO If not, See if there is an html bloom file there
    story = parsePhotoStoryXML(context, storyPath)
    //write the story (if it is not null) to json.
    if(story != null) {
        story.toJson(context)
        return story
    }
    return null
}

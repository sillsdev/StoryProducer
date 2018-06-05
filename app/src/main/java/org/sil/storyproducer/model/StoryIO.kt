package org.sil.storyproducer.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.provider.DocumentFile
import com.squareup.moshi.Moshi
import org.apache.commons.io.IOUtils
import org.sil.storyproducer.model.RectAdapter
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.jsonAdapter
import org.sil.storyproducer.model.parsePhotoStoryXML


private val PROJECT_DIR = "project"
private val PROJECT_FILE = "story.json"

class StoryIO(val story: Story) {

    val projectPath: DocumentFile? = story.storyPath.findFile(PROJECT_DIR)
    val projectFile: DocumentFile? = projectPath?.findFile(PROJECT_FILE)

    fun storyToJson(context: Context){
        val moshi = Moshi
                .Builder()
                .add(RectAdapter())
                .add(DocumentFileAdapter())
                .build()
        val adapter = Story.jsonAdapter(moshi)
        if(projectPath == null)
            story.storyPath.createDirectory(PROJECT_DIR)
        if(projectPath != null) {
            projectPath.createFile("application/json", PROJECT_FILE)
            if(projectFile != null){
                val oStream = context.contentResolver.openOutputStream(projectFile.uri);
                oStream.write(adapter.toJson(story).toByteArray(Charsets.UTF_8))
                oStream.close()
            }
        }
    }

    fun getImage(context: Context, slideNum: Int, sampleSize: Int = 1): Bitmap? {
        val imName = story.slides[slideNum].imageFile
        if(story.storyPath.findFile(imName).exists()){
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            val iStream = context.contentResolver.openInputStream(story.storyPath.findFile(imName).uri)
            return BitmapFactory.decodeStream(iStream, null, options)
        }
        return null
    }

    companion object {
        fun storyFromJson(context: Context, storyPath: DocumentFile): Story?{
            val moshi = Moshi
                    .Builder()
                    .add(RectAdapter())
                    .add(DocumentFileAdapter())
                    .build()
            val adapter = Story.jsonAdapter(moshi)
            val projectPath: DocumentFile? = storyPath.findFile(PROJECT_DIR)
            val projectFile = projectPath?.findFile(PROJECT_FILE)
            if (projectFile != null) {
                    val iStream = context.contentResolver.openInputStream(projectFile.uri);
                    val fileContents = iStream.reader().use { it.readText() }
                var story = adapter.fromJson(fileContents)
                if(story != null) {
                    story.storyPath = storyPath
                    return story
                }
            }
            return null
        }
    }
}

fun parseStoryIfPresent(context: Context, storyPath: DocumentFile): Story? {
    var story: Story?
    //Check if path is path
    if(!storyPath.isDirectory) return null
    //make a project directory if there is none.
    if (storyPath.findFile(PROJECT_DIR) != null) {
        //parse the project file, if there is one.
        story = StoryIO.storyFromJson(context,storyPath)
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

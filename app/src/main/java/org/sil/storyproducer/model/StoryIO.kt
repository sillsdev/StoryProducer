package org.sil.storyproducer.model

import android.content.Context
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import net.lingala.zip4j.ZipFile
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.*
import java.io.ByteArrayOutputStream
import java.io.File

class StoryIOException(message: String) : Exception(message)

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

@Throws(StoryIOException::class)
fun storyFromJson(context: Context, storyTitle: String): Story? {
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
        throw StoryIOException("StoryIO: storyFromJson() parse exception from moshi")
    }
}

/**
 * Attempt to parse a story from directory. A null Story is returned in the case of a parse error or non-template directory
 *
 * @return Story
 */
fun parseStoryIfPresent(context: Context, storyPath: DocumentFile): Story? {
    if(!storyPath.isDirectory || storyPath.name == WORD_LINKS_DIR) return null

    var story: Story?
    if (storyRelPathExists(context,PROJECT_DIR,storyPath.name!!)) {
        // project directory exists, so working on a JSON template
        try {
            story = storyFromJson(context, storyPath.name!!)
            return story
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
    } else {
        // no project directory exists, so working on an XML or BLOOM template
        // will need to save to JSON
        try {
            // TODO: should not return null, throw exception
            story = parsePhotoStoryXML(context, storyPath)
        } catch (e : Exception){
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
        if(story == null) {
            try {
                // TODO: should not return null, throw exception
                story = parseBloomHTML(context, storyPath)
            } catch (e : Exception){
                FirebaseCrashlytics.getInstance().recordException(e)
                return null
            }
        }
        // write this story to JSON
        if (story != null) {
            story.toJson(context)
            return story
        }
        return null
    }
}

fun migrateStory(context: Context, story: Story): Story? {
    // Migrate the LOCALCREDITS slide data (present up to v3.0.2) to
    // the property on the Story
    for (i in 0..story.slides.count()-1) {
        val slide = story.slides[i]
        if (slide.slideType == SlideType.LOCALCREDITS) {
            // Remove the LOCALCREDITS slide
            val newSlides = story.slides.toMutableList()
            newSlides.removeAt(i)
            story.slides = newSlides

            // Update the property if it is different from the obsolete starting text
            if (slide.translatedContent != context.getString(R.string.LC_obsolete_starting_text)) {
                story.localCredits = slide.translatedContent
            }
            break
        }
    }
    return story
}

fun isZipped(fileName: String?): Boolean {
    return fileName?.substringAfterLast(".", "")?.let {
        arrayOf("zip", "bloom", "bloomd").contains(it)
    } == true
}

fun unzipIfZipped(context: Context, file: DocumentFile, existingFolders: Array<androidx.documentfile.provider.DocumentFile?>): String? {
    //only unzip zipped files.
    if (!isZipped(file.name)) {
        return file.name
    }

    val name = file.name!!.substringBeforeLast(".","")
    val sourceFile = File("${context.filesDir}/${file.name!!}")
    val zipFile = ZipFile(sourceFile.absolutePath)

    try
    {
        //copy file to internal files directory to perform the normal "File" opterations on.
        val uri = getWorkspaceUri(file.name!!)
        if(uri != null){copyToFilesDir(context,uri,sourceFile)}

        //Exctract to files/unzip
        val fileHeaders = zipFile.fileHeaders

        val folderNames: MutableList<String> = mutableListOf()
        for (f in existingFolders){
            if(f != null) folderNames.add(f.name ?: continue)
        }

        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4192)
        var count : Int

        for (f in fileHeaders){

            if (storyRelPathExists(context, f.fileName)) continue

            val ostream = getChildOutputStream(context, "$name/${f.fileName}") ?: continue

            // reading and writing
            val zis = zipFile.getInputStream(f)
            count = zis.read(buffer)
            try {
                while (count != -1) {
                    baos.write(buffer, 0, count)
                    val bytes = baos.toByteArray()
                    ostream.write(bytes)
                    baos.reset()
                    count = zis.read(buffer, 0, 4192)
                }
            } catch (e: Exception) {
            }
            ostream.close()
            zis.close()
        }

        //delete copied zip file
    }
    catch(e: Exception) { }
    //delete copied and original zip file to save space
    sourceFile.delete()
    deleteWorkspaceFile(context,file.name!!)

    return name
}
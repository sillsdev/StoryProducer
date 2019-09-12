package org.sil.storyproducer.model

import android.content.Context
import android.support.v4.provider.DocumentFile
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

fun parseStoryIfPresent(context: Context, storyPath: DocumentFile): Story? {
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

fun unzipIfNewFolders(context: Context, zipDocFile: DocumentFile?, existingFolders: Array<DocumentFile?>){
    try
    {
        val zipFile = File("${context.filesDir}/${zipDocFile!!.name!!}")
        if(!zipFile.exists()){
            copyToFilesDir(context,getWorkspaceUri(zipDocFile.name!!),zipFile)
        }
        val zis = ZipInputStream(zipFile.inputStream())

        val folderNames: MutableList<String> = mutableListOf()
        for (f in existingFolders){
            if(f != null) folderNames.add(f.name ?: continue)
        }

        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4192)
        var count: Int = 0

        while(true) {

            zis.closeEntry() //close the last entry even if you continue.
            val ze: ZipEntry? = zis.nextEntry
            //if it's not a zip file, return false (didn't work).
            if (ze == null) break

            val filename = ze.name ?: continue

            //Only parse new root folders, not existing folders.
            val folderName = filename.substring(0, filename.indexOf('/'))
            if (folderName in folderNames) continue

            if (storyRelPathExists(context, filename)) continue

            val ostream = getChildOutputStream(context, filename) ?: continue

            // reading and writing
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
        }

        zis.close();
    }
    catch(e: Exception)
    {
        return
    }

    return
}

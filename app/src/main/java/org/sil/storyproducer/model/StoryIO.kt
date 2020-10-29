package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import net.lingala.zip4j.ZipFile
import org.sil.storyproducer.tools.file.*
import java.io.ByteArrayOutputStream
import java.io.File

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
        FirebaseCrashlytics.getInstance().recordException(e)
        story = null
    }
    if(story == null){
        try {
            story = parseBloomHTML(context, storyPath)
        } catch (e : Exception){
            FirebaseCrashlytics.getInstance().recordException(e)
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
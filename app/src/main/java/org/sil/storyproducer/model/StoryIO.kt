package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import net.lingala.zip4j.ZipFile
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.*
import java.io.ByteArrayOutputStream
import java.io.File
import timber.log.Timber



fun Story.toJson(context: Context){
    val filePath = "$PROJECT_DIR/$PROJECT_FILE" // location of file
    val moshi = Moshi
            .Builder()
            .add(RectAdapter())
            .add(UriAdapter())
            .build()
    val adapter = Story.jsonAdapter(moshi)
    val oStream = getStoryChildOutputStream(context,
            filePath,"",this.title)
    if(oStream != null) {
        try {
            oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        }catch(e:java.lang.Exception){
            // If we get here, there was an exception thrown while writing the story.json file
            // Create a suitable error string.  Use  method name, File location, story title & the error
            val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                    "File: "   + this.title + ", " +
                    "Loc: "    + filePath + ", " +
                    "Err: "    + e.toString()

            // Record the error message & exception (includes stack trace) in FireBase
            FirebaseCrashlytics.getInstance().log(errInfo)
            FirebaseCrashlytics.getInstance().recordException(e)

            // Record the message to the android system log
            // The 4 character tag, "SP::", is a quick way to filter messages in the Logcat utility
            // found in Android studio
            // (i.e., to view message during debug, create a Logcat filter for "Log Message:"
            // looking for "SP::")
            Timber.e("SP::(%s)", errInfo) // uses Kotlin Log class with severity level: Error
        }
        finally{oStream.close()}
    }
}

fun storyFromJson(context: Context, storyTitle: String): Story?{
    val filePath = "$PROJECT_DIR/$PROJECT_FILE"  // location of file
    try {
        // use Moshi to restore all information associated with this story
        val moshi = Moshi
                .Builder()
                .add(RectAdapter())
                .add(UriAdapter())
                .build()
        val adapter = Story.jsonAdapter(moshi)
        val fileContents = getStoryText(context, filePath, storyTitle)
                ?: return null
        return adapter.fromJson(fileContents)
    } catch (e: Exception) {
        // If we get here, there was an exception thrown from the
        // Moshi adapter parsing the story.json file
        // Probably some kind of corruption in the story file.
        // Create a suitable error string.  Use  method name, File location, story title & the error
        val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                        "File: "   + storyTitle + ", " +
                        "Loc: "    + filePath + ", " +
                        "Err: "    + e.toString()

        // Record the error message & exception (includes stack trace) in FireBase
        FirebaseCrashlytics.getInstance().log(errInfo)
        FirebaseCrashlytics.getInstance().recordException(e)

        // Record the message to the android system log
        // The 4 character tag, "SP::", is a quick way to filter messages in the Logcat utility
        // found in Android studio
        // (i.e., to view message during debug, create a Logcat filter for "Log Message:"
        // looking for "SP::")
        Timber.e("SP::(%s)", errInfo) // uses Kotlin Log class with severity level: Error

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
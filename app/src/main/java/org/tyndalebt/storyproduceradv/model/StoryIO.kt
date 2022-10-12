package org.tyndalebt.storyproduceradv.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import net.lingala.zip4j.ZipFile
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.BuildConfig
import org.tyndalebt.storyproduceradv.tools.file.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun Story.toJson(context: Context){
    // DKH - Updated 06/02/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
    // Each time we write out a story file, record timestamp and the Story Producer version name & code
    storyToJasonAppVersionCode = BuildConfig.VERSION_CODE  // should be an integer, eg: 23
    storyToJasonAppVersionName = BuildConfig.VERSION_NAME  // should be a string, eg: 3.0.5.debug
    storyToJasonTimeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) // eg: 2021-06-04 15:07:03

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
//            oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
            val tmpStr = adapter.toJson(this)
            if (tmpStr.length > 1) {
                val storyJson = tmpStr.toByteArray(Charsets.UTF_8)
                if (storyJson.size > 50) {
                    oStream.write(storyJson)    // Force close was getting here with empty Json file to write
                }
                else
                    Workspace.saveLogToFile(context, "toJson: toByteArray returned ${tmpStr.length} bytes")
            }
            else
                Workspace.saveLogToFile(context, "toJson: adapter.toJson returned ${tmpStr.length} bytes")

        }catch(e:java.lang.Exception){
            // DKH - Updated 06/02/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
            // If we get here, there was an exception thrown while writing the story.json file
            // Create a suitable error string.  Use  method name, File location, story title & the error
            val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                    "File: "   + this.title + ", " +
                    "Loc: "    + filePath + ", " +
                    "Err: "    + e.toString()

            // Record the error message & exception (includes stack trace) in FireBase
            FirebaseCrashlytics.getInstance().log(errInfo)
            FirebaseCrashlytics.getInstance().recordException(e)

//            Workspace.saveLogToFile(context, "toJson: $errInfo")

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

fun storyFromJson(context: Context, storyTitle: DocumentFile): Story?{
    val filePath = "$PROJECT_DIR/$PROJECT_FILE"  // location of file
    var fileContents: String? = null

    try {
        // use Moshi to restore all information associated with this story
        val moshi = Moshi
                .Builder()
                .add(RectAdapter())
                .add(UriAdapter())
                .build()
        val adapter = Story.jsonAdapter(moshi)
        fileContents = getStoryText(context, filePath, storyTitle.name!!)
                ?: return null
        return adapter.fromJson(fileContents)
    } catch (e: Exception) {
        // DKH - Updated 06/02/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
        // If we get here, there was an exception thrown from the
        // Moshi adapter parsing the story.json file
        // Probably some kind of corruption in the story file.
        // Create a suitable error string.  Use  method name, File location, story title & the error
        val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                        "File: "   + storyTitle.name + ", " +
                        "Loc: "    + filePath + ", " +
                        "Err: "    + e.toString()

        // Record the error message & exception (includes stack trace) in FireBase
        FirebaseCrashlytics.getInstance().log(errInfo)
        FirebaseCrashlytics.getInstance().recordException(e)

//        Workspace.saveLogToFile(context, errInfo)

        // Record the message to the android system log
        // The 4 character tag, "SP::", is a quick way to filter messages in the Logcat utility
        // found in Android studio
        // (i.e., to view message during debug, create a Logcat filter for "Log Message:"
        // looking for "SP::")
        Timber.e("SP::(%s)", errInfo) // uses Kotlin Log class with severity level: Error
    }
    // if we get here we caught an exception and have exited from the catch clause
    // Even though we cannot parse the file with this version of the software, save the
    // content of file for later evaluation for data salvage or error evaluation

    // create a backup file name  with a time stamp
    // example file name: storyWithParseErr_2021-06-03-14-11-26.json
    var backupFileName =
            "project/storyWithParseErr_" +  //location under directory root (eg "002 Lost Coin" )
                    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date()) +
                    ".json"

    // open the backup file and write the contents of story.json to the backup file
    // Can't do a rename on story.json because it is locked from some where else in the app
    var oStream = getStoryChildOutputStream(context,backupFileName,"",storyTitle.name!!)
    if(oStream != null) {
        // We were able to create the file so log an info message
        Timber.i("SP::(%s %s)", "Got valid oStream for backup file:",backupFileName)

        try {
            // catach any IO errors
            // fileContents is not null because we checked above
            oStream.write(fileContents?.toByteArray())  // write the new file
            oStream.close()  // flush out data to file
            // Record the message to the android system log - with special embedded string "SP::"
            Timber.i("SP::(%s)", "Wrote backup file contents")
        }catch(e:java.lang.Exception){
            //  Unable to write backup file
            val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                    "Unable to create/write story.json backup file" + ", " +
                    "File: "   + fileContents + ", "
                    "Err: "    + e.toString()

            // Record the error message & exception (includes stack trace) in FireBase
            FirebaseCrashlytics.getInstance().log(errInfo)
            FirebaseCrashlytics.getInstance().recordException(e)

            // Record the message to the android system log - with special embedded string "SP::"
            Timber.e("SP::(%s)", errInfo) // uses Kotlin Log class with severity level: Error
        }
    }else{
        // report an error to logcat and Firebase about not being able to create a backup file
        val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                "File: "   + backupFileName + ", " +
                "Err: "    + "Could not create backup file"

        // Record the error message in FireBase
        FirebaseCrashlytics.getInstance().log(errInfo)

        // Record the message to the android system log - with special embedded string "SP::"
        Timber.e("SP::(%s)", errInfo) // uses Kotlin Log class with severity level: Error
    }

    return null
}

fun parseStoryIfPresent(context: Context, storyPath: androidx.documentfile.provider.DocumentFile): Story? {
    var story: Story?
    //Check if path is path
    if(!storyPath.isDirectory) return null
    //make a project directory if there is none.
    if (storyRelPathExists(context,PROJECT_DIR,storyPath.name!!)) {
        //parse the project file, if there is one.
        story = storyFromJson(context, storyPath)
        //if there is a story from the file, do not try to read any templates, just return.
        if(story != null) return story
    }
/* DBH 8/9/2022  XML logic no longer used
    try {
        story = parsePhotoStoryXML(context, storyPath)
    } catch (e : Exception){
        FirebaseCrashlytics.getInstance().recordException(e)
        story = null
    }
 */
    try {
        story = parseBloomHTML(context, storyPath)
    } catch (e : Exception){
        FirebaseCrashlytics.getInstance().recordException(e)
        story = null
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
        arrayOf("zip", "bloom", "bloomd", "bloomSource").contains(it)
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
        //copy file to internal files directory to perform the normal "File" operations on.
        val uri = getWorkspaceUri(file.name!!)
        if(uri != null){copyToFilesDir(context,uri,sourceFile)}

        Thread.sleep(250)
        //Extract to files/unzip
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
            ostream.flush()
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
package org.sil.storyproducer.model

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.lingala.zip4j.ZipFile
import org.sil.storyproducer.App
import org.sil.storyproducer.BuildConfig
import org.sil.storyproducer.R
import org.sil.storyproducer.tools.file.copyToFilesDir
import org.sil.storyproducer.tools.file.deleteWorkspaceFile
import org.sil.storyproducer.tools.file.getChildOutputStream
import org.sil.storyproducer.tools.file.getDocumentText
import org.sil.storyproducer.tools.file.getStoryChildOutputStream
import org.sil.storyproducer.tools.file.getWorkspaceUri
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.file.workspaceRelPathExists
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


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
            .add(KotlinJsonAdapterFactory())
            .build()
//    val adapter = Story.jsonAdapter(moshi)
    val adapter: JsonAdapter<Story> = moshi.adapter(Story::class.java).nonNull()
    val oStream = getStoryChildOutputStream(context,
            filePath,"",this.title)
    if(oStream != null) {
        try {
            oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
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

fun storyFromJson(context: Context, storyPath: DocumentFile, validateOnly: Boolean = false): Story? {

    val storyFilePath = storyPath.findFile(PROJECT_DIR)
            ?.let { projDir -> projDir.findFile(PROJECT_FILE) }
                ?: return null
    if (!storyFilePath.isFile)
        return null
    val filePath = "$PROJECT_DIR/$PROJECT_FILE"  // location of file
    var fileContents: String? = null

    try {
        // use Moshi to restore all information associated with this story
        val moshi = Moshi
                .Builder()
                .add(RectAdapter())
                .add(UriAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()
//        val adapter = Story.jsonAdapter(moshi)
        val adapter: JsonAdapter<Story> = moshi.adapter(Story::class.java).nonNull()
        fileContents = getDocumentText(context, storyFilePath)
                ?: return null
        return adapter.fromJson(fileContents)
    } catch (e: Exception) {
        // DKH - Updated 06/02/2021  for Issue 555: Report Story Parse Exceptions and Handle them appropriately
        // If we get here, there was an exception thrown from the
        // Moshi adapter parsing the story.json file
        // Probably some kind of corruption in the story file.
        // Create a suitable error string.  Use  method name, File location, story title & the error
        val errInfo =   "Method: " + Throwable().stackTrace[0].methodName + ", " +
                        "File: "   + storyPath.name + ", " +
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
    if (validateOnly)
        return null // return if no backup needed (just validating story)

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
    var oStream = getStoryChildOutputStream(context,backupFileName,"",storyPath.name!!)
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

fun isValidStory(context: Context, storyPath: DocumentFile) : Boolean {
    var story = parseStoryIfPresent(context, storyPath, true)
    if (story == null)
        return false
    return true
}

fun parseStoryIfPresent(context: Context, storyPath: DocumentFile, validateOnly: Boolean = false, lang : String? = null): Story? {
    var story: Story? = null
    //Check if path is path
    if(!storyPath.isDirectory) return null

    //make a project directory if there is none.
    if (storyPath.findFile(PROJECT_DIR) != null) {
        //parse the project file, if there is one.
        story = storyFromJson(context, storyPath, validateOnly)
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
        if (validateOnly)
            return null // no valid story.json
        try {
            story = parseBloomHTML(context, storyPath, lang)
        } catch (e : Exception){
            FirebaseCrashlytics.getInstance().recordException(e)
            story = null
        }
    }
    //write the story (if it is not null) to json.
    if(story != null) {
        if (!validateOnly)
            story.toJson(context)   // No need to write json if validating only
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

fun bloomSourceAutoDLDir() : String {return App.appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path!!}

fun bloomSourceZipExt() : String {return ".bloomSource"}

fun isZipped(fileName: String?): Boolean {
    return fileName?.substringAfterLast(".", "")?.let {
        arrayOf("zip", "bloom", "bloomd", "bloomSource", "bloompub").contains(it)
    } == true
}
// copy a file to the new location
fun copySubFile(context: Context, subFile: String, newDocumentFolder: DocumentFile, oldDocumentFolder: DocumentFile): Boolean {

    val oldDoc = oldDocumentFolder.findFile(subFile) ?: return false
    if (!oldDoc.isFile)
        return false

    if (newDocumentFolder.findFile(subFile) != null)
        return false    // Should not have the same file in the target folder (abort)

    try {
        // copy the file using a buffer here
        val newDoc = newDocumentFolder.createFile("", subFile) ?: return false

        val buffer = ByteArray(1024 * 64)
        val outStream = context.contentResolver.openOutputStream(newDoc.uri) ?: return false
        val inStream = context.contentResolver.openInputStream(oldDoc.uri) ?: return false
        var bytesRead = 0
        do {
            bytesRead = inStream.read(buffer)
            if (bytesRead > 0)
                outStream.write(buffer, 0, bytesRead)
        } while (bytesRead > 0)
        inStream.close()
        outStream.close()

        return true

    } catch (e: java.io.IOException) {
    }

    return false;
}

// Copy a sub-folder and content to a new folder location - this is done recursively
fun copySubFolder(context: Context, subFolder: String, newDocumentFolder: DocumentFile, oldDocumentFolder: DocumentFile, isWordlinksOrVideo: Boolean = false): Boolean {

    var allCopiedOk = true
    val oldFolder = oldDocumentFolder.findFile(subFolder) ?: return false
    if (!oldFolder.isDirectory)
        return false    // no old folder found to copy

    var newFolder = newDocumentFolder.findFile(subFolder)
    if (newFolder != null && !newFolder.isDirectory)
        return false    // found a non-directory - give up

    if (newFolder != null && !isWordlinksOrVideo)
        return false    // found a folder but not copying wordlinks or videos - give up

    if (newFolder == null)  // if no new folder create one
        newFolder = newDocumentFolder.createDirectory(subFolder) ?: return false

    for (oldSubDoc in oldFolder.listFiles()) {
        if (oldSubDoc.isDirectory) {
            if (!copySubFolder(context, oldSubDoc.name!!, newFolder, oldFolder)) {
                return false
            }
        } else if (oldSubDoc.isFile) {
            if (!copySubFile(context, oldSubDoc.name!!, newFolder, oldFolder)) {
                allCopiedOk = false // flag so that old wordlinks or videos folder does not get deleted
                if (!isWordlinksOrVideo)
                    return false   // abort further copying if not a wordlinks or videos sub-file
            }
        } else
            return false
    }

    return allCopiedOk  // only successful if all files copied ok
}

// Copy a Story/videos/wordlinks sub-folder from the source templates folder and if ok return for further processing
fun copyOldStory(context: Context, file: DocumentFile, newWorkspaceFolder: DocumentFile, oldWorkspaceFolder: DocumentFile): DocumentFile? {

    do {
        // NB: This list may also contains Stories and zipped Stories already found in the new SP Templates folder

        if (newWorkspaceFolder.uri == oldWorkspaceFolder.uri)
            return file // same folder so no need to copy

        if (oldWorkspaceFolder.name?.isEmpty() ?: return file)
            return file // no old folder so noting to copy

        if (newWorkspaceFolder.name?.isEmpty() ?: break)
            break   // no new SP Templates folder so can't copy TODO: report error?

        if (file.name?.isEmpty() ?: break)
            break;  // no Story name so can't copy TODO: report error?

        val non_story_folders = arrayOf(VIDEO_DIR, WORD_LINKS_DIR)
        val isWordlinksOrVideo = non_story_folders.contains(file.name!!)
        if (!isWordlinksOrVideo) {
            // if this is not a video or worklinks folder AND
            if (workspaceRelPathExists(context, file.name!!)) {
                // folder already exists in new workspace
                return file // Story already exists - don't copy but process it
            }
        }

        if (isZipped(file.name)) {
                        // The Story archive zipped file must be for the unzipIfZipped() function
            return file // NB any zipped file in the old folder will be ignored
        }

        if (file.isFile)
            return file // Unknown file, return it for processing and don't copy here

        if (!file.isDirectory)
            break // Stories are always folders TODO: report error?

        // get the old subfolder as a DocumentFile
        val oldSubFolder = oldWorkspaceFolder.findFile(file.name!!) ?: break
        if (!oldSubFolder.isDirectory)
            break   // old stories are are always folders TODO: report error?

        // Here we check that this sub-folder is a valid Story before copying it
        if (!isWordlinksOrVideo) {
            // but only validate if not a video or wordlinks folder
            // get the newly copied subfolder as a DocumentFile
            if (!isValidStory(context, oldSubFolder))
                break   // don't process it further TODO: report warning?
        }

        // here is the main action - copy this subfolder now
        if (!copySubFolder(context, file.name!!, newWorkspaceFolder, oldWorkspaceFolder, isWordlinksOrVideo))
            break   // copying story failed so no more processing TODO: report error (if not wordlinks or videos folder)?

        // get the newly copied subfolder as a DocumentFile
        val newSubFolder = newWorkspaceFolder.findFile(file.name!!) ?: break    // TODO: report error?

        // Story folder copied ok - so check new folder parses before deleting the old story folder
        if (!isWordlinksOrVideo) {
            // but only validate if not a video or wordlinks folder
            if (!isValidStory(context, newSubFolder))
                break   // don't process it further TODO: report error?
        }

        // everything copied ok so we can delete the old copied folder
        if (!oldSubFolder.delete())
            break   // failed to delete so don't process further TODO: report error?

        // if a video or wordlinks folder copied ok then no need to process further
        if (isWordlinksOrVideo)
            break   // don't process non story folders any more

        return newSubFolder // continue processing this copied Story folder

    } while (false)

    return null // TODO: report an error to user (via crashlitics?)
}

fun unzipIfZipped(context: Context, file: DocumentFile, existingFolders: Array<DocumentFile?>): String? {
    //only unzip zipped files.
    if (!isZipped(file.name)) {
        return file.name
    }

    var unzippedOk = false  // only delete file if it can be unzipped (installed) ok
    var storyName = file.name!!.substringBeforeLast(".","")

    // remove any language extension embedded in the story name [no longer needed]
//    val pattern = Regex("\\.lang_[a-z]+$")
//    val match = pattern.find(storyName)
//    if (match != null)
//        storyName = storyName.substring(0, storyName.length - match?.value.length)

    val internalFile = File("${context.filesDir}/${file.name!!}")
    var dlFileStr = bloomSourceAutoDLDir() + "/" + file.name
    var dlFile = File(dlFileStr)
    var dlFileExists = dlFile.exists()
    var zipFile : ZipFile
    zipFile = if (dlFileExists)
        ZipFile(dlFile.absolutePath)
    else
        ZipFile(internalFile.absolutePath)
    try
    {
        //copy file to internal files directory to perform the normal "File" operations on.
        val uri = getWorkspaceUri(file.name!!)
        if(uri != null && !dlFileExists) {
            copyToFilesDir(context, uri, internalFile)
        }

        //Extract to files/unzip
        val fileHeaders = zipFile.fileHeaders

        val folderNames: MutableList<String> = mutableListOf()
        for (f in existingFolders){
            if(f != null) folderNames.add(f.name ?: continue)
        }
        // BW not sure why we just made a list of existing folder and file names (folderNames).
        // Did we hope to check if the new folder name storyName already exists?

        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4192)
        var count : Int

        for (f in fileHeaders){

            if (storyRelPathExists(context, f.fileName, storyName)) continue    // added storyName to fix unzipping issue

            val ostream = getChildOutputStream(context, "$storyName/${f.fileName}") ?: continue

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
        unzippedOk = true
    }
    catch(e: Exception) {
        unzippedOk = false
        Timber.w("Failed to unzip downloaded story: '%s' [%s]", file.name, e.message)
    }
    //delete copied and original zip file to save space
    if (dlFileExists) {
        if (unzippedOk) {
            if (!dlFile.delete()) {
                Timber.w("Failed to delete downloaded story: '%s'", file.name)
            }
        }
    }
    else {
        if (unzippedOk) {
            internalFile.delete()
            deleteWorkspaceFile(context, file.name!!)
        }
    }

    return storyName
}

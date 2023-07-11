package org.sil.storyproducer.model

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.tools.file.getText
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

val REGISTRATION_FILENAME = "registration.json"

class Registration{
    private var jsonData: JSONObject = JSONObject()

    var complete : Boolean
    get() {return getBoolean("registration_complete",false)}
    set(value){putBoolean("registration_complete",value)}

    // gets the String from a text file in internal storage or null if error
    private fun getInternalText(context: Context, relPath: String) : String? {
        try {
            val iInternalStreamPath = "${context.filesDir}${File.separator}$relPath";
            val iInternalStream = File(iInternalStreamPath).inputStream()
            val internalFileText = iInternalStream.reader().use {it.readText()}
            if (internalFileText.isNotEmpty())
                return internalFileText;
            return null
        } catch (e: FileNotFoundException) {
            return null
        }
    }

    // returns an output file stream to a file in internal storage or null if error
    private fun getInternalOutputStream(context: Context, relPath: String) : OutputStream? {
        try {
            val iInternalStreamPath = "${context.filesDir}${File.separator}$relPath";
            val iInternalOFile = File(iInternalStreamPath)
            val iInternalOStream = FileOutputStream(iInternalOFile, false)  // false = don't append
            return iInternalOStream;
        } catch (e: FileNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            return null
        }
    }

    fun load(context: Context) {
        // check for registration info in internal storage first
        var regString: String? = getInternalText(context,REGISTRATION_FILENAME)
        if (regString == null) {
            // not found so look in the old place for a registration file in the templates folder
            regString = getText(context, REGISTRATION_FILENAME)
        }
        if(regString != null) {
            try {
                jsonData = JSONObject(regString)
            } catch (e: JSONException) {
                FirebaseCrashlytics.getInstance().recordException(e)
                jsonData = JSONObject()
            }
        }
    }
    fun save(context: Context){
        // 03/08/2021 - DKH  Fix file corruption in registration.json file (see issue 549)
        // Since we are not saving anything in the current registration file, truncate the file
        // to eliminate all data.  Previously, the file was open with default mode of "w" which
        // preserved all the data in the file. If the new file is smaller in size,
        // there are garbage characters left.

        // now we only save registration info in internal app storage
        // WAS: val oStream = getChildOutputStream(context,REGISTRATION_FILENAME,"","wt")
        // so now only write registration file to internal storage
        val oStream = getInternalOutputStream(context, REGISTRATION_FILENAME)
        if(oStream != null) {
            try {
                oStream.write(jsonData.toString(1).toByteArray(Charsets.UTF_8))
            } catch(e:Exception){
                FirebaseCrashlytics.getInstance().recordException(e)
            } finally {
                oStream.close()
            }
        }
    }

    fun putString(name: String, value: String){jsonData.put(name,value)}
    fun putBoolean(name: String, value: Boolean){jsonData.put(name, value)}

    fun getString(name: String, default: String = "") : String {
        var regString = default
        try {
            regString = jsonData.getString(name)
        } catch (e: JSONException) {}
        return regString
    }

    fun getBoolean(name: String, default: Boolean = false) : Boolean {
        var regVal = default
        try {
            regVal = jsonData.getBoolean(name)
        } catch (e: JSONException) { }
        return regVal
    }
}



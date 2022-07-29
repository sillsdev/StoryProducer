package org.tyndalebt.spadv.model

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONException
import org.json.JSONObject
import org.tyndalebt.spadv.tools.file.getChildOutputStream
import org.tyndalebt.spadv.tools.file.getText

val REGISTRATION_FILENAME = "registration.json"

class Registration{
    private var jsonData: JSONObject = JSONObject()

    var complete : Boolean
    get() {return getBoolean("registration_complete",false)}
    set(value){putBoolean("registration_complete",value)}

    fun load(context: Context) {
        val regString: String? = getText(context,REGISTRATION_FILENAME)
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
        val oStream = getChildOutputStream(context,REGISTRATION_FILENAME,"","wt")
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



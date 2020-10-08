package org.sil.storyproducer.model

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.tools.file.getChildOutputStream
import org.sil.storyproducer.tools.file.getText

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
        val oStream = getChildOutputStream(context,REGISTRATION_FILENAME,"")
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



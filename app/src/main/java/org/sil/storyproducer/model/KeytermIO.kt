package org.sil.storyproducer.model

import android.content.Context
import android.support.v4.provider.DocumentFile
import com.squareup.moshi.Moshi
import org.sil.storyproducer.tools.file.*

fun Keyterm.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .build()
    val adapter = Keyterm.jsonAdapter(moshi)
    val oStream = getKeytermChildOutputStream(context,
            "${this.term}.json","","${this.term}_${this.term.hashCode()}")
    if(oStream != null) {
        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        oStream.close()
    }
}

fun keytermFromJson(context: Context, keytermName: String): Keyterm?{
    val moshi = Moshi
            .Builder()
            .add(UriAdapter())
            .build()
    val adapter = Keyterm.jsonAdapter(moshi)
    val fileContents = getStoryText(context,"$keytermName/${keytermName.substringBefore('_')}.json", "keyterms") ?: return null
    return adapter.fromJson(fileContents)
}

fun parseKeytermIfPresent(context: Context, keytermPath: DocumentFile): Keyterm? {
    var keyterm: Keyterm? = null
    //Check if path is path
    if(!keytermPath.isDirectory) return null
    //make a project directory if there is none.
    if (storyRelPathExists(context,keytermPath.name!!, "keyterms")) {
        //parse the project file, if there is one.
        keyterm = keytermFromJson(context, keytermPath.name!!)
        //if there is a story from the file, do not try to read any templates, just return.
        if(keyterm != null) return keyterm
    }
    //write the story (if it is not null) to json.
    if(keyterm != null) {
        keyterm.toJson(context)
        return keyterm
    }
    return null
}
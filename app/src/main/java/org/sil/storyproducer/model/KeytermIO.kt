package org.sil.storyproducer.model

import android.content.Context
import com.squareup.moshi.Moshi
import org.sil.storyproducer.tools.file.getKeytermChildOutputStream
import org.sil.storyproducer.tools.file.getStoryText

fun KeytermList.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .build()
    val adapter = KeytermList.jsonAdapter(moshi)
    val oStream = getKeytermChildOutputStream(context,
            KEYTERMS_JSON_FILE,"")
    if(oStream != null) {
        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        oStream.close()
    }
}

fun keytermListFromJson(context: Context): KeytermList?{
    val moshi = Moshi
            .Builder()
            .add(UriAdapter())
            .build()
    val adapter = KeytermList.jsonAdapter(moshi)
    val fileContents = getStoryText(context, KEYTERMS_JSON_FILE, KEYTERMS_DIR) ?: return null
    return adapter.fromJson(fileContents)
}
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
            "keyterms.json","")
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
    val fileContents = getStoryText(context,"keyterms.json", "keyterms") ?: return null
    return adapter.fromJson(fileContents)
}
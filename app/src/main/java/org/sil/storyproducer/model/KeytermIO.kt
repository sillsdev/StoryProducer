package org.sil.storyproducer.model

import android.content.Context
import com.squareup.moshi.Moshi
import org.sil.storyproducer.tools.file.*

fun Keyterm.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .build()
    val adapter = Keyterm.jsonAdapter(moshi)
    val oStream = getKeytermChildOutputStream(context,
            "${this.term}.json","",this.term)
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
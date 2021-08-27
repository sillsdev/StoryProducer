package org.sil.storyproducer.tools.file

import android.content.Context
import com.squareup.moshi.Moshi
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.WORD_LINKS_DIR
import org.sil.storyproducer.model.WORD_LINKS_JSON_FILE

fun WordLinkList.toJson(context: Context){
    val moshi = Moshi
            .Builder()
            .build()
    val adapter = WordLinkList.jsonAdapter(moshi)
    val oStream = getWordLinksChildOutputStream(context,
            WORD_LINKS_JSON_FILE,"")
    if(oStream != null) {
        oStream.write(adapter.toJson(this).toByteArray(Charsets.UTF_8))
        oStream.close()
    }
}

/**
 * Retrieves the list of all the word links from wordlinks.json
 */
fun wordLinkListFromJson(context: Context): WordLinkList? {
    val moshi = Moshi
            .Builder()
            .add(UriAdapter())
            .build()
    val adapter = WordLinkList.jsonAdapter(moshi)
    val fileContents = getStoryText(context, WORD_LINKS_JSON_FILE, WORD_LINKS_DIR) ?: return null
    return adapter.fromJson(fileContents)
}
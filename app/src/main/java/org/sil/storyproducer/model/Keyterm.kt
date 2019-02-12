package org.sil.storyproducer.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Keyterm (var term: String = "",
               var termForms: List<String> = listOf(),
               var alternateRenderings: List<String> = listOf(),
               var explanation: String = "",
               var relatedTerms: List<String> = listOf(),

               var backTranslations: MutableList<BackTranslation> = mutableListOf(),
               var chosenKeytermFile: String = "") {
    companion object
}

@JsonClass(generateAdapter = true)
class BackTranslation (var textBackTranslation : String = "",
                       var audioBackTranslation : String = "") {
    companion object
}

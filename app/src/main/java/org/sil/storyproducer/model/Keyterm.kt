package org.sil.storyproducer.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Keyterm{
    var term = ""
    var termForms: MutableList<String> = ArrayList<String>()
    var alternateRenderings = ""
    var explanation = ""
    var relatedTerms: MutableList<String> = ArrayList<String>()
}

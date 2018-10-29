package org.sil.storyproducer.model

class Keyterm (term: String,
               termForms: MutableList<String>,
               alternateRenderings: String,
               explanation: String,
               relatedTerms: MutableList<String>){
    var term = term
    var termForms: MutableList<String> = termForms
    var alternateRenderings = alternateRenderings
    var explanation = explanation
    var relatedTerms: MutableList<String> = relatedTerms
}

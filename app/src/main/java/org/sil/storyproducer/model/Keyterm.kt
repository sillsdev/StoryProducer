package org.sil.storyproducer.model

class Keyterm (term: String,
               termForms: List<String>,
               alternateRenderings: List<String>,
               explanation: String,
               relatedTerms: List<String>){
    var term = term
    var termForms: List<String> = termForms
    var alternateRenderings: List<String> = alternateRenderings
    var explanation = explanation
    var relatedTerms: List<String> = relatedTerms
}

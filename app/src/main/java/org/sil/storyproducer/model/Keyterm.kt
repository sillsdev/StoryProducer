package org.sil.storyproducer.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Keyterm (var term: String = "",
               var termForms: List<String> = listOf(),
               var alternateRenderings: List<String> = listOf(),
               var explanation: String = "",
               var relatedTerms: List<String> = listOf()) : Parcelable{
}

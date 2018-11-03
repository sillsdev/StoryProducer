package org.sil.storyproducer.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Keyterm (var term: String,
               var termForms: List<String>,
               var alternateRenderings: String,
               var explanation: String,
               var relatedTerms: List<String>) : Parcelable {
}

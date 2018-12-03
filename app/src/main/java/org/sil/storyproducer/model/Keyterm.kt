package org.sil.storyproducer.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@JsonClass(generateAdapter = true)
@Parcelize
class Keyterm (var term: String = "",
               var termForms: List<String> = listOf(),
               var alternateRenderings: List<String> = listOf(),
               var explanation: String = "",
               var relatedTerms: List<String> = listOf(),

               var backTranslations: @RawValue MutableList<BackTranslation> = mutableListOf(),
               var chosenKeytermFile: String = "") : Parcelable{
    companion object
}

@JsonClass(generateAdapter = true)
@Parcelize
class BackTranslation (var textBackTranslation : MutableList<String> = mutableListOf(),
                       var audioBackTranslation : String = "") : Parcelable {
    companion object
}

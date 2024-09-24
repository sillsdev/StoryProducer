package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.jsoup.nodes.Element

class NumberedPageSlideBuilder : SlideBuilder() {


    fun build(context: Context,
                storyPath: DocumentFile,
                storyAudioPath: DocumentFile,
                storyAudioMap: MutableMap<String, DocumentFile>,
                page: Element,
                lang: String,
                isSPAuthored: Boolean): Slide? {

        val slide = Slide()
        slide.slideType = SlideType.NUMBEREDPAGE

        slide.prevPageImageFile = prevPageImage
        if (!parsePage(context, false, page, slide, storyPath, storyAudioPath, storyAudioMap, lang)) {
            if (isSPAuthored) {
                prevPageImage = slide.imageFile // no audio in this page but maybe an image file for next page
                FirebaseCrashlytics.getInstance().log("Story '${storyPath.name}' not built as this slide does not contain audio.")
                return null  // SP authored templates should contain audio
            }
        }
        prevPageImage = ""  // this pages image was used so not available to next page

        val bloomEditables = page.getElementsByAttributeValueContaining("class", BLOOM_TRANSLATION_GROUP)
                .filter { !it.hasClass(BLOOM_IMAGE_DESCRIPTION) }
                .map { it.getElementsByAttributeValueContaining("class", BLOOM_EDITABLE) }
                .flatten()
                .filter { it.attr(LANG) == lang }
                .filter { textOf(it).isNotEmpty() }

        if (!bloomEditables.isEmpty()) {

            if (!isSPAuthored) {
                // Not an SP authored bloom story so display each and
                // every editable on a seperate line with no bold reference
                slide.reference = ""
                for (editable in bloomEditables) {
                    val editableText = textOf(editable)
                    if (editableText.isNotEmpty()) {
                        if (slide.content.isNotEmpty())
                            slide.content += "\n"
                        slide.content += editableText
                    }
                }
            } else {
                // this is where SP authored bloom books should put them
                slide.content = textOf(bloomEditables.firstOrNull())
                slide.reference = textOf(bloomEditables.getOrNull(1))
            }
        }

        if (!slide.prevPageImageFile.isNullOrEmpty() || !slide.imageFile.isNullOrEmpty() || !slide.content.trim().isNullOrEmpty()) {
            return slide
        } else {
            FirebaseCrashlytics.getInstance().log("Story '${storyPath.name}' not built as no matching image or text for this slide.")
            return null // no matching image or text for this slide (test on SP authored templates)!!!!!
        }
    }

    private fun textOf(bloomEditable: Element?): String {
        return bloomEditable
                ?.wholeText()
                ?.trim()
                .orEmpty()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    companion object {

        const val BLOOM_EDITABLE = "bloom-editable"
        const val BLOOM_TRANSLATION_GROUP = "bloom-translationGroup"
        const val BLOOM_IMAGE_DESCRIPTION = "bloom-imageDescription"

        var prevPageImage = ""
    }

}

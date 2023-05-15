package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Element

class NumberedPageSlideBuilder : SlideBuilder() {

    fun build(context: Context, file: DocumentFile, page: Element, lang: String): Slide? {
        val slide = Slide()
        slide.slideType = SlideType.NUMBEREDPAGE

        slide.prevPageImageFile = prevPageImage
        if (!parsePage(context, false, page, slide, file, lang)) {
            prevPageImage = slide.imageFile // no audio in this page but maybe an image file for next page
            return null
        }
        prevPageImage = ""  // this pages image was used so not available to next page

        val bloomEditables = page.getElementsByAttributeValueContaining("class", BLOOM_TRANSLATION_GROUP)
                .filter { !it.hasClass(BLOOM_IMAGE_DESCRIPTION) }
                .map { it.getElementsByAttributeValueContaining("class", BLOOM_EDITABLE) }
                .flatten()
                .filter { it.attr(LANG) == lang }
                .filter { textOf(it).isNotEmpty() }

        if (!bloomEditables.isEmpty()) {
            if (bloomEditables.size > 2) {
                // probably not an SP authored bloom story so display each
                // and every editable on a seperate line with no bold reference
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

        return slide
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

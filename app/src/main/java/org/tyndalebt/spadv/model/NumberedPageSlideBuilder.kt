package org.tyndalebt.spadv.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Element

class NumberedPageSlideBuilder : SlideBuilder() {

    fun build(context: Context, file: DocumentFile, page: Element, lang: String): Slide? {
        val slide = Slide()
        slide.slideType = SlideType.NUMBEREDPAGE

        if (!parsePage(context, false, page, slide, file)) {
            return null
        }

        val bloomEditables = page.getElementsByAttributeValueContaining("class", BLOOM_TRANSLATION_GROUP)
                .filter { !it.hasClass(BLOOM_IMAGE_DESCRIPTION) }
                .map { it.getElementsByAttributeValueContaining("class", BLOOM_EDITABLE) }
                .flatten()
                .filter { it.attr(LANG) == lang }

        if (!bloomEditables.isEmpty()) {
            slide.content = textOf(bloomEditables.firstOrNull())
            slide.reference = textOf(bloomEditables.getOrNull(1))
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

    }

}
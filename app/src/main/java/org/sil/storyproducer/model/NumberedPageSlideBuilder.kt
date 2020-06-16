package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class NumberedPageSlideBuilder : SlideBuilder() {

    fun build(context: Context, file: DocumentFile, page: Element, lang: String): Slide? {
        val slide = Slide()
        slide.slideType = SlideType.NUMBEREDPAGE

        if (!parsePage(context, false, page, slide, file)) {
            return null
        }

        page.getElementsByAttributeValueContaining("class", BLOOM_EDITABLE)
                .filter { it.attr(LANG) == lang }
                .also {
            slide.content = buildContent(it)
            slide.reference = buildScriptureReference(it)
        }

        return slide
    }

    private fun buildContent(bloomEditables: List<Element>): String {
        return bloomEditables
                .getOrNull(bloomEditables.size - 2)
                ?.wholeText()
                ?.trim()
                .orEmpty()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    private fun buildScriptureReference(bloomEditables: List<Element>): String {
        return bloomEditables
                .lastOrNull()
                ?.wholeText()
                ?.trim()
                .orEmpty()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    companion object {

        const val BLOOM_EDITABLE = "bloom-editable"

    }

}
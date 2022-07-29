package org.tyndalebt.spadv.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.tyndalebt.spadv.tools.file.storyRelPathExists

class BloomFrontCoverSlideBuilder : SlideBuilder() {

    lateinit var context: Context

    var lang = "*"

    fun build(context: Context, file: DocumentFile, html: Document): Slide? {
        this.context = context
        return html.getElementsByAttributeValueContaining(CLASS, OUTSIDE_FRONT_COVER).firstOrNull()?.let {
            buildSlide(file, html, it)
        }
    }

    private fun buildSlide(file: DocumentFile, html: Document, outsideFrontCover: Element): Slide {
        val slideSubtitle = buildSubtitle(outsideFrontCover).orEmpty()
        val slideContent = buildContent(html)
        lang = getContentLanguage(html)
        val frontCoverContent = FrontCoverContent(slideContent, file.name.orEmpty(), slideSubtitle, lang)

        return Slide().apply {
            slideType = SlideType.FRONTCOVER
            subtitle = slideSubtitle
            content = buildTitleIdeas(frontCoverContent)
            narrationFile = buildNarrationFile(file, html, lang).orEmpty()
            reference = frontCoverContent.scriptureReference
            parsePage(context, frontCoverContent.graphic.startsWith("front"), outsideFrontCover, this, file)
        }
    }

    internal fun buildSubtitle(outsideFrontCover: Element): String? {
        val smallCoverCredits = outsideFrontCover.getElementsByAttributeValueContaining(DATA_BOOK, SMALL_COVER_CREDITS)
        for (credit in smallCoverCredits) {
            credit.children().firstOrNull()?.wholeText()?.also {
                if (it.isNotEmpty()) {
                    return it
                }
            }
        }

        return null
    }

    internal fun buildContent(html: Document): Elements? {
        return bloomDataDiv(html)
                ?.children()
    }

    internal fun bloomDataDiv(html: Document): Element? {
        return html.getElementById(BLOOM_DATA_DIV)
    }

    internal fun buildTitleIdeas(frontCoverContent: FrontCoverContent): String {
        return frontCoverContent.run {
            "$titleIdeasHeading\n${titleIdeas.joinToString("\n")}"
        }
    }

    internal fun getContentLanguage(html: Document): String {
        return bloomDataDiv(html)
                ?.children()
                ?.find { it.attr(DATA_BOOK) == CONTENT_LANGUAGE_1 }                         // <div data-book="contentLanguage1"
                ?.wholeText()
                ?.trim()
                ?: lang
    }

    internal fun buildNarrationFile(file: DocumentFile, html: Document, lang: String): String? {
        return bloomDataDiv(html)
                ?.children()
                ?.find { it.attr(DATA_BOOK) == TITLE_IDEA_1 && it.attr(LANG) == lang }      // <div data-book="spTitleIdea1" lang="*">
                ?.children()?.firstOrNull()                                                 // <p>
                ?.children()?.firstOrNull()                                                 // <span>
                ?.id()
                ?.let { "audio/$it.mp3" }
                ?.let { if (storyRelPathExists(context, it, file.name.orEmpty())) it else null }
    }

    companion object {

        const val CLASS = "class"
        const val DATA_BOOK = "data-book"
        const val OUTSIDE_FRONT_COVER = "outsideFrontCover"
        const val SMALL_COVER_CREDITS = "smallCoverCredits"
        const val BLOOM_DATA_DIV = "bloomDataDiv"
        const val CONTENT_LANGUAGE_1 = "contentLanguage1"
        const val TITLE_IDEA_1 = "spTitleIdea1"

    }

}
package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.sil.storyproducer.tools.file.storyRelPathExists

class BloomFrontCoverSlideBuilder {

    lateinit var context: Context

    fun build(context: Context, file: DocumentFile, html: Document): Slide? {
        this.context = context
        return html.getElementsByAttributeValueContaining(CLASS, OUTSIDE_FRONT_COVER).firstOrNull()?.let {
            buildSlide(file, html, it)
        }
    }

    private fun buildSlide(file: DocumentFile, html: Document, outsideFrontCover: Element): Slide {
        val subtitle = buildSubtitle(outsideFrontCover).orEmpty()
        val content = buildContent(html)
        val frontCoverContent = FrontCoverContent(content, file.name.orEmpty(), subtitle)

        return buildSlide(file, html, outsideFrontCover, subtitle, frontCoverContent)
    }

    private fun buildSlide(file: DocumentFile, html: Document, outsideFrontCover: Element, slideSubtitle: String, frontCoverContent: FrontCoverContent): Slide {
        return Slide().apply {
            slideType = SlideType.FRONTCOVER
            subtitle = slideSubtitle
            content = buildTitleIdeas(frontCoverContent)
            narrationFile = buildNarrationFile(file, html).orEmpty()
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

    internal fun buildContent(html: Document): String {
        return fourthPageOfTranslationInstructions(html)
                ?.wholeText()
                .orEmpty()
                .trim()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    internal fun fourthPageOfTranslationInstructions(html: Document): Element? {
        return html.getElementsByAttributeValueContaining(CLASS, SCREEN_ONLY)
                .getOrNull(3)
                ?.getElementsByAttributeValueContaining(CLASS, BLOOM_TRANSLATION_GROUP)
                ?.firstOrNull()
    }

    internal fun buildTitleIdeas(frontCoverContent: FrontCoverContent): String {
        return frontCoverContent.run {
            "$titleIdeasHeading\n${titleIdeas.joinToString("\n")}"
        }
    }

    internal fun buildNarrationFile(file: DocumentFile, html: Document): String? {
        return fourthPageOfTranslationInstructions(html)
                ?.children()
                ?.find { it.wholeText().contains(TITLE_IDEA_1) }
                ?.children()
                ?.find { it.wholeText().startsWith(TITLE_IDEA_1) }
                ?.children()
                ?.firstOrNull()
                ?.id()
                ?.let { "audio/$it.mp3" }
                ?.let { if (storyRelPathExists(context, it, file.name!!)) it else null }
    }

    companion object {

        const val CLASS = "class"
        const val DATA_BOOK = "data-book"
        const val OUTSIDE_FRONT_COVER = "outsideFrontCover"
        const val SMALL_COVER_CREDITS = "smallCoverCredits"
        const val SCREEN_ONLY = "screen-only"
        const val BLOOM_TRANSLATION_GROUP = "bloom-translationGroup"
        const val TITLE_IDEA_1 = "TitleIdea1"

    }

}
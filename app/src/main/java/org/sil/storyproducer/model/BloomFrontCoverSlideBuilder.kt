package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BloomFrontCoverSlideBuilder {

    fun build(context: Context, file: DocumentFile, html: Document): Slide? {
        return html.getElementsByAttributeValueContaining(CLASS, OUTSIDE_FRONT_COVER).firstOrNull()?.let {
            buildSlide(context, file, html, it)
        }
    }

    private fun buildSlide(context: Context, file: DocumentFile, html: Document, outsideFrontCover: Element): Slide {
        val slide = Slide()
        slide.slideType = SlideType.FRONTCOVER

        val content = buildContent(html)
        val frontCoverContent = FrontCoverContent(content)
        val frontCoverGraphicProvided = frontCoverContent.graphic.orEmpty().startsWith("front")
        slide.content = buildTitleIdeas(frontCoverContent)
        slide.reference = frontCoverContent.scriptureReference.orEmpty()

        parsePage(context, frontCoverGraphicProvided, outsideFrontCover, slide, file)

        val smallCoverCredits = outsideFrontCover.getElementsByAttributeValueContaining(DATA_BOOK, SMALL_COVER_CREDITS)
        for (credit in smallCoverCredits) {
            credit.children().firstOrNull()?.wholeText()?.also {
                if (it.isNotEmpty()) {
                    slide.subtitle = it
                }
            }
        }

        return slide
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

    companion object {

        const val CLASS = "class"
        const val DATA_BOOK = "data-book"
        const val OUTSIDE_FRONT_COVER = "outsideFrontCover"
        const val SMALL_COVER_CREDITS = "smallCoverCredits"
        const val SCREEN_ONLY = "screen-only"
        const val BLOOM_TRANSLATION_GROUP = "bloom-translationGroup"

    }

}
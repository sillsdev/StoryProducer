package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class BloomFrontCoverSlideBuilder {

    fun build(context: Context, storyPath: DocumentFile, titlePage: Element, screen_only: Elements): Slide {
        val slide = Slide()
        slide.slideType = SlideType.FRONTCOVER

        //get title ideas - the 4th element, if there is one.
        var content = ""
        if (screen_only.size == 4) {
            val tgroup = screen_only[3].getElementsByAttributeValueContaining("class", "bloom-translationGroup")
            if (tgroup.size >= 1) {
                content = tgroup[0].wholeText().trim().replace("\\s*\\n\\s*".toRegex(), "\n")
            }
        }

        val frontCoverContent = FrontCoverContent(content)
        val frontCoverGraphicProvided = frontCoverContent.graphic.orEmpty().startsWith("front")
        slide.content = buildTitleIdeas(frontCoverContent)
        slide.reference = frontCoverContent.scriptureReference.orEmpty()

        parsePage(context, frontCoverGraphicProvided, titlePage, slide, storyPath)

        val smallCoverCredits = titlePage.getElementsByAttributeValueContaining("data-book", "smallCoverCredits")
        for (credit in smallCoverCredits) {
            credit.children().firstOrNull()?.wholeText()?.also {
                if (it.isNotEmpty()) {
                    slide.subtitle = it
                }
            }
        }

        return slide
    }

    internal fun buildTitleIdeas(frontCoverContent: FrontCoverContent): String {
        return frontCoverContent.run {
            "$titleIdeasHeading\n${titleIdeas.joinToString("\n")}"
        }
    }

}
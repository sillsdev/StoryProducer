package org.sil.storyproducer.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class NumberedPageSlideBuilder {

    lateinit var context: Context

    fun build(context: Context, file: DocumentFile, page: Element): Slide? {
        this.context = context
        return buildSlide(file, page)
    }

    private fun buildSlide(file: DocumentFile, page: Element): Slide? {
        val slide = Slide()
        slide.slideType = SlideType.NUMBEREDPAGE

        if (!parsePage(context, false, page, slide, file)) {
            return null
        }

        page.getElementsByAttributeValueContaining("class", "audio-sentence").also {
            slide.content = buildContent(it)
            slide.reference = buildScriptureReference(it)
        }

        return slide
    }

    private fun buildContent(audioSentences: Elements): String {
        return audioSentences
                .getOrNull(0)
                ?.wholeText()
                .orEmpty()
                .trim()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

    private fun buildScriptureReference(audioSentences: Elements): String {
        return audioSentences
                .getOrNull(1)
                ?.wholeText()
                .orEmpty()
                .trim()
                .replace("\\s*\\n\\s*".toRegex(), "\n")
    }

}
package org.tyndalebt.spadv.model

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class FrontCoverContent(
        val content: Elements?,
        val storyTitle: String,
        storyScriptureReference: String,
        val lang: String
) {

    val lines: List<Pair<String, String>> = content.orEmpty()
            .filter(::byLang)
            .map { keywordOf(it) to valueOf(it) }

    val graphic: String = firstValueWithKeyword(GRAPHIC)
            ?.let { if (it.startsWith(GRAY) || it.startsWith(FRONT)) it else null }
            ?: DEFAULT_GRAPHIC

    val scriptureReference: String = firstValueWithKeyword(SCRIPTURE_REFERENCE)
            ?: storyScriptureReference

    val titleIdeasHeading: String = firstValueWithKeyword(TITLE_IDEAS_HEADING)
            ?: DEFAULT_TITLE_IDEAS_HEADING

    val titleIdeas: List<String> = lines
            .filterNot { it.first.equals(TITLE_IDEAS_HEADING, true) }
            .filter { it.first.startsWith(TITLE_IDEA, true) }
            .map { it.second }
            .ifEmpty { listOf(storyTitle) }



    private fun firstValueWithKeyword(keyword: String): String? = lines
            .firstOrNull { it.first.equals(keyword, true) }
            ?.second

    private fun keywordOf(element: Element): String = element.attributes().find { it.key == DATA_BOOK }
            ?.value
            .orEmpty()
            .trim()
            .trim('\u200C')

    private fun valueOf(element: Element): String = element.wholeText()
            .trim()
            .trim('\u200C', '"', '\'')

    private fun byLang(element: Element): Boolean {
        return element.attr(LANG) == lang
                || (element.attr(LANG) == "*" && element.attr(DATA_BOOK) == GRAPHIC)
    }

    companion object {

        const val DATA_BOOK = "data-book"
        const val LANG = "lang"
        const val GRAPHIC = "spGraphic"
        const val SCRIPTURE_REFERENCE = "spReference"
        const val TITLE_IDEAS_HEADING = "spTitleIdeasHeading"
        const val TITLE_IDEA = "spTitleIdea"

        const val GRAY = "gray"
        const val FRONT = "front"

        const val DEFAULT_GRAPHIC = "gray-background"
        const val DEFAULT_TITLE_IDEAS_HEADING = "Title ideas:"

    }

}
package org.sil.storyproducer.model

class FrontCoverContent(
        val content: String,
        val storyTitle: String,
        val storyScriptureReference: String
) {

    val lines: List<Pair<String, String>> = content.split("\n")
            .map { it.split("=")  }
            .map { keywordOf(it) to valueOf(it) }

    val graphic: String = firstValueWithKeyword(GRAPHIC)
            ?.let { if (it.startsWith("gray") || it.startsWith("front")) it else null }
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

    fun firstValueWithKeyword(keyword: String): String? = lines
            .firstOrNull { it.first.equals(keyword, true) }
            ?.second

    fun keywordOf(parts: List<String>): String = parts.getOrNull(0).orEmpty().trim()
            .trim('\u200C')

    fun valueOf(parts: List<String>): String = parts.getOrNull(1).orEmpty().trim()
            .trim('\u200C', '"', '\'')

    companion object {

        const val GRAPHIC = "Graphic"
        const val SCRIPTURE_REFERENCE = "ScriptureReference"
        const val TITLE_IDEAS_HEADING = "TitleIdeasHeading"
        const val TITLE_IDEA = "TitleIdea"

        const val DEFAULT_GRAPHIC = "gray-background"
        const val DEFAULT_TITLE_IDEAS_HEADING = "Title ideas:"

    }

}
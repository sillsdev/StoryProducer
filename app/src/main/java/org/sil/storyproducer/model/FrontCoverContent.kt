package org.sil.storyproducer.model

class FrontCoverContent(
        val content: String
) {

    val lines: List<Pair<String, String>> = content.split("\n")
            .map { it.split("=")  }
            .map { keywordOf(it) to valueOf(it) }

    val graphic: String? = firstValueWithKeyword(GRAPHIC)

    val scriptureReference: String? = firstValueWithKeyword(SCRIPTURE_REFERENCE)

    val titleIdeasHeading: String? = firstValueWithKeyword(TITLE_IDEAS_HEADING)

    val titleIdeas: List<String> = lines
            .filterNot { it.first.equals(TITLE_IDEAS_HEADING, true) }
            .filter { it.first.startsWith(TITLE_IDEA, true) }
            .map { it.second }

    fun firstValueWithKeyword(keyword: String): String? = lines
            .firstOrNull { it.first.equals(keyword, true) }
            ?.second

    fun keywordOf(parts: List<String>): String = parts.getOrNull(0).orEmpty().trim().trim('\u200C')

    fun valueOf(parts: List<String>): String = parts.getOrNull(1).orEmpty().trim().trim('\u200C')

    companion object {

        const val GRAPHIC = "Graphic"
        const val SCRIPTURE_REFERENCE = "ScriptureReference"
        const val TITLE_IDEAS_HEADING = "TitleIdeasHeading"
        const val TITLE_IDEA = "TitleIdea"

    }

}
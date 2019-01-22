package org.sil.storyproducer.model

class TermTree {
    private val root: WordNode = WordNode()

    fun insertTerm(term: String){
        val words = splitBeforeAndAfterAnyNonLetters(term).toMutableList()
        insertTerm(words, root)
    }

    private fun insertTerm(words: MutableList<String>, currentNode: WordNode){
        if(words.size > 0) {
            val word = words.removeAt(0)
            val nextNode = currentNode.childWords[word] ?: WordNode()
            currentNode.childWords[word] = nextNode
            insertTerm(words, nextNode)
        }
    }

    fun splitBeforeAndAfterAnyNonLetters(text: String): List<String>{
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }
        return words
    }
}
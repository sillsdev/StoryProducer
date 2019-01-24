package org.sil.storyproducer.model

class TermTree {
    private val root: WordNode = WordNode()

    fun insertTerm(term: String){
        val words = splitBeforeAndAfterAnyNonLetters(term)
        var currentNode = root

        for(word in words){
            val nextNode = currentNode.childWords[word] ?: WordNode()
            currentNode.childWords[word] = nextNode
            currentNode = nextNode
        }
        currentNode.isKeyterm = true
    }

    fun splitOnKeyterms(text: String): List<String>{
        val words = splitBeforeAndAfterAnyNonLetters(text)
        val resultPhrases: MutableList<String> = mutableListOf()
        var nonKeytermPhrase = ""

        while(words.size > 0) {
            val keytermPhrase = getIfKeyterm(words, root)
            if(keytermPhrase == ""){
                nonKeytermPhrase += words.removeAt(0)
            }
            else{
                resultPhrases.add(nonKeytermPhrase)
                resultPhrases.add(keytermPhrase)
                nonKeytermPhrase = ""
            }
        }
        resultPhrases.add(nonKeytermPhrase)

        return resultPhrases
    }

    private fun getIfKeyterm(words: MutableList<String>, currentNode: WordNode): String{
        if(words.isNotEmpty()){
            val word = words.removeAt(0)

            if(currentNode.childWords.containsKey(word.toLowerCase())){
                val nextNode = currentNode.childWords[word.toLowerCase()]!!
                val keyterm = getIfKeyterm(words, nextNode)

                if(nextNode.isKeyterm || keyterm != ""){
                    return word + keyterm
                }
            }

            words.add(0, word)
        }

        return ""
    }

    private fun splitBeforeAndAfterAnyNonLetters(text: String): MutableList<String>{
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }

        return words
    }

    private class WordNode {
        var isKeyterm: Boolean = false
        var childWords: MutableMap<String, WordNode> = mutableMapOf()
    }
}
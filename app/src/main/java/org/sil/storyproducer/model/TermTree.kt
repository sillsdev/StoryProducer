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
    }

    //TODO Refactor
    fun splitOnKeyterms(text: String): List<String>{
        val words = splitBeforeAndAfterAnyNonLetters(text)
        val resultPhrases: MutableList<String> = mutableListOf()
        var currentNode = root
        var currentPhrase: MutableList<String> = mutableListOf()

        while(words.size > 0) {
            val word = words.removeAt(0)
            if(currentNode.childWords.containsKey(word.toLowerCase())){
                currentPhrase.add(word)

                currentNode = currentNode.childWords[word.toLowerCase()]!!
            }
            else if(currentNode.childWords.isEmpty()){
                resultPhrases.add(currentPhrase.fold(""){
                    result, word -> result + word
                })

                words.add(0, word)

                currentPhrase = mutableListOf()
                currentNode = root
            }
            else{
                currentPhrase.add(word)

                resultPhrases.add(currentPhrase.removeAt(0))

                words.addAll(0, currentPhrase)

                currentPhrase = mutableListOf()
                currentNode = root
            }
        }

        return resultPhrases
    }

    private fun splitBeforeAndAfterAnyNonLetters(text: String): MutableList<String>{
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }

        return words
    }

    private class WordNode {
        var childWords: MutableMap<String, WordNode> = mutableMapOf()
    }
}
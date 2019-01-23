package org.sil.storyproducer.model

class TermTree {
    private val root: WordNode = WordNode()

    fun insertTerm(term: String){
        val words = splitBeforeAndAfterAnyNonLetters(term)
        insertTerm(words, root)
    }

    //TODO Refactor
    fun searchParseKeytermThingy(text: String): List<String>{
        val words = splitBeforeAndAfterAnyNonLetters(text)
        val things: MutableList<String> = mutableListOf()

        var currentNode = root
        var thing: MutableList<String> = mutableListOf()
        while(words.size > 0) {
            val word = words.removeAt(0)
            if(currentNode.childWords.containsKey(word.toLowerCase())){
                thing.add(word)
                currentNode = currentNode.childWords[word.toLowerCase()]!!
            }
            else if(currentNode.childWords.isEmpty()){
                things.add(thing.fold(""){
                    result, word -> result + word
                })
                words.add(0, word)
                thing = mutableListOf()
                currentNode = root
            }
            else{
                thing.add(word)
                things.add(thing.removeAt(0))
                words.addAll(0, thing)
                thing = mutableListOf()
                currentNode = root
            }
        }

        return things
    }

    private fun splitBeforeAndAfterAnyNonLetters(text: String): MutableList<String>{
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }
        return words
    }

    private fun insertTerm(words: MutableList<String>, currentNode: WordNode){
        if(words.size > 0) {
            val word = words.removeAt(0)
            val nextNode = currentNode.childWords[word] ?: WordNode()
            currentNode.childWords[word] = nextNode
            insertTerm(words, nextNode)
        }
    }

    private class WordNode {
        var childWords: MutableMap<String, WordNode> = mutableMapOf()
    }
}
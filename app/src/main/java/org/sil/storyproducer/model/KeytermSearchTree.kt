package org.sil.storyproducer.model

/*
 * The purpose of this class is to make searching for multi-word keyterms possible and fast within a larger paragraph of text.
 * Consider a list of keyterms. Each keyterm gets split into a list of words.
 * The point of this class is to quickly differentiate between keyterms that begin with the same list of words.
 * Example: feast, Feast of Pentecost, Feast of Shelters
 *
 * In this class a word refers to a consecutive string of letters, or any individual non-letter symbol.
 *
 * When building the tree all starting words of keyterms that are the same would get combined into a single node.
 * The children of that node would be all the possible next words that could come after the starting word to make up a keyterm.
 * When searching we would traverse this tree word by word as long as we get words that match.
 * This will get us the longest match- the longest possible keyterm from a given list of words.
 */
class KeytermSearchTree {
    private val root: WordNode = WordNode()

    /*
     * This function takes in a single term and builds up the tree based on the words in that term.
     * The tree would be as deep as the term with the most words.
     * If a given word already exists in current node's map, navigate down that node, otherwise create a new node.
     */
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

    /*
     * The purpose of this function is to split a paragraph of text into a list of keyterms and non-keyterm strings between those keyterms.
     */
    fun splitOnKeyterms(text: String): List<String>{
        val words = splitBeforeAndAfterAnyNonLetters(text)
        val resultPhrases: MutableList<String> = mutableListOf()
        var nonKeytermPhrase = ""

        while(words.size > 0) {
            val keytermPhrase = getIfKeyterm(words, root)
            // If the returned keyterm is empty, no keyterm was found.
            // All parsed words are reinserted onto the list of terms to parse.
            // The first word is removed as this word is guaranteed to not be part of a keyterm.
            // That first word is appended to the string of words since the last keyterm was found.
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

    /*
     * The purpose of this function is to return the longest possible term made up of the next consecutive words left to parse.
     * It returns an empty string if no keyterm can be found
     * and any consumed words are reinserted into the main list of words that have yet to be parsed.
     *
     * This function is recursive. It will navigate down the tree as long as
     * the next word to be parsed matches a word in the current node's map.
     * When this occurs, the function will propagate back up until it finds a node that is the end of a keyterm
     * and will then begin appending words to its return value.
     */
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

    /*
     * This function splits a given string so that all consecutive characters that are letters are split into their own strings.
     * Every other individual character (",", ".", " ", etc.) are split into their own strings.
     * This makes it possible to separate symbols from words.
     * Example: Jesus's Spirit -> "Jesus", "'", "s", " ", "Spirit"
     *
     * A list of strings is returned
     */
    private fun splitBeforeAndAfterAnyNonLetters(text: String): MutableList<String>{
        // This regex uses lookahead and lookbehind characters to check if any character has a non-letter before or after it
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }

        return words
    }


    private class WordNode {
        var isKeyterm: Boolean = false
        /*
         * The key in the map and it's corresponding WordNode together represent a word.
         * The word string isn't stored in the WordNode itself as it isn't needed in the algorithm.
         * A map is used because it can be quickly searched by a key which would be the next words.
         * A word could have any number of next words because multiple keyterms could start with the same word.
         */
        var childWords: MutableMap<String, WordNode> = mutableMapOf()
    }
}
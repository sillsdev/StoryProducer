package org.tyndalebt.spadv.model

/**
 * The purpose of this class is to make searching for multi-word wordlinks possible and fast within a larger paragraph of text.
 * The point of this class is to quickly differentiate between similar wordlinks (ones which begin with the same words).
 * Example: feast, Feast of Pentecost, Feast of Shelters
 *
 * In this class a word refers to a consecutive string of letters, or any individual non-letter symbol.
 *
 * When building the WordLinkSearchTree (WordLink Search Tree) all starting words of wordlinks that are the same would get combined into a single node.
 * The children of that node would be all the possible next words that could come after the starting word to make up a wordlinks.
 * When searching we would traverse this tree word by word as long as we get words that match.
 * This will get us the longest match - the longest possible wordlinks from a given list of words.
 */
class WordLinkSearchTree {
    private val root: WordNode = WordNode()

    /**
     * This function takes in a single term and builds up the tree based on the words in that term.
     * The tree would be as deep as the term with the most words.
     * If a given word already exists in current node's map, navigate down that node, otherwise create a new node.
     *
     * @param word the word to insert into the tree
     */
    fun insertTerm(word: String) {
        val words = splitBeforeAndAfterAnyNonLetters(word)
        var currentNode = root

        for (w in words) {
            val nextNode = currentNode.childWords[w.toLowerCase()] ?: WordNode()
            currentNode.childWords[w.toLowerCase()] = nextNode
            currentNode = nextNode
        }
        currentNode.isWordLink = true
    }

    /**
     * The purpose of this function is to split a paragraph of text into a list of wordlinks and non-wordlinks strings between those wordlinks.
     *
     * @param text the text containing wordlinks
     * @return A list of Strings
     */
    fun splitOnWordLinks(text: String): List<String> {
        val words = splitBeforeAndAfterAnyNonLetters(text)
        val resultPhrases: MutableList<String> = mutableListOf()
        var nonWordLinkPhrase = ""

        while (words.size > 0) {
            val wordLinkPhrase = getIfWordLink(words, root)
            // If the returned wordlink is empty, no wordlink was found.
            // All parsed words are reinserted onto the list of terms to parse.
            // The first word is removed as this word is guaranteed to not be part of a wordlink.
            // That first word is appended to the string of words since the last wordlink was found.
            if(wordLinkPhrase == ""){
                nonWordLinkPhrase += words.removeAt(0)
            }
            else{
                resultPhrases.add(nonWordLinkPhrase)
                resultPhrases.add(wordLinkPhrase)
                nonWordLinkPhrase = ""
            }
        }
        resultPhrases.add(nonWordLinkPhrase)
        resultPhrases.removeAll{ it == "" }

        return resultPhrases
    }

    /**
     * The purpose of this function is to return the longest possible term made up of the next consecutive words left to parse.
     * It returns an empty string if no wordlink can be found
     * and any consumed words are reinserted into the main list of words that have yet to be parsed.
     *
     * This function is recursive. It will navigate down the tree as long as
     * the next word to be parsed matches a word in the current node's map.
     * When this occurs, the function will propagate back up until it finds a node that is the end of a wordlink
     * and will then begin appending words to its return value.
     *
     * @param words
     * @param currentNode
     * @return A string
     */
    private fun getIfWordLink(words: MutableList<String>, currentNode: WordNode): String{
        if(words.isNotEmpty()){
            val word = words.removeAt(0)

            if(currentNode.childWords.containsKey(word.toLowerCase())){
                val nextNode = currentNode.childWords[word.toLowerCase()]!!
                val wordLink = getIfWordLink(words, nextNode)

                if(nextNode.isWordLink || wordLink != ""){
                    return word + wordLink
                }
            }
            words.add(0, word)
        }
        return ""
    }

    /**
     * This function splits a given string so that all consecutive characters that are letters are split into their own strings.
     * Every other individual character (",", ".", " ", etc.) are split into their own strings.
     * This makes it possible to separate symbols from words.
     * Example: Jesus's Spirit -> "Jesus", "'", "s", " ", "Spirit"
     *
     * @param text
     * @return A list of strings
     */
    private fun splitBeforeAndAfterAnyNonLetters(text: String): MutableList<String>{
        // This regex uses lookahead and lookbehind characters to check if any character has a non-letter before or after it
        val words = text.split(Regex("(?![a-zA-Z])|(?<![a-zA-Z])")).toMutableList()
        words.removeAll { it == "" }
        return words
    }


    private class WordNode {
        var isWordLink: Boolean = false
        /*
         * The key in the map and it's corresponding WordNode together represent a word.
         * The word string isn't stored in the WordNode itself as it isn't needed in the algorithm.
         * A map is used because it can be quickly searched by a key which would be the next words.
         * A word could have any number of next words because multiple wordlinks could start with the same word.
         */
        var childWords: MutableMap<String, WordNode> = mutableMapOf()
    }
}
package org.sil.storyproducer.test.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.sil.storyproducer.model.KeytermSearchTree

@RunWith(RobolectricTestRunner::class)
class TestKeytermSearchTree{
    @Test
    fun splitOnKeyterms_When_TextIsOnlyTerm_Should_ReturnListWithOnlyTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "term"
        val expectedPhrases: List<String> = listOf("term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)
        
        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextIsEmpty_Should_ReturnEmptyList() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = ""
        val expectedPhrases: List<String> = listOf()

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_NoTerms_Should_ReturnText() {
        val keytermSearchTree = KeytermSearchTree()
        val textToSearch = "This is a test."
        val expectedPhrases: List<String> = listOf("This is a test.")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextHasNoTerms_Should_ReturnText() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "This is a test."
        val expectedPhrases: List<String> = listOf("This is a test.")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextHasTermFollowedByNonLetter_Should_SplitOnTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "term's"
        val expectedPhrases: List<String> = listOf("term", "'s")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextHasTermPrecededByNonLetter_Should_SplitOnTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "thing,term"
        val expectedPhrases: List<String> = listOf("thing,", "term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextHasTermFollowedByAndPrecededByNonLetter_Should_SplitOnTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "thing ,term's"
        val expectedPhrases: List<String> = listOf("thing ,", "term", "'s")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermHasNonLetter_Should_StillSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term's")
        val textToSearch = "the term's thing"
        val expectedPhrases: List<String> = listOf("the ", "term's", " thing")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermEndsWithNonLetter_Should_StillSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term.")
        val textToSearch = "This is a term."
        val expectedPhrases: List<String> = listOf("This is a ", "term.")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermStartsWithNonLetter_Should_StillSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("-term")
        val textToSearch = "Definition -term"
        val expectedPhrases: List<String> = listOf("Definition ", "-term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermStartsAndEndsWithNonLetter_Should_StillSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("-term.")
        val textToSearch = "Definition -term."
        val expectedPhrases: List<String> = listOf("Definition ", "-term.")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermContainsSixNonLettersInARow_Should_StillSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term,.--':abc")
        val textToSearch = "Definition term,.--':abc"
        val expectedPhrases: List<String> = listOf("Definition ", "term,.--':abc")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermInTextIsFollowedByLetter_Should_NotSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "terms"
        val expectedPhrases: List<String> = listOf("terms")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermInTextFollowsLetter_Should_NotSplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "keyterm"
        val expectedPhrases: List<String> = listOf("keyterm")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsWithTermFollowedByNonTerm_Should_ReturnListWithTermFollowedByNonTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "term is a cool word!!!"
        val expectedPhrases: List<String> = listOf("term", " is a cool word!!!")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsWithNonTermFollowedByTerm_Should_ReturnListWithNonTermFollowedByTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "This is a really cool term"
        val expectedPhrases: List<String> = listOf("This is a really cool ", "term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsAndEndsWithTerm_Should_ReturnListWithTermFollowedByNonTermFollowedByTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "term is a cool term"
        val expectedPhrases: List<String> = listOf("term", " is a cool ", "term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsAndEndsWithNonTerm_Should_ReturnListWithNonTermFollowedByTermFollowedByNonTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "This term is cool"
        val expectedPhrases: List<String> = listOf("This ", "term", " is cool")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsAndEndsWithDifferentTerms_Should_ReturnListWithTermFollowedByNonTermFollowedByDifferentTerm() {
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        keytermSearchTree.insertTerm("different")
        val textToSearch = "term is different"
        val expectedPhrases: List<String> = listOf("term", " is ", "different")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_CapitalizedTermAndTermCapitalizedInText_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("Term")
        val textToSearch = "this Term is here"
        val expectedPhrases: List<String> = listOf("this ", "Term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_CapitalizedTermAndTermLowerCaseInText_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("Term")
        val textToSearch = "this term is here"
        val expectedPhrases: List<String> = listOf("this ", "term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_LowerCaseTermAndTermCapitalizedInText_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "this Term is here"
        val expectedPhrases: List<String> = listOf("this ", "Term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_LowerCaseTermAndTermLowerCaseInText_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        val textToSearch = "this term is here"
        val expectedPhrases: List<String> = listOf("this ", "term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultipleTermsInText_Should_SplitOnTerms(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        keytermSearchTree.insertTerm("second")
        keytermSearchTree.insertTerm("third")
        val textToSearch = "This term is second, third."
        val expectedPhrases: List<String> = listOf("This ", "term", " is ", "second", ", ", "third", ".")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultipleTermsExistButNotAllInText_Should_SplitOnTerms(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        keytermSearchTree.insertTerm("second")
        keytermSearchTree.insertTerm("third")
        val textToSearch = "This term is only second."
        val expectedPhrases: List<String> = listOf("This ", "term", " is only ", "second", ".")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermWithTwoWords_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term")
        val textToSearch = "this cool term is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermWithNonLetter_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term's")
        val textToSearch = "this cool term's thing is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term's", " thing is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermWithThreeWords_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term thingy")
        val textToSearch = "this cool term thingy is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term thingy", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermsWithSameStartingWordExistButNotBothInText_Should_SplitOnCorrectTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term")
        keytermSearchTree.insertTerm("cool thing")
        val textToSearch = "this cool thing is here"
        val expectedPhrases: List<String> = listOf("this ", "cool thing", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermsWithSameStartingWordExistAndBothInText_Should_SplitOnCorrectTermInBothCases(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term")
        keytermSearchTree.insertTerm("cool thing")
        val textToSearch = "this cool thing is a cool term"
        val expectedPhrases: List<String> = listOf("this ", "cool thing", " is a ", "cool term")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermsWithSameStartingTwoWordsExistButNotBothInText_Should_SplitOnCorrectTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term thing")
        keytermSearchTree.insertTerm("cool term not")
        val textToSearch = "this cool term not is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term not", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_ThreeMultiWordTermsWithSameStartingTwoWordsExistButNotBothInText_Should_SplitOnCorrectTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term thing")
        keytermSearchTree.insertTerm("cool term not")
        keytermSearchTree.insertTerm("cool term yes")
        val textToSearch = "this cool term not is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term not", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermsWithDifferentStartingWordButRestOfTermIsTheSame_Should_SplitOnCorrectTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term")
        keytermSearchTree.insertTerm("weird term")
        val textToSearch = "this cool term is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermStartsWithASingleWordTerm_Should_SplitOnLongerTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term")
        keytermSearchTree.insertTerm("cool")
        val textToSearch = "this cool term is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermStartsWithAMultiWordTerm_Should_SplitOnLongerTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term thingy")
        keytermSearchTree.insertTerm("cool term")
        val textToSearch = "this cool term thingy is here"
        val expectedPhrases: List<String> = listOf("this ", "cool term thingy", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_MultiWordTermsOverlap_Should_SplitOnFirstTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool term thingy thing")
        keytermSearchTree.insertTerm("this cool term")
        val textToSearch = "this cool term thingy thing is here"
        val expectedPhrases: List<String> = listOf("this cool term", " thingy thing is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TextStartsWithPartialMultiWordTermThatOverlapWithTerm_Should_SplitOnActualTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("cool thingy thing")
        keytermSearchTree.insertTerm("this cool term")
        val textToSearch = "this cool thingy thing is here"
        val expectedPhrases: List<String> = listOf("this ", "cool thingy thing", " is here")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermWithinPartialMultiWordTerm_Should_SplitOnActualTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("term")
        keytermSearchTree.insertTerm("cool term thingy thing")
        val textToSearch = "this cool term thingy not thing"
        val expectedPhrases: List<String> = listOf("this cool ", "term", " thingy not thing")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }

    @Test
    fun splitOnKeyterms_When_TermWithTenWords_Should_SplitOnTerm(){
        val keytermSearchTree = KeytermSearchTree()
        keytermSearchTree.insertTerm("this is a long term with many words within it")
        val textToSearch = "Maybe this is a long term with many words within it and ends with a period."
        val expectedPhrases: List<String> = listOf("Maybe ", "this is a long term with many words within it", " and ends with a period.")

        val actualPhrases = keytermSearchTree.splitOnKeyterms(textToSearch)

        Assert.assertEquals(expectedPhrases, actualPhrases)
    }
}

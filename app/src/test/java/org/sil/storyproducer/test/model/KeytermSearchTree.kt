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
}
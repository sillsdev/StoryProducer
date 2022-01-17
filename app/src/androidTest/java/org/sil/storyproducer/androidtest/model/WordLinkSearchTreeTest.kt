package org.sil.androidtest.model

import org.sil.storyproducer.model.WordLinkSearchTree

class WordLinkSearchTreeTest {
// It's not clear to me how to actually test insertTerm. What does it do?
// @Test fun insertTerm_SingleWord

    @Test fun splitOnWordLinks_SingleWord {
        val words = WordLinkSearchTree.splitOnWordLinks("Example Here")
        assertEquals(words, "Example")
    }
}

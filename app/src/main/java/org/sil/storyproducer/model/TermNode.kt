package org.sil.storyproducer.model

class WordNode {
    var childWords: MutableMap<String, WordNode> = mutableMapOf()
}
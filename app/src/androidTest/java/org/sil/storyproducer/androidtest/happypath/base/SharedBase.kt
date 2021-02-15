package org.sil.storyproducer.androidtest.happypath.base

interface SharedBase {
    fun getStoryName() : String
    fun getStoryDirectory() : String
    fun getExportVideoName() : String

    fun getSlideNumberId() : Int
    fun getPlayerSeekbarId() : Int
    fun getPlaybackButtonId() : Int
}
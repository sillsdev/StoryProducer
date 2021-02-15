package org.sil.storyproducer.androidtest.happypath.base

import org.junit.Before
import org.junit.BeforeClass
import org.sil.storyproducer.film.R

open class PhotoBase : SharedBase {

    private val nameOfTestStory = "Lost Coin"
    private val nameOfTestStoryDirectory = "002 Lost Coin"
    private val nameOfSampleExportVideo = "LostCoinSample.mp4"

    override fun getStoryName() : String {
        return nameOfTestStory
    }

    override fun getStoryDirectory() : String {
        return nameOfTestStoryDirectory
    }

    override fun getExportVideoName() : String {
        return nameOfSampleExportVideo
    }

    override fun getSlideNumberId() : Int {
        return R.id.slide_player_slide_number
    }

    override fun getPlayerSeekbarId(): Int {
        return R.id.slide_player_seekbar
    }

    override fun getPlaybackButtonId(): Int {
        return R.id.slide_player_playback_button
    }

}
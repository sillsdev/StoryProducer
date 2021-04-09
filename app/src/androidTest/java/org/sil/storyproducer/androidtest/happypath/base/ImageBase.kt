package org.sil.storyproducer.androidtest.happypath.base

import org.sil.storyproducer.R

open class ImageBase : SharedBase {

    private val nameOfTestStory = "Lost Coin"
    private val nameOfTestStoryDirectory = "002 Lost Coin"
    private val nameOfSampleExportVideo = "Lost_Coin.mp4"

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
        return R.id.slide_number_text
    }

    override fun getPlayerSeekbarId(): Int {
        return R.id.videoSeekBar
    }

    override fun getPlaybackButtonId(): Int {
        return R.id.fragment_reference_audio_button
    }

}
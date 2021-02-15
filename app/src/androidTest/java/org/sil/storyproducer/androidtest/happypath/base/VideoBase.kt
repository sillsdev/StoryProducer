package org.sil.storyproducer.androidtest.happypath.base

import org.junit.Before
import org.junit.BeforeClass
import org.sil.storyproducer.film.R

open class VideoBase : SharedBase {

    private val nameOfTestStory = "Prophets Story"
    private val nameOfTestStoryDirectory = "033 Prophets Story (no LWC)"
    private val nameOfSampleExportVideo = "ProphetsStory.mp4"

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
        return R.id.video_player_slide_number
    }

    override fun getPlayerSeekbarId(): Int {
        return R.id.video_player_seekbar
    }

    override fun getPlaybackButtonId(): Int {
        return R.id.video_player_play_pause_button
    }

}
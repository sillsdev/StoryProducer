package org.sil.storyproducer.androidtest.happypath.base

import org.sil.storyproducer.R

open class ImageBase : SharedBase {

    // 09/11/2021 - DKH: Update for Testing Abstraction #566
    // ALL_STORIES, IN_PROGRESS, COMPLETED are the tabs in the "Story Templates" view
    //  that contain the story titles.
    // The same exact title can appear in two places, eg, once in
    //     ALL_STORIES tab and once in IN_PROGRESS tab.  When running the Espresso test,
    //     the Espresso title picker throws an exception because it cannot
    //     differentiate between the same title in the ALL_STORIES tab and the
    //     IN_PROGRESS tab.
    // To provide more uniqueness, add one space to the title in the IN_PROGRESS tab, two
    //     spaces to the title in the COMPLETED tab and three spaces to any title in other
    //     tabbed ENUMS.
    //     The users will not see a difference but the Espresso software will be
    //     able to pick the appropriate story/tab combo.
    // Future Espresso tests will have to add spaces to "nameOfTestStory" to pick them from the
    //     IN_PROGRESS tab or the COMPLETED tab

    // 09/02/2021 - DKH: Update for Testing Abstraction #566
    // Precisely identify story string for automated comparison during Espresso testing
    private val nameOfTestStory = "002 Lost Coin" // this only matches stories in ALL_STORIES tab
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

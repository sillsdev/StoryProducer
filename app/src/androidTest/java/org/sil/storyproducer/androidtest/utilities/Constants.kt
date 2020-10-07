package org.sil.storyproducer.androidtest.utilities

import android.content.Context
import android.content.res.Resources
import androidx.test.InstrumentationRegistry

object Constants {
    // The duration for which a clip  gets played should be shorter than the
    // corresponding duration for which a clip gets recorded.
    // Durations are in milliseconds
    const val durationToPlayNarration: Long = 100
    const val durationToPlayTranslatedClip: Long = 100
    const val durationToRecordLearnClip: Long = 1500
    const val durationToRecordTranslatedClip: Long = durationToRecordLearnClip
    const val durationToRecordFeedbackClip: Long = 250
    const val durationToRecordVoiceStudioClip: Long = 250
    const val durationToWaitForVideoExport: Long = 60000
    const val intervalToWaitBetweenCheckingForVideoExport: Long = 1000

    const val durationToWaitWhenSwipingBetweenSlides: Long = 200 //Swipe.FAST = 100 * 2

    const val numberOfTimesToSwipeWhenApprovingAllSlides: Int = 6

    const val nameOfTestStory = "Lost Coin"

    const val nameOfTestStoryDirectory = "a000 Lost Coin Eng"
    const val nameOfSampleExportVideo = "LostCoinSample.mp4"

    val storageRoots = arrayOf(
            "mnt/sdcard",
            "storage/removable_SD Card",
            "storage",
            "mnt/SDCARD")
    var storage = storageRoots[3]
    val workspaceDirectory : String
        get() {return "$storage/SPWorkspace"}
    val espressoResourceDirectory : String
        get() {return "$storage/EspressoResources"}
    val exportedVideosDirectory : String
        get() {return "$workspaceDirectory/videos"}

    val resources: Resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    object Phase {
        const val learn = "Learn"
        const val translate = "Translate + Revise"
        const val communityWork = "Community Work"
        const val accuracyCheck = "Accuracy Check"
        const val voiceStudio = "Voice Studio"
        const val finalize = "Finalize"
        const val share = "Share"
    }

}

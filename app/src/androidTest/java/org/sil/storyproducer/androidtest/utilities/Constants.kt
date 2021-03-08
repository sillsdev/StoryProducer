package org.sil.storyproducer.androidtest.utilities

import android.content.res.Resources
import androidx.test.InstrumentationRegistry

object Constants {

    // DO NOT CHANGE THIS VALUE
    var workspaceIsInitialized = false

    // The duration for which a clip  gets played should be shorter than the
    // corresponding duration for which a clip gets recorded.
    // Durations are in milliseconds
    const val durationToPlayNarration: Long = 100
    const val durationToPlayTranslatedClip: Long = 1500
    const val durationToRecordLearnClip: Long = 1500
    const val durationToRecordTranslatedClip: Long = durationToRecordLearnClip
    const val durationToRecordFeedbackClip: Long = 250
    const val durationToRecordVoiceStudioClip: Long = 250
    const val durationToWaitForVideoExport: Long = 150000
    const val intervalToWaitBetweenCheckingForVideoExport: Long = 1000

    const val durationToWaitWhenSwipingBetweenSlides: Long = 200 //Swipe.FAST = 100 * 2

    const val numberOfTimesToSwipeWhenApprovingAllSlides: Int = 6

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
        const val translate = "Translate"
        const val communityWork = "Community Work"
        const val accuracyCheck = "Accuracy Check"
        const val voiceStudio = "Voice Studio"
        const val finalize = "Finalize"
        const val share = "Share"
        const val reviewAdjust = "Review + Adjust"
    }

}

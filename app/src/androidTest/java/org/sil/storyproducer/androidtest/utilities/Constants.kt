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
    const val durationToPlayTranslatedClip: Long = 750
    const val durationToRecordLearnClip: Long = 1500
    const val durationToRecordTranslatedClip: Long = durationToRecordLearnClip
    const val durationToRecordFeedbackClip: Long = 250
    const val durationToRecordVoiceStudioClip: Long = 250
    const val durationToWaitForVideoExport: Long = 200000
    const val intervalToWaitBetweenCheckingForVideoExport: Long = 1000

    const val durationToWaitWhenSwipingBetweenSlides: Long = 200 //Swipe.FAST = 100 * 2
    // 09/17/2021 - DKH: Update for Testing Abstraction #566
    // numberOfTimesToSwipeWhenApprovingAllSlides is very story template specific,
    //  eg, Lost Coin only has 6 slides.  Other stories with different slide counts will not work
    const val numberOfTimesToSwipeWhenApprovingAllSlides: Int = 6

    // 09/17/2021 - DKH: Update for Testing Abstraction #566
    // This is a list of possible root mount points across different devices.  Standardize
    //  on Pixel emulators which all use sdcard.  This way we can run different Android versions
    //  on different Pixel models without change or extra software
    val storageRoots = arrayOf(
            "sdcard",  // Android Studio Emulators for Pixel
            "storage/removable_SD Card",
            "storage",
            "mnt/SDCARD")

    var storage = storageRoots[0] // Standard root access point for Pixel Emulators
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
        const val reviewAdjust = "Review + Adjust"
    }

}

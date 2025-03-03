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

    const val durationToWaitWhenSwipingBetweenSlides: Long = 400 //Swipe.FAST = 100 * 2
    // 09/17/2021 - DKH: Update for Testing Abstraction #566
    // numberOfTimesToSwipeWhenApprovingAllSlides is very story template specific,
    //  eg, Lost Coin only has 6 slides.  Other stories with different slide counts will not work
    const val numberOfTimesToSwipeWhenApprovingAllSlides: Int = 5

    // 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
    // For Android 10 scoped storage, these were updated to relative path names from hard coded
    // path names.  Apps cannot directly access hard coded path names in the scoped storage
    // paradigm.  The user must okay the app's access to any directory in external storage.
    // Scoped storage is backward compatible with Android 9 and lower
    const val workspaceDirectory = "SPWorkspace"        // compatible with scoped storage
    const val exportedVideosDirectory = "videos"        // compatible with scoped storage
    const val dirNameForUserUpdatesToStory = "project"    // compatible with scoped storage

    val resources: Resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    object Phase {
        const val learn = "Learn"
        const val translate = "Record"
        const val communityWork = "Community"
        const val accuracyCheck = "Accuracy"
        const val voiceStudio = "Drama"
        const val finalize = "Create"
        const val share = "Share"
        const val reviewAdjust = "Review + Adjust"  // Unused?
    }

}

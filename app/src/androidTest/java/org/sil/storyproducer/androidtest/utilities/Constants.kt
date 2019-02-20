package org.sil.storyproducer.androidtest.utilities
object Constants {
    // The duration for which a clip  gets played should be shorter than the
    // corresponding duration for which a clip gets recorded.
    // Durations are in milliseconds
    const val durationToPlayNarration: Long = 100
    const val durationToPlayTranslatedClip: Long = 100
    const val durationToRecordLearnClip: Long = 1000
    const val durationToRecordTranslatedClip: Long = durationToRecordLearnClip
    const val durationToRecordFeedbackClip: Long = 250
    const val durationToRecordVoiceStudioClip: Long = 250

    const val durationToWaitWhenSwipingBetweenSlides: Long = 50

    object Phase {
        const val learn = "Learn"
        const val translate = "Translate"
        const val communityWork = "Community Work"
        const val accuracyCheck = "Accuracy Check"
        const val voiceStudio = "Voice Studio"
        const val finalize = "Finalize"
    }
}


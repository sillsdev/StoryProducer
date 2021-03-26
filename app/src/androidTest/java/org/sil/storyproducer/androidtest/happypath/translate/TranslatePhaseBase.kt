package org.sil.storyproducer.androidtest.happypath.translate

import org.junit.*
import org.sil.storyproducer.androidtest.happypath.PlayerPhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

class TranslatePhaseBase(sharedBase: SharedBase) : PlayerPhaseTestBase(sharedBase) {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.translate, base)
    }

    fun should_BeAbleToSwipeBetweenSlides() {
        test_swipingBetweenSlides()
    }

    fun should_beAbleToSwipeToNextPhase() {
        test_swipingToNextPhase(Constants.Phase.communityWork)
    }

    fun should_BeAbleToPlayNarrationOfASlide() {
        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected playback progress to increase with time.", endingProgress > originalProgress)
    }


    fun should_BeAbleToRecordTranslationForASlide() {
        // The "pulsing" animation on the recording toolbar causes the
        // Espresso click to hang, so we disable it for the test.
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            giveAppTimeToRecordAudio()
            pressMicButton()
        }
    }

}

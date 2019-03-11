package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.*
import org.junit.runner.RunWith
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.junit.*
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@LargeTest
@RunWith(AndroidJUnit4::class)
class TranslatePhaseTest : SwipablePhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.translate)
    }

    @Test
    fun should_BeAbleToSwipeBetweenSlides() {
        testSwipingBetweenSlides()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        testSwipingToNextPhase(Constants.Phase.communityWork)
    }

    @Test
    fun should_BeAbleToPlayNarrationOfASlide() {
        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected playback progress to increase with time.", endingProgress > originalProgress)
    }

    @Test
    fun should_BeAbleToRecordTranslationForASlide() {
        // The "pulsing" animation on the recording toolbar causes the
        // Espresso click to hang, so we disable it for the test.
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            giveAppTimeToRecordAudio()
            pressMicButton()
        }
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToRecordAudio() {
        Thread.sleep(Constants.durationToRecordTranslatedClip)
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressPlayPauseButton() {
        onView(allOf(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(Constants.durationToPlayTranslatedClip)
    }
}

package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.*
import org.junit.runner.RunWith
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.runners.MethodSorters
import java.lang.Integer.parseInt
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.junit.*
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator


@LargeTest
@RunWith(AndroidJUnit4::class)
class TranslatePhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToTranslatePhase()
    }

    @Test
    fun should_BeAbleToSwipeBetweenSlides() {
        val originalSlideNumber = findCurrentSlideNumber()
        var nextSlideNumber = originalSlideNumber + 1
        expectToBeOnSlide(originalSlideNumber)
        swipeLeftOnSlide()
        giveUiTimeToChangeSlides()
        expectToBeOnSlide(nextSlideNumber)
        swipeRightOnSlide()
        giveUiTimeToChangeSlides()
        expectToBeOnSlide(originalSlideNumber)
    }

    @Test
    fun should_BeAbleToPlayNarrationOfASlide() {
        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertNotEquals(endingProgress, originalProgress)
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

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        onView(allOf(withId(R.id.slide_number_text), withText(originalSlideNumber.toString()))).check(matches(isDisplayed()))
    }

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return parseInt(slideNumberTextView!!.text.toString())
    }

    private fun swipeRightOnSlide() {
        onView(allOf(withId(R.id.phase_frame))).perform(swipeRight())
    }

    private fun swipeLeftOnSlide() {
        onView(allOf(withId(R.id.phase_frame))).perform(swipeLeft())
    }

    private fun giveUiTimeToChangeSlides() {
        Thread.sleep(Constants.durationToWaitWhenSwipingBetweenSlides)
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

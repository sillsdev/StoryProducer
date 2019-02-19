package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.view.View.INVISIBLE
import android.widget.ImageButton
import android.widget.ListView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert
import org.junit.Test
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

class CommunityWorkPhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToCommunityWorkPhase()
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
    fun should_BeAbleToPlayTranslationOfASlide() {
        makeSureAnAudioClipIsAvailable()

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        pressPlayPauseButton()
        val progressAfterPausing = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", progressAfterPausing > originalProgress)
    }

    @Test
    fun should_BeAbleToRecordFeedback() {
        var originalNumberOfRecordings = getCurrentNumberOfRecordings()

        AnimationsToggler.disableCustomAnimations()
        pressMicButton()
        Thread.sleep(Constants.durationToRecordFeedbackClip)
        pressMicButton()
        AnimationsToggler.disableCustomAnimations()

        var finalNumberOfRecordings = getCurrentNumberOfRecordings()
        Assert.assertEquals("Expected an additional feedback recording to exist", originalNumberOfRecordings + 1, finalNumberOfRecordings)
    }

    @Test
    fun should_BeAbleToSwipeToNextPhase() {
        swipeUpOnSlide()
        expectToBeOnAccuracyCheckPhase()
    }

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return Integer.parseInt(slideNumberTextView!!.text.toString())
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.slide_number_text), ViewMatchers.withText(originalSlideNumber.toString()))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun swipeRightOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(swipeRight())
    }

    private fun swipeLeftOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(swipeLeft())
    }

    private fun swipeUpOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(swipeUp())
    }

    private fun makeSureAnAudioClipIsAvailable() {
        selectPhase(Constants.Phase.translate)
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
        selectPhase(Constants.Phase.communityWork)
    }

    private fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(org.sil.storyproducer.R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != INVISIBLE
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.disableCustomAnimations()
        pressMicButton()
        Thread.sleep(Constants.durationToRecordTranslatedClip)
        pressMicButton()
        AnimationsToggler.enableCustomAnimations()
    }

    private fun getCurrentNumberOfRecordings() =
            ActivityAccessor.getCurrentActivity()!!.findViewById<ListView>(R.id.recordings_list)!!.childCount

    private fun giveUiTimeToChangeSlides() {
        Thread.sleep(Constants.durationToWaitWhenSwipingBetweenSlides)
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun expectToBeOnAccuracyCheckPhase() {
        Espresso.onView(withText(Constants.Phase.accuracyCheck)).check(matches(isDisplayed()))
    }
}
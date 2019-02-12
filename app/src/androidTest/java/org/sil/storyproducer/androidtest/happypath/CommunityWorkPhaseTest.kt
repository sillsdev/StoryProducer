package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.widget.ListView
import androidx.test.espresso.Espresso
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
    fun should_BeAbleToPlayNarrationOfASlide() {
        // TODO: Make this test useful by copying a known-length audio clip to directory.
        // TODO: Also, ensure that we are actually on the slide that has the audio clip.
        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", endingProgress > originalProgress)
    }

    @Test
    fun should_BeAbleToRecordFeedback() {
        // Get number of recordings (if any)
        var originalNumberOfRecordings = ActivityAccessor.getCurrentActivity()!!.findViewById<ListView>(org.sil.storyproducer.R.id.recordings_list)!!.childCount
        // Record a feedback clip
        Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
        Thread.sleep(250)
        Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
        // Get number of recordings
        var finalNumberOfRecordings = ActivityAccessor.getCurrentActivity()!!.findViewById<ListView>(org.sil.storyproducer.R.id.recordings_list)!!.childCount
        // expect it to be n + 1
        Assert.assertEquals(originalNumberOfRecordings + 1, finalNumberOfRecordings)
    }

    @Test
    fun should_BeAbleToSwipeToNextPhase() {
        swipeUpOnSlide()
        expectToBeOnAccuracyCheckPhase()
    }

    private fun expectToBeOnAccuracyCheckPhase() {
        Espresso.onView(withText("Accuracy Check")).check(matches(isDisplayed()))
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.slide_number_text), ViewMatchers.withText(originalSlideNumber.toString()))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return Integer.parseInt(slideNumberTextView!!.text.toString())
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

    private fun giveUiTimeToChangeSlides() {
        Thread.sleep(50)
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(2000)
    }
}
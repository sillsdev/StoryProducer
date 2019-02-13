package org.sil.storyproducer.androidtest.happypath

import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.view.View.INVISIBLE
import android.widget.ImageButton
import android.widget.ImageView
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
        selectPhase("Translate")
        if (!areThereAnyRecordings()) {
            prepareForTestByCreatingATranslationClip()
        }
        selectPhase("Community Work")

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        val endingProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Assert.assertTrue("Expected progress bar to increase in position.", endingProgress > originalProgress)
    }

    private fun areThereAnyRecordings(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != INVISIBLE
    }

    private fun prepareForTestByCreatingATranslationClip() {
        disableCustomAnimations()
        pressMicButton()
        Thread.sleep(5000)
        pressMicButton()
        enableCustomAnimations()
    }

    private fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(org.sil.storyproducer.R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    private fun enableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(ActivityAccessor.getCurrentActivity()).edit()
        preferencesEditor.remove(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation))
    }

    private fun disableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(ActivityAccessor.getCurrentActivity()).edit()
        preferencesEditor.putBoolean(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), true)
        preferencesEditor.commit()
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
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
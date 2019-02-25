package org.sil.storyproducer.androidtest.happypath

import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.view.View
import android.widget.ImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import junit.framework.Assert.assertTrue
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccuracyCheckPhaseTest : PhaseTestBase() {
    val durationToRecordTranslatedClip: Long = 1000
    val durationToPlayTranslatedClip: Long = 100

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToAccuracyCheckPhase()
    }

    @Test
    fun shouldBeAbleToPlayRecordedAudioForSpecificSlide() {
        makeSureAnAudioClipIsAvailable()

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Thread.sleep(durationToPlayTranslatedClip)
        pressPlayPauseButton()
        val progressAfterPausing = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", progressAfterPausing > originalProgress)
    }

    @Test
    fun shouldBeAbleToToggleApprovedState() {
        Thread.sleep(1000)
        pressCheckmarkButton()
        Thread.sleep(1000)
        pressCheckmarkButton()
        Thread.sleep(1000)
        pressCheckmarkButton()
        Thread.sleep(1000)
        pressCheckmarkButton()
        Thread.sleep(1000)
        //TODO
    }

    @Test
    fun shouldBeAbleToSwipeBetweenSlides() {
        val originalSlideNumber = findCurrentSlideNumber()
        var nextSlideNumber = originalSlideNumber + 1
        expectToBeOnSlide(originalSlideNumber)
        // swipe to the left
        swipeLeftOnSlide()
        Thread.sleep(50)
        expectToBeOnSlide(nextSlideNumber)
        // swipe to the right
        swipeRightOnSlide()
        Thread.sleep(50)
        expectToBeOnSlide(originalSlideNumber)
    }

    @Test
    fun passwordConfirmationPopupShouldBehaveCorrectly() {
        //TODO
    }

    private fun makeSureAnAudioClipIsAvailable() {
        selectPhase("Translate")
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
        selectPhase("Accuracy Check")
    }

    private fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(org.sil.storyproducer.R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != View.INVISIBLE
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.disableCustomAnimations()
        pressMicButton()
        Thread.sleep(durationToRecordTranslatedClip)
        pressMicButton()
        AnimationsToggler.enableCustomAnimations()
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressCheckmarkButton() {
        onView(allOf(withId(R.id.concheck_checkmark_button), isDisplayed())).perform(click())
    }

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return Integer.parseInt(slideNumberTextView!!.text.toString())
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.slide_number_text), ViewMatchers.withText(originalSlideNumber.toString()))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun swipeLeftOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(swipeLeft())
    }

    private fun swipeRightOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(swipeRight())
    }

}

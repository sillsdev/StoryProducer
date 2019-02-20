package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.view.View
import android.widget.ImageButton
import android.widget.ListView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Test
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace

class VoiceStudioPhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToVoiceStudioPhase()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        swipeUpOnSlide()
        expectToBeOnPhase(Constants.Phase.finalize)
    }

    @Test
    fun should_beAbleToPlaySlideAudio() {
        makeSureAnAudioClipIsAvailable()
        approveSlides()
        val originalProgress = getCurrentSlideAudioProgress()

        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()

        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected ending progress to be greater than original progress.", endingProgress > originalProgress)
    }

    @Test
    fun should_beAbleToRecordSequentialAudioSnippetsAsOneClip() {
        AnimationsToggler.withoutCustomAnimations {
            // record a snippet with the mic button
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            // pause the recording
            Thread.sleep(Constants.durationToRecordVoiceStudioClip)
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            // Check the number of clips present in the list. n
            Espresso.onView(allOf(withId(R.id.list_recordings_button), isDisplayed())).perform(click())
            val numberOfClipsAfterRecordingFirstSnippet = getCurrentNumberOfClips()
            Espresso.onView(allOf(withId(R.id.exitButton), isDisplayed())).perform(click())

            // record another snippet to the clip
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            // pause the recording
            Thread.sleep(Constants.durationToRecordVoiceStudioClip)
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            // stop/finalize the attempt
            Espresso.onView(allOf(withId(R.id.finish_recording_button), isDisplayed())).perform(click())
            // check the number of recordings present in the list. Should be n
            Espresso.onView(allOf(withId(R.id.list_recordings_button), isDisplayed())).perform(click())
            val numberOfClipsAfterRecordingSecondSnippet = getCurrentNumberOfClips()
            Assert.assertEquals(numberOfClipsAfterRecordingFirstSnippet, numberOfClipsAfterRecordingSecondSnippet)
            Espresso.onView(allOf(withId(R.id.exitButton), isDisplayed())).perform(click())

            // record a clip with the mic button
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            Thread.sleep(Constants.durationToRecordVoiceStudioClip)
            // pause the recording
            Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
            // stop/finalize the attempt
            Espresso.onView(allOf(withId(R.id.finish_recording_button), isDisplayed())).perform(click())
            // check the number of recordings present in the list. Should n + 1
            Espresso.onView(allOf(withId(R.id.list_recordings_button), isDisplayed())).perform(click())
            val numberOfClipsAfterRecordingASnippetForANewClip = getCurrentNumberOfClips()
            Assert.assertEquals(numberOfClipsAfterRecordingSecondSnippet!! + 1, numberOfClipsAfterRecordingASnippetForANewClip)
            Espresso.onView(allOf(withId(R.id.exitButton), isDisplayed())).perform(click())
        }
    }

    private fun getCurrentNumberOfClips(): Int? {
        val numberOfClips = arrayOfNulls<Int>(1)
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.recordings_list), ViewMatchers.isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "Gets the currently displayed activity so that Espresso tests can reach 'under the hood' and reference actual Views."
            }

            override fun perform(uiController: UiController, view: View) {
                numberOfClips[0] = (view as ListView).childCount
            }
        })
        return numberOfClips[0]
    }

    @Test
    fun should_beAbleToSwipeBetweenSlides() {
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

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return Integer.parseInt(slideNumberTextView!!.text.toString())
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.slide_number_text), ViewMatchers.withText(originalSlideNumber.toString()))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun swipeRightOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(ViewActions.swipeRight())
    }

    private fun swipeLeftOnSlide() {
        Espresso.onView(allOf(ViewMatchers.withId(R.id.phase_frame))).perform(ViewActions.swipeLeft())
    }

    private fun giveUiTimeToChangeSlides() {
        Thread.sleep(Constants.durationToWaitWhenSwipingBetweenSlides)
    }

    private fun makeSureAnAudioClipIsAvailable() {
        selectPhase(Constants.Phase.translate)
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
        selectPhase(Constants.Phase.voiceStudio)
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != View.INVISIBLE
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.disableCustomAnimations()
        pressMicButton()
        Thread.sleep(Constants.durationToRecordTranslatedClip)
        pressMicButton()
        AnimationsToggler.enableCustomAnimations()
    }

    private fun pressMicButton() {
        Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(Constants.durationToPlayTranslatedClip)
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(allOf(withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun approveSlides() {
        selectPhase(Constants.Phase.accuracyCheck)
        for (item in Workspace.activeStory.slides) {
            item.isChecked = true;
        }
        Workspace.activeStory.isApproved = true
        selectPhase(Constants.Phase.voiceStudio)
    }

    private fun selectPhase(phaseTitle: String) {
        Espresso.onView(withId(R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    private fun swipeUpOnSlide() {
        Espresso.onView(ViewMatchers.withId(R.id.phase_frame)).perform(swipeUp())
    }

    private fun expectToBeOnPhase(phase: String) {
        Espresso.onView(allOf(ViewMatchers.withText(phase))).check(matches(isDisplayed()))
    }
}
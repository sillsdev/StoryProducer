package org.sil.storyproducer.androidtest.happypath.voice_studio

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.PlayerPhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

class VoiceStudioPhaseBase(sharedBase: SharedBase) : PlayerPhaseTestBase(sharedBase) {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.voiceStudio, base)
    }

    fun should_beAbleToSwipeToNextPhase() {
        test_swipingToNextPhase(Constants.Phase.finalize)
    }

    fun should_beAbleToSwipeToNextPhaseForVideo() {
        test_swipingToNextPhase(Constants.Phase.reviewAdjust)
    }

    fun should_beAbleToPlaySlideAudio() {
        // Arrange
        makeSureAnAudioClipIsAvailable(Constants.Phase.voiceStudio)
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.voiceStudio)
        val originalProgress = getCurrentSlideAudioProgress()

        // Act
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()

        // Assert
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected ending progress to be greater than original progress.", endingProgress > originalProgress)
    }

    fun should_beAbleToSwipeBetweenSlides() {
        test_swipingBetweenSlides()
    }

    fun should_beAbleToRecordSequentialAudioSnippetsAsOneClip() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.voiceStudio)

        verifyThatRecordingMultipleSnippetsDoesNotCreateMultipleClips()

        openRecordingsListDialog()
        val countOfClipsAfterRecordingSecondSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()

        verifyThatFinalizingOneClipThenRecordingANewSnippetCreatesANewClip(countOfClipsAfterRecordingSecondSnippet)
    }

    private fun verifyThatRecordingMultipleSnippetsDoesNotCreateMultipleClips(): Int? {
        // record a snippet of audio to the clip
        recordAVoiceStudioTranslationSnippet()
        openRecordingsListDialog()
        val countOfClipsAfterRecordingFirstSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()
        // record another snippet to the clip
        recordAVoiceStudioTranslationSnippet()
        // 'finalize' the clip, which should prevent additional snippets from being appended to it.
        clickStopClipButton()
        openRecordingsListDialog()
        val countOfClipsAfterRecordingSecondSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()

        Assert.assertEquals(countOfClipsAfterRecordingFirstSnippet, countOfClipsAfterRecordingSecondSnippet)
        return countOfClipsAfterRecordingSecondSnippet
    }

    private fun verifyThatFinalizingOneClipThenRecordingANewSnippetCreatesANewClip(countOfClipsAfterRecordingSecondSnippet: Int?) {
        // record a snippet of audio to the clip
        recordAVoiceStudioTranslationSnippet()
        // 'finalize' the clip, which should prevent additional snippets from being appended to it.
        clickStopClipButton()
        openRecordingsListDialog()
        val numberOfClipsAfterRecordingASnippetForANewClip = getCurrentCountOfClips()
        closeRecordingsListDialog()

        Assert.assertEquals(countOfClipsAfterRecordingSecondSnippet!! + 1, numberOfClipsAfterRecordingASnippetForANewClip)
    }

    private fun clickStopClipButton() {
        Espresso.onView(allOf(withId(R.id.finish_recording_button), isDisplayed())).perform(click())
    }

    private fun openRecordingsListDialog() {
        Espresso.onView(allOf(withId(R.id.list_recordings_button), isDisplayed())).perform(click())
    }

    private fun closeRecordingsListDialog() {
        Espresso.onView(allOf(withId(R.id.exitButton), isDisplayed())).perform(click())
    }

    private fun getCurrentCountOfClips(): Int? {
        val numberOfClips = arrayOfNulls<Int>(1)
        Espresso.onView(allOf(withId(R.id.recordings_list), isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "Gets the currently displayed activity so that Espresso tests can reach 'under the hood' and reference actual Views."
            }

            override fun perform(uiController: UiController, view: View) {
                numberOfClips[0] = (view as androidx.recyclerview.widget.RecyclerView).childCount
            }
        })
        return numberOfClips[0]
    }

    private fun recordAVoiceStudioTranslationSnippet() {
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            Thread.sleep(Constants.durationToRecordVoiceStudioClip)
            pressMicButton()
        }
    }

}
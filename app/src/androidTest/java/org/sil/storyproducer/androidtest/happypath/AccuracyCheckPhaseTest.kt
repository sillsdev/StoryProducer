package org.sil.storyproducer.androidtest.happypath

import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.DialogTitle
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
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccuracyCheckPhaseTest : SwipablePhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.accuracyCheck)
    }

    @Test
    fun shouldBeAbleToSwipeBetweenSlides() {
        testSwipingBetweenSlides()
    }

    @Test
    fun shouldBeAbleToPlayRecordedAudioForSpecificSlide() {
        makeSureAnAudioClipIsAvailable(Constants.Phase.accuracyCheck)

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        pressPlayPauseButton()
        val progressAfterPausing = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", progressAfterPausing > originalProgress)
    }

    @Test
    fun shouldBeAbleToToggleApprovedState() {
        val currentSlideNum = findCurrentSlideNumber();
        val originalApprovalState = Workspace.activeStory.slides[currentSlideNum].isChecked

        pressCheckmarkButton()

        val approvalStateAfterClickingCheckmark = Workspace.activeStory.slides[currentSlideNum].isChecked
        Assert.assertNotEquals(originalApprovalState, approvalStateAfterClickingCheckmark)

        pressCheckmarkButton()

        val approvalStateAfterSecondClickOnCheckmark = Workspace.activeStory.slides[currentSlideNum].isChecked
        Assert.assertEquals(originalApprovalState, approvalStateAfterSecondClickOnCheckmark)
    }

    @Test
    fun passwordConfirmationPopupShouldBehaveCorrectly() {
        swipeThroughAndApproveAllSlides()
        typePasswordAndClickSubmit()
        shouldNowBeOnVoiceStudioPhase()
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

    private fun swipeThroughAndApproveAllSlides() {
        for (i in 1..Constants.numberOfTimesToSwipeWhenApprovingAllSlides) {
            pressCheckmarkButton()
            swipeLeftOnSlide()
        }
        pressCheckmarkButton()
    }

    private fun typePasswordAndClickSubmit() {
        Thread.sleep(200)
        onView(allOf(withId(R.id.password_text_field), isDisplayed())).perform(clearText()).perform(typeText("appr00ved"))
        onView(withText("SUBMIT")).perform(click())
        Thread.sleep(1000)
    }

    private fun shouldNowBeOnVoiceStudioPhase() {
        onView(withText(containsString("Voice Studio"))).check(ViewAssertions.matches(isDisplayed()))
    }

}

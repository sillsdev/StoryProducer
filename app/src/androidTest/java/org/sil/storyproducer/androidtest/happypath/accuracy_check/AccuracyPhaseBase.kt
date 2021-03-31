package org.sil.storyproducer.androidtest.happypath.accuracy_check

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.*
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.PlayerPhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.*
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace

class AccuracyPhaseBase(sharedBase: SharedBase) : PlayerPhaseTestBase(sharedBase) {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.accuracyCheck, base)
    }

    fun shouldBeAbleToSwipeBetweenSlides() {
        test_swipingBetweenSlides()
    }

    fun shouldBeAbleToPlayRecordedAudioForSpecificSlide() {
        makeSureAnAudioClipIsAvailable(Constants.Phase.accuracyCheck)

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        pressPlayPauseButton()
        val progressAfterPausing = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", progressAfterPausing > originalProgress)
    }

    fun shouldBeAbleToToggleApprovedState() {
        val currentSlideNum = findCurrentSlideNumber()
        val originalApprovalState = Workspace.activeStory.slides[currentSlideNum].isChecked

        pressCheckmarkButton()

        val approvalStateAfterClickingCheckmark = Workspace.activeStory.slides[currentSlideNum].isChecked
        Assert.assertNotEquals(originalApprovalState, approvalStateAfterClickingCheckmark)

        pressCheckmarkButton()

        val approvalStateAfterSecondClickOnCheckmark = Workspace.activeStory.slides[currentSlideNum].isChecked
        Assert.assertEquals(originalApprovalState, approvalStateAfterSecondClickOnCheckmark)
    }

    fun passwordConfirmationPopupShouldBehaveCorrectly() {
        swipeThroughAndApproveAllSlides()
        typePasswordAndClickSubmit()
        shouldNowBeOnVoiceStudioPhase()
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

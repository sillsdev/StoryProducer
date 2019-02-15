package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

class VoiceStudioPhaseTest : PhaseTestBase() {
    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToVoiceStudioPhase()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        swipeUpOnSlide()
        expectToBeOnPhase("Finalize")
    }

    private fun swipeUpOnSlide() {
        Espresso.onView(ViewMatchers.withId(R.id.phase_frame)).perform(swipeUp())
    }

    private fun expectToBeOnPhase(phase: String) {
        Espresso.onView(allOf(ViewMatchers.withText(phase))).check(matches(isDisplayed()))
    }
}
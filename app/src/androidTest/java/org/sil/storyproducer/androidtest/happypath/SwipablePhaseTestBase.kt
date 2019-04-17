package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatTextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.Constants

open abstract class SwipablePhaseTestBase : PhaseTestBase() {
    protected fun testSwipingBetweenSlides() {
        val originalSlideNumber = findCurrentSlideNumber()
        var nextSlideNumber = originalSlideNumber + 1
        expectToBeOnSlide(originalSlideNumber)
        swipeLeftOnSlide()
        expectToBeOnSlide(nextSlideNumber)
        swipeRightOnSlide()
        expectToBeOnSlide(originalSlideNumber)
    }

    protected fun testSwipingToNextPhase(nameOfNextPhase: String) {
        swipeUpOnSlide()
        expectToBeOnPhase(nameOfNextPhase)
    }

    protected fun swipeRightOnSlide() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.phase_frame))).perform(ViewActions.swipeRight())
        Thread.sleep(Constants.durationToWaitWhenSwipingBetweenSlides)
    }

    protected fun swipeLeftOnSlide() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.phase_frame))).perform(ViewActions.swipeLeft())
        Thread.sleep(Constants.durationToWaitWhenSwipingBetweenSlides)
    }

    private fun swipeUpOnSlide() {
        Espresso.onView(ViewMatchers.withId(R.id.phase_frame)).perform(ViewActions.swipeUp())
    }

    protected fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return Integer.parseInt(slideNumberTextView!!.text.toString())
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.slide_number_text), ViewMatchers.withText(originalSlideNumber.toString()))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun expectToBeOnPhase(phase: String) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withText(phase))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
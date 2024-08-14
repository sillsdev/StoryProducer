package org.sil.storyproducer.androidtest.happypath

import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.Constants

abstract class SwipablePhaseTestBase(sharedBase: SharedBase) : PhaseTestBase() {

    protected val base = sharedBase

    protected fun test_swipingBetweenSlides() {
        val originalSlideNumber = findCurrentSlideNumber()
        val nextSlideNumber = originalSlideNumber + 1
        expectToBeOnSlide(originalSlideNumber)
        swipeLeftOnSlide()
        expectToBeOnSlide(nextSlideNumber)
        swipeRightOnSlide()
        expectToBeOnSlide(originalSlideNumber)
    }

    protected fun test_swipingToNextPhase(nameOfNextPhase: String) {
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
        val slideNumberTextView = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatTextView>(base.getSlideNumberId())
        if (slideNumberTextView?.text == "Title") return 0
        return Integer.parseInt(slideNumberTextView!!.text.split(" ")[1])
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(base.getSlideNumberId()), ViewMatchers.withText(
            if (originalSlideNumber == 0) "Title" else "Slide $originalSlideNumber"
        ))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun expectToBeOnPhase(phase: String) {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withText(phase))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}

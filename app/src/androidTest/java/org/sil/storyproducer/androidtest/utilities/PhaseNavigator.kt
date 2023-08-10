package org.sil.storyproducer.androidtest.utilities

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.R
import java.lang.RuntimeException

object PhaseNavigator {
    fun selectPhase(phaseTitle: String) {
        Thread.sleep(500)
        onView(ViewMatchers.withId(R.id.toolbar)).perform(ViewActions.click())
        Thread.sleep(500)
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
        Thread.sleep(500)
    }

    fun navigateFromRegistrationScreenToPhase(phaseName: String, sharedBase: SharedBase) {
        if(isRegistrationDisplayed()) {
            skipRegistration()
        }

        clickOnStory(sharedBase.getStoryName())
        selectPhase(phaseName)
    }

    fun doInPhase(inPhase: String, function: () -> Unit, returnPhase: String) {
        selectPhase(inPhase)
        function()
        selectPhase(returnPhase)
    }

    fun skipRegistration() {
        onView(withText(R.string.bypass_button_text)).perform(ViewActions.click())
        onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    private fun clickOnStory(storyName: String) {
        // 09/11/2021 - DKH: Update for Testing Abstraction #566
        // ALL_STORIES, IN_PROGRESS, COMPLETED are the tabs in the "Story Templates" view
        //  that contain the story titles.
        // The same exact title can appear in two places, eg, once in
        //     ALL_STORIES tab and once in the IN_PROGRESS tab.  When running the Espresso test,
        //     the Espresso title picker throws an exception because it cannot
        //     differentiate between the same title in the ALL_STORIES tab and the
        //     IN_PROGRESS tab.
        // To provide more uniqueness, add one space to each title in the IN_PROGRESS tab, two
        //     spaces to each title in the COMPLETED tab and three spaces to each title in
        //     any other tabbed ENUMS.
        //     The users will not see a difference but the Espresso software will be
        //     able to pick the appropriate story/tab combo.
        // So we do exact match (equalTo) instead of just containing part of the string (containsString)
        onView(withText(CoreMatchers.equalTo(storyName))).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    private fun isRegistrationDisplayed() : Boolean {
        return onView(withText(R.string.bypass_button_text)).isDisplayed()
    }

    fun ViewInteraction.isDisplayed(): Boolean {
        return try {
            check(matches(ViewMatchers.isDisplayed()))
            true
        } catch (e: RuntimeException) {
            false
        }
    }
    fun ViewInteraction.isNotDisplayed() : Boolean {
        return !this.isDisplayed()
    }
}

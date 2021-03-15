package org.sil.storyproducer.androidtest.utilities

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.film.R
import java.lang.RuntimeException

object PhaseNavigator {
    fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
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
        Espresso.onView(ViewMatchers.withText(R.string.bypass_button_text)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    private fun clickOnStory(storyName: String) {
        Espresso.onView(ViewMatchers.withText(CoreMatchers.containsString(storyName))).perform(ViewActions.scrollTo(), ViewActions.click())
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
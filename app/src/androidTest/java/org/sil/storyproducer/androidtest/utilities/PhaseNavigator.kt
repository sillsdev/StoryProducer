package org.sil.storyproducer.androidtest.utilities

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.film.R

object PhaseNavigator {
    fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    fun navigateFromRegistrationScreenToPhase(phaseName: String, sharedBase: SharedBase) {
        skipRegistration()

        // Wait for the dialog to close
//        onView(withText(R.string.scanning_sp_templates)).check(doesNotExist());
        Thread.sleep(500)

        clickOnStory(sharedBase.getStoryName())
        selectPhase(phaseName)
    }

    fun doInPhase(inPhase: String, function: () -> Unit, returnPhase: String) {
        selectPhase(inPhase)
        function()
        selectPhase(returnPhase)
    }

    fun skipRegistration() {
        Espresso.onView(ViewMatchers.withText("Skip Registration")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    private fun clickOnStory(storyName: String) {
        Espresso.onView(ViewMatchers.withText(CoreMatchers.containsString(storyName))).perform(ViewActions.scrollTo(), ViewActions.click())
    }
}
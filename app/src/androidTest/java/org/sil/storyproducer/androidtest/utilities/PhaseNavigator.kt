package org.sil.storyproducer.androidtest.utilities

import android.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers

object PhaseNavigator {
    fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(org.sil.storyproducer.R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }

    fun navigateFromRegistrationScreenToPhase(phaseName: String) {
        skipRegistration()
        clickOnStory(Constants.nameOfTestStory)
        selectPhase(phaseName)
    }

    fun doInPhase(inPhase: String, function: () -> Unit, returnPhase: String) {
        selectPhase(inPhase)
        function()
        selectPhase(returnPhase)
    }

    private fun skipRegistration() {
        Espresso.onView(ViewMatchers.withText("Skip Registration")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.button1)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    private fun clickOnStory(storyName: String) {
        Espresso.onView(ViewMatchers.withText(CoreMatchers.containsString(storyName))).perform(ViewActions.scrollTo(), ViewActions.click())
    }
}
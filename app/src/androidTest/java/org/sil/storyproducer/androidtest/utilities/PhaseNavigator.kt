package org.sil.storyproducer.androidtest.utilities

import android.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers

object PhaseNavigator {
    fun navigateFromRegistrationScreenToTranslatePhase() {
        skipRegistration()
        Espresso.onView(ViewMatchers.withText(CoreMatchers.containsString("Lost Coin"))).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(org.sil.storyproducer.R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`("Translate"))).perform(ViewActions.click())
    }

    private fun skipRegistration() {
        Espresso.onView(ViewMatchers.withText("Skip Registration")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.button1)).perform(ViewActions.scrollTo(), ViewActions.click())
    }
}
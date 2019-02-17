package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RegistrationActivityTest : PhaseTestBase() {
    override fun navigateToPhase() {
        // This function is intentionally empty, since
        // this test verifies the registration screen, which
        // appears prior to any phase.
    }

    @Test
    fun should_beAbleToSkipRegistration() {
        onView(withText("Skip Registration")).perform(click())
        onView(withId(android.R.id.button1)).perform(scrollTo(), click())
        onView(withText(containsString("Lost Coin"))).check(matches(isDisplayed()))
    }

}

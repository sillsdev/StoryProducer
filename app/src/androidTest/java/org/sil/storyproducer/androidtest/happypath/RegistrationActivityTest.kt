package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.happypath.base.PhotoBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.happypath.base.annotation.PhotoTest
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@PhotoTest
@RunWith(AndroidJUnit4::class)
class RegistrationActivityTest() : PhaseTestBase() {
    override fun navigateToPhase() {
        // This function is intentionally empty, since
        // this test verifies the registration screen, which
        // appears prior to any phase.
    }

    val base : SharedBase = PhotoBase()

    @Before
    fun setup() {
        revertWorkspaceToCleanState(base)
    }

    @Test
    fun should_beAbleToSkipRegistration() {
        PhaseNavigator.skipRegistration()
        onView(withText(containsString(base.getStoryName()))).check(matches(isDisplayed()))
    }

}

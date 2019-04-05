package org.sil.storyproducer.androidtest.example

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.controller.RegistrationActivity

@RunWith(AndroidJUnit4::class)
@Ignore // Since this is an example test, it should not run automatically.
class HelloWorldEspressoTest {

    @Rule
    @JvmField
    val activityRule = ActivityTestRule(RegistrationActivity::class.java)

    @Test
    fun registrationActivity_should_showSkipRegistrationButton() {
        onView(withText("Skip Registration")).check(matches(isDisplayed()))
    }
}
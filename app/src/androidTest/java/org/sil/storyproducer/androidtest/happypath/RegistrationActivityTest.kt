package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.utilities.IntentMocker.setUpDummyWorkspacePickerIntent
import org.sil.storyproducer.androidtest.utilities.IntentMocker.tearDownDummyWorkspacePickerIntent
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.controller.RegistrationActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class RegistrationActivityTest {

    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    @Test
    fun should_beAbleToSkipRegistration() {
        setUpDummyWorkspacePickerIntent()
        mActivityTestRule.launchActivity(null)
        tearDownDummyWorkspacePickerIntent()

        onView(withText("Skip Registration")).perform(click())
        onView(withId(android.R.id.button1)).perform(scrollTo(), click())
        onView(withText(containsString("Lost Coin"))).check(matches(isDisplayed()))
    }

}

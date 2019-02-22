package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.IntentMocker
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.controller.RegistrationActivity

open abstract class PhaseTestBase {
    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    @Before
    fun setUp() {
        launchActivityAndBypassWorkspacePicker()
        navigateToPhase()
    }

    open abstract fun navigateToPhase()

    private fun launchActivityAndBypassWorkspacePicker() {
        IntentMocker.setUpDummyWorkspacePickerIntent()
        mActivityTestRule.launchActivity(null)
        IntentMocker.tearDownDummyWorkspacePickerIntent()
    }

    protected fun selectPhase(phaseTitle: String) {
        Espresso.onView(ViewMatchers.withId(R.id.toolbar)).perform(ViewActions.click())
        Espresso.onData(CoreMatchers.allOf(CoreMatchers.`is`(CoreMatchers.instanceOf(String::class.java)), CoreMatchers.`is`(phaseTitle))).perform(ViewActions.click())
    }
}
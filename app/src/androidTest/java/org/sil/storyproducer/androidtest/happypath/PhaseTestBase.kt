package org.sil.storyproducer.androidtest.happypath

import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
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

}
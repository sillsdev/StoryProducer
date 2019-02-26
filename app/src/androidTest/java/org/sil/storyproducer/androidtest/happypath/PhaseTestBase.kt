package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.IntentMocker
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.controller.RegistrationActivity
import java.io.File

open abstract class PhaseTestBase {
    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    companion object {
        @JvmStatic
        @BeforeClass
        fun copyFreshTestStoryToWorkspace() {
            try {
                val source = File(concatenateSourcePath())
                val destination = File(concatenateDestinationPath())
                if (destination.exists()) {
                    destination.deleteRecursively()
                }
                source.copyRecursively(destination, true)
            } catch (e: Exception){
                Assert.fail("Failed to copy pristine story template from test resources folder to workspace folder.")
            }
        }

        private fun concatenateSourcePath(): String {
            return Constants.espressoResourceDirectory + File.separator + Constants.nameOfTestStoryDirectory
        }

        private fun concatenateDestinationPath(): String {
            return Constants.workspaceDirectory + File.separator + Constants.nameOfTestStoryDirectory
        }
    }

    @Before
    fun setUp() {
        launchActivityAndBypassWorkspacePicker()
        navigateToPhase()
    }

    abstract fun navigateToPhase()

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
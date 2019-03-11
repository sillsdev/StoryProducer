package org.sil.storyproducer.androidtest.happypath

import androidx.test.rule.GrantPermissionRule
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.IntentMocker
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.model.Workspace
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
        fun revertWorkspaceToCleanState() {
            copyFreshTestStoryToWorkspace()
            deleteExportedVideos()
        }

        private fun copyFreshTestStoryToWorkspace() {
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

        private fun deleteExportedVideos() {
            try {
                val exportedVideosDirectory = File(Constants.exportedVideosDirectory)
                if (exportedVideosDirectory.exists()) {
                    exportedVideosDirectory.deleteRecursively()
                }
            } catch (e: Exception) {
                Assert.fail("Failed to find or to delete video export directory.")
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

    protected fun approveSlides() {
        for (item in Workspace.activeStory.slides) {
            item.isChecked = true
        }
        Workspace.activeStory.isApproved = true
    }
}
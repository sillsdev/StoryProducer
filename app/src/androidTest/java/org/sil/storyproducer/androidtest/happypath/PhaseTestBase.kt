package org.sil.storyproducer.androidtest.happypath

import android.os.Build
import android.view.View
import android.widget.ImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers
import org.junit.*
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.*
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.model.Workspace
import java.io.File

abstract class PhaseTestBase {

    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    companion object {

//        @JvmStatic
        @BeforeClass
        fun revertWorkspaceToCleanState(sharedBase: SharedBase) {
            if(!Constants.workspaceIsInitialized) {
                WorkspaceSetter.setWorkspaceSoOtherTestsRunCorrectly()
                Constants.workspaceIsInitialized = true
            }
            checkSDCardType()
            copyFreshTestStoryToWorkspace(sharedBase)
            deleteExportedVideos()
        }

        private fun checkSDCardType() {
            for(s in Constants.storageRoots) {
                Constants.storage = s
                if(File(Constants.workspaceDirectory).exists())
                    return
            }
            //no available place works!
            Assert.fail("Cannot find the workspace directory: ${Constants.workspaceDirectory}")
        }

        private fun copyFreshTestStoryToWorkspace(sharedBase: SharedBase) {
            try {
                val source = File(concatenateSourcePath(sharedBase))
                val destination = File(concatenateDestinationPath(sharedBase))
                if (destination.exists()) {
                    destination.deleteRecursively()
                }

                // 09/19/2021 - DKH: Update for Testing Abstraction #566
                // Android 10 and greater does not allow File operations
                if(Build.VERSION.SDK_INT < 29) { // not valid operation for Android 10 or greater
                    // todo: Recode this for Android 10 scoped storage
                    source.copyRecursively(destination, true)
                }
            } catch (e: Exception){
                Assert.fail("Failed to copy pristine story template from test resources folder " +
                        "to workspace folder. Ensure that Story Producer has the 'Storage' " +
                        "permission.")
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

        private fun concatenateSourcePath(sharedBase: SharedBase): String {
//            return Constants.espressoResourceDirectory + File.separator + Constants.nameOfTestStoryDirectory
            return Constants.espressoResourceDirectory + File.separator + sharedBase.getStoryDirectory()

        }

        private fun concatenateDestinationPath(sharedBase: SharedBase): String {
//            return Constants.workspaceDirectory + File.separator + Constants.nameOfTestStoryDirectory
            return Constants.workspaceDirectory + File.separator + sharedBase.getStoryDirectory()
        }
    }

    @Before
    fun setUp() {
        launchActivityAndBypassWorkspacePicker()
        navigateToPhase()
    }

    abstract fun navigateToPhase()

    private fun launchActivityAndBypassWorkspacePicker() {
        mActivityTestRule.launchActivity(null)
    }

    protected fun approveSlides() {
        for (item in Workspace.activeStory.slides) {
            item.isChecked = true
        }
        Workspace.activeStory.isApproved = true
    }

    protected fun makeSureAnAudioClipIsAvailable(phaseToReturnTo: String) {
        PhaseNavigator.doInPhase(Constants.Phase.translate, {
            recordAnAudioTranslationClip()
        }, phaseToReturnTo)
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != View.INVISIBLE
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            Thread.sleep(Constants.durationToRecordTranslatedClip)
            pressMicButton()
        }
    }

    private fun pressMicButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.start_recording_button), ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }

}
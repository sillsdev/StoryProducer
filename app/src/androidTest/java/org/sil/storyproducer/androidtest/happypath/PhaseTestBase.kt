package org.sil.storyproducer.androidtest.happypath

import android.view.View
import android.widget.ImageButton
import androidx.core.view.GravityCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.*
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.model.Workspace

// 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
// This class was refactored to accommodate scoped storage introduced in Android 10
// For Android 10 and subsequent Android versions, an App (ie, Story Producer)
// has to ask the user for permission to access files in external storage (Espresso is the user).
// New special file classes are used to read/write/delete
// files and directories in external storage.
//
// To fit into this paradigm, during pretest we create a workspace directory with a Bloom
// file in it (eg, 002 Lost Coin.bloom). Espresso starts Story Producer and then Story Producer
// will ask Espresso (ie the user) where the workspace directory is located.
// Espresso does the user selections telling Story Producer where the workspace directory
// is located and it is okay for Story Producer to access the workspace directory.
// Then, Story Producer unpacks the
// Bloom file (eg, 002 Lost Coin.bloom) creating a story directory in the workspace directory.
// All of Story Producer's accesses use the new scoped storage classes for Android 10.
// Once  Story Producer has the permissions to access all files and directories in the
// workspace directory hierarchy, the Espresso routines can also read/write/delete
// any file/directory in the workspace directory hierarchy
// Scoped storage is backward compatible with Android 9 and lower
//
// This methodology allows the Espresso routines to conform to Android 10 scoped storage and also
// test the Story Producer's Bloom extraction code (this was not tested previously).

abstract class PhaseTestBase {

    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(MainActivity::class.java, false, false)

    @Rule
    @JvmField
    val mActivityRegistrationRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    companion object {

        //        @JvmStatic
        @BeforeClass
        fun revertWorkspaceToCleanState(sharedBase: SharedBase) {

            if(!Constants.workspaceIsInitialized) {
                WorkspaceSetter.setWorkspaceSoOtherTestsRunCorrectly()

                // deletes 'project' sub-folder with story.json project
                // so that a clean test story is loaded from the html file
                copyFreshTestStoryToWorkspace(sharedBase)

                // Selects the 'Add Demo to Story List' nav-drawer command
                // so that all stories are re-loaded to a fresh state
                selectAddDemoStory()

                // set initialized flag so that we only load one
                // fresh test story per unit test run.
                Constants.workspaceIsInitialized = true
            }

            // 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
            // checkSDCardType() - Deleted this for Android 10 scoped storage updates

            deleteExportedVideos()  // delete videos for each test group
        }

        // Selects the 'Add Demo to Story List' drawer command
        private fun selectAddDemoStory() {
            Thread.sleep(1000)
            // open the drawer menu
            Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open(GravityCompat.START))
            Thread.sleep(500)
            // click on add demo story to list command
            Espresso.onView(ViewMatchers.withSubstring(Constants.resources.getString(R.string.copy_demo))).perform(ViewActions.click())
            Thread.sleep(2000)  // give it time to reload all stories
        }

        private fun copyFreshTestStoryToWorkspace(sharedBase: SharedBase) {
            // 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
            // WSDoc is the Workspace directory for this test and is
            // stored in a Story Producer Object.
            val WSDoc = Workspace.workdocfile

            // The Workspace directory contains the story directory (eg: 002 Lost Coin) which was
            // created before the running of any Espresso test.
            // To create a fresh story (which is done before each test), delete any files
            // in the story directory that were created during the last test
            try {
                // find the story directory under test, eg, 002 Lost Coin
                WSDoc.findFile(sharedBase.getStoryDirectory())?.let {
                    // We found the story directory, find the directory which contains
                    // user generated data
                    it.findFile(Constants.dirNameForUserUpdatesToStory)?.let {
                        // We found the directory that would contain any data generated by the
                        // user in the last test, so delete the directory and all files
                        if(it.delete()==false){ // returns false if not deleted
                            Assert.fail("Failed to delete test data directory: " +
                                    Constants.dirNameForUserUpdatesToStory )
                        }
                    }
                    // Recreate the directory for user generated data
                    it.createDirectory(Constants.dirNameForUserUpdatesToStory)!!  // throw exception on directory create fail
                    return  // success, so return
                }
                // report we were unable to find the story directory
                Assert.fail("Failed to find story directory: " + sharedBase.getStoryDirectory())
            } catch (e: Exception) {
                Assert.fail("Failed to create directory: " +
                        sharedBase.getStoryDirectory() + "/" +
                        Constants.dirNameForUserUpdatesToStory)
            }
        }

        private fun deleteExportedVideos() {

                // 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
                // WSDoc is the Workspace directory for this test and is stored in a Story Producer Object.
                val WSDoc = Workspace.workdocfile

                // Find the directory that would contain any test generated videos
                WSDoc.findFile(Constants.exportedVideosDirectory)?.let {
                    // video directory exists delete it, along with any videos in the directory
                    if(it.delete()==false){ // returns fasle if not deleted
                        Assert.fail("Failed to delete video export directory.")
                    }
                }

                // The video directory may not exist, this is okay

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
        Thread.sleep(1000)
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

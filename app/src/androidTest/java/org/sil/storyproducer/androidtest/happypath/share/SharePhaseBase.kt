package org.sil.storyproducer.androidtest.happypath.share

import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.*
import org.junit.Assert
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.happypath.PhaseTestBase
import org.sil.storyproducer.androidtest.happypath.WorkspaceSetter
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace
import java.io.File


class SharePhaseBase(sharedBase: SharedBase) : PhaseTestBase() {

    val base = sharedBase

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.share, base)
    }

    fun when_thereAreNoExportedVideos_should_showNoVideosMessage() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.share)

        // Remove newly created video from the list
//        val deleteButton = onView(
//                Matchers.allOf(withId(R.id.file_delete_button),
//                        childAtPosition(withParent(withId(R.id.videos_list)), 3), isDisplayed()))
//        deleteButton.perform(click())
//
//        val confirmDelete = onView(Matchers.allOf(withId(android.R.id.button1), withText("Yes")))
//        confirmDelete.perform(ViewActions.scrollTo(), click())
//        Thread.sleep(500)

        onView(withText(R.string.no_videos)).check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

    fun when_aVideoHasBeenExported_should_showItInTheList() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.finalize)
        val videoFilename = base.getExportVideoName()
        copySampleVideoToExportDirectory(videoFilename)
        Workspace.activeStory.addVideo(videoFilename)
        PhaseNavigator.selectPhase(Constants.Phase.share)

        onView(withText(CoreMatchers.containsString(videoFilename))).check(matches(isDisplayed()))
    }

    private fun copySampleVideoToExportDirectory(videoFilename: String) {
        try {
            // 10/04/2021 - DKH: Espresso test fail for Android 10 and 11 #594
            // Update this function to use scoped storage that was introduced
            // in the Android 10 release. The functions that were used here are no longer
            // supported in Android 10 and subsequent releases.  Basically we move
            // from using the "File" class to using the "DocumentFile" class.
            // DocumentFile is supported in pre Android 10 releases.
            // The DocumentFile Class has to obtain permission from the user before any files
            // can be created by the Story Producer App (or Espresso).

            // The Workspace.workdocfile documentFile was created by Espresso menu picks
            // during startup and is the root directory for Story Producer and these test.
            // Espresso used the
            // workspaceDirectory variable string in Constants.kt (eg, /sdcard/SPWorkspace) and
            // simulated the user typing the in the string so that the Story Producer App would
            // know where to find the Story template.  We grab the documentFile (Workspace.workdocfile)
            // from Story Producer, which points to the selected workspace, and then do some setup
            // for the next test.
            val WSDoc = Workspace.workdocfile

            // For this test, we create a video file with no data.  That is all that is needed
            // to pass the test.

            // check to see if the video directory exists in the root directory
            // grab the directory name
            val videoDirName = Constants.exportedVideosDirectory.split('/').last()
            WSDoc.findFile(videoDirName)?.let {
                // video directory exists delete it, along with any videos in the directory
                it.delete()
            }

            // If we were able to create the video directory (ie, non null using ?),
            // then try and create the file
            WSDoc.createDirectory(videoDirName)?.let {
                // see if we can create the file
                it.createFile("",videoFilename)!!  // throw exception on file create fail
                return  // success, so return
            }

            // report we were unable to create the directory
            Assert.fail("Failed to create sample video directory for test.")

        } catch (e: Exception){
            // report we were unable to create the video file
            Assert.fail("Failed to create sample video to exported videos directory for test.")
        }
    }

}
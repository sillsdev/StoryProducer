package org.sil.storyproducer.androidtest.happypath.share

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.*
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.PhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.R
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
            val sampleVideo = File(Constants.espressoResourceDirectory + File.separator + videoFilename)
            val destination = File(Constants.exportedVideosDirectory + File.separator + videoFilename)
            // 09/19/2021 - DKH: Update for Testing Abstraction #566
            // Android 10 and greater does not allow File operations
            if(Build.VERSION.SDK_INT < 29) { // not valid operation for Android 10 or greater
                // todo: Recode this for Android 10 scoped storage
                File(Constants.exportedVideosDirectory).mkdirs()
                sampleVideo.copyRecursively(destination, true)
            }
        } catch (e: Exception){
            Assert.fail("Failed to copy sample video to exported videos directory for test.")
        }
    }

}
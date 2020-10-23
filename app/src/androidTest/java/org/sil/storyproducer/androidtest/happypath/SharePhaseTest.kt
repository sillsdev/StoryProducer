package org.sil.storyproducer.androidtest.happypath

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.Constants.durationToWaitWhenSwipingBetweenSlides
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace
import java.io.File


@LargeTest
@RunWith(AndroidJUnit4::class)
class SharePhaseTest : PhaseTestBase() {
    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.share)
    }

    @Test
    fun when_thereAreNoExportedVideos_should_showNoVideosMessage() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.share)

        onView(withText(org.sil.storyproducer.R.string.no_videos)).check(matches(isDisplayed()))
    }

    @Test
    fun when_aVideoHasBeenExported_should_showItInTheList() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.finalize)
        val videoFilename = Constants.nameOfSampleExportVideo
        copySampleVideoToExportDirectory(videoFilename)
        Workspace.activeStory.addVideo(videoFilename)
        PhaseNavigator.selectPhase(Constants.Phase.share)

        onView(withText(CoreMatchers.containsString(videoFilename))).check(matches(isDisplayed()))
    }

    private fun copySampleVideoToExportDirectory(videoFilename: String) {
        try {
            val sampleVideo = File(Constants.espressoResourceDirectory + File.separator + videoFilename)
            val destination = File(Constants.exportedVideosDirectory + File.separator + videoFilename)
            File(Constants.exportedVideosDirectory).mkdirs()
            sampleVideo.copyRecursively(destination, true)
        } catch (e: Exception){
            Assert.fail("Failed to copy sample video to exported videos directory for test.")
        }
    }

}
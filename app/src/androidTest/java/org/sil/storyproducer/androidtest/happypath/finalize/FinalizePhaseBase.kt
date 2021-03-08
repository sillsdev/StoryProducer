package org.sil.storyproducer.androidtest.happypath.finalize

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.SwipablePhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.film.R
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace
import java.io.File
import java.util.*

class FinalizePhaseBase(sharedBase: SharedBase) : SwipablePhaseTestBase(sharedBase) {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.finalize, base)
    }

    fun test_updateLocalCredits() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.finalize)

        // Local Credits Constant
        val credits : String = Constants.resources.getString(R.string.LC_starting_text)
        val newText : String = "Edited By Espresso!"

        assert(Workspace.activeStory.localCredits.isNotEmpty())
        Workspace.activeStory.localCredits = credits

        val updateLocalCredits = onView(Matchers.allOf(withId(R.id.button_local_credits),
                withText(Constants.resources.getString(R.string.export_local_credits_unchanged)),
                isDisplayed()))

        val saveButton = onView(Matchers.allOf(withId(android.R.id.button1), isDisplayed()))

        updateLocalCredits.perform(click())
        onView(withId(R.id.edit_text_input)).perform(replaceText(""))
        saveButton.perform(click())
        Thread.sleep(500)

        // Ensure that the local credits can't be erased
        assert(credits == Workspace.activeStory.localCredits)

        updateLocalCredits.perform(click())
        // Update the text to newText
        onView(withId(R.id.edit_text_input)).perform(replaceText(newText))
        saveButton.perform(click())
        Thread.sleep(500)

        assert(Workspace.activeStory.localCredits == newText)
    }

    // Will fail if updateLocalCredits() doesn't work correctly.
    fun when_createVideoButtonPressedWithDefaultOptions_should_produceVideoFileWithMp4Extension() {
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.finalize)

        val videoTitle = generateUniqueVideoTitle()
        Espresso.onView(allOf(withId(R.id.editText_export_title), isDisplayed())).perform(clearText()).perform((typeText(videoTitle)))
        Espresso.closeSoftKeyboard()

        var videoCreationIdling = VideoCreationIdlingResource()
        IdlingRegistry.getInstance().register(videoCreationIdling)
        // click the create video button
        Espresso.onView(allOf(withId(R.id.button_export_start), isDisplayed())).perform(click())
        // verify that the expected video file exists on disk
        waitForVideoToExist(videoTitle, Constants.durationToWaitForVideoExport)

        IdlingRegistry.getInstance().unregister(videoCreationIdling)
    }

    private fun generateUniqueVideoTitle(): String {
        val currentDate = Date()
        return base.getStoryName().replace(" ", "_") + currentDate.time.toString()
    }

    private fun waitForVideoToExist(videoTitle: String, timeout: Long) {
        val startTime = System.currentTimeMillis()
        var foundTheVideo = false
        var exceededTheTimeout = false
        while (!foundTheVideo && !exceededTheTimeout) {
            foundTheVideo = doesVideoFileExist(videoTitle,".mp4")
            exceededTheTimeout = System.currentTimeMillis() - startTime > timeout
            Thread.sleep(Constants.intervalToWaitBetweenCheckingForVideoExport)
        }
        if (!foundTheVideo) {
            Assert.fail("Gave up expecting to find an exported video file after waiting " + timeout.toString() + "ms.")
        }
    }

    private fun doesVideoFileExist(videoTitle: String, extension: String) : Boolean{
        val files = File(Constants.exportedVideosDirectory).listFiles() ?: return false
        for (f in files){
            if(f.name.contains(videoTitle) && f.name.contains(extension))
                return true
        }
        return false
    }

}
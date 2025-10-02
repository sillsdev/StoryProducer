package org.sil.storyproducer.androidtest.happypath.finalize

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.SwipablePhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.happypath.translate_revise.TranslatePhaseBase
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace
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
        val newText = "Edited By Espresso!"

        assert(Workspace.activeStory.localCredits.isNotEmpty())
        Workspace.activeStory.localCredits = credits

        val updateLocalCredits = onView(Matchers.allOf(withId(R.id.button_local_credits),
                withText(Constants.resources.getString(R.string.export_local_credits_unchanged)),
                isDisplayed()))

        val saveButton = onView(Matchers.allOf(withId(android.R.id.button1), isDisplayed()))

        updateLocalCredits.perform(click())
        onView(withId(R.id.edit_text_input)).perform(replaceText(""))
        Thread.sleep(500)
        saveButton.perform(click())
        Thread.sleep(500)//1500)

        // Ensure that the local credits can't be erased
        assert(credits == Workspace.activeStory.localCredits)
        Thread.sleep(500)

        updateLocalCredits.perform(click())
        // Update the text to newText
        Thread.sleep(500)
        onView(withId(R.id.edit_text_input)).perform(replaceText(newText))
        Thread.sleep(500)
        saveButton.perform(click())
        Thread.sleep(500)//1500)

        assert(Workspace.activeStory.localCredits == newText)
    }

    // Will fail if updateLocalCredits() doesn't work correctly.
    fun when_createVideoButtonPressedWithDefaultOptions_should_produceVideoFileWithMp4Extension() {

        // After the directory gets wiped out, create one valid recording
        PhaseNavigator.doInPhase(Constants.Phase.translate, {
            val translateReviseBase = TranslatePhaseBase(base)
            translateReviseBase.should_BeAbleToRecordTranslationForASlide()
        }, Constants.Phase.finalize)

        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.finalize)

        val videoTitle = generateUniqueVideoTitle()
        onView(allOf(withId(R.id.editText_export_title), isDisplayed())).perform(clearText()).perform((typeText(videoTitle)))
        Espresso.closeSoftKeyboard()

        // Added code to edit credits and enable creating video button
        onView(allOf(withId(R.id.button_local_credits), isDisplayed())).perform(click())
        onView(allOf(withId(R.id.edit_text_input), isDisplayed())).perform(clearText()).perform((typeText("My Credits")))
        val saveButton = onView(Matchers.allOf(withId(android.R.id.button1), isDisplayed()))
        saveButton.perform(click())

        val videoCreationIdling = VideoCreationIdlingResource()
        IdlingRegistry.getInstance().register(videoCreationIdling)

        try {
            // click the create video button
            onView(allOf(withId(R.id.button_export_start), isDisplayed())).perform(click())
            // verify that the expected video file exists on disk
            waitForVideoToExist(videoTitle, Constants.durationToWaitForVideoExport)
        } finally {
            IdlingRegistry.getInstance().unregister(videoCreationIdling)
        }
    }

    private fun generateUniqueVideoTitle(): String {
        val currentDate = Date()
        return base.getStoryName().replace(" ", "_") + currentDate.time.toString()
    }

    private fun waitForVideoToExist(videoTitle: String, timeout: Long) {
        val startTime = System.currentTimeMillis()
        var foundTheMp4Video = false
        var foundThe3gpVideo = false
        var exceededTheTimeout = false
        while ((!foundTheMp4Video || !foundThe3gpVideo) && !exceededTheTimeout) {
            foundTheMp4Video = doesVideoFileExist(videoTitle,".mp4")
            foundThe3gpVideo = doesVideoFileExist(videoTitle,".3gp")
            exceededTheTimeout = System.currentTimeMillis() - startTime > timeout
            Thread.sleep(Constants.intervalToWaitBetweenCheckingForVideoExport)
        }
        // sleep so we can see it
        Thread.sleep(1000)
        if (!foundTheMp4Video || !foundThe3gpVideo) {
            Assert.fail("Gave up expecting to find an exported video file after waiting " + timeout.toString() + "ms.")
        }
    }

    private fun doesVideoFileExist(videoTitle: String, extension: String) : Boolean{
        // 10/23/2021 - DKH: Update for "Espresso test fail for Android 10 and 11" Issue #594
        // For Android 10 and subsequent Android versions, an App (ie Story Producer)
        // has to ask the user for permission to access files in external storage.
        // New special file classes are used to read/write/delete
        // files and directories in external storage.
        // Refactor this routine to use Android 10 scoped storage

        // Grab the workspace from Story Producer (this is the workspace that Espresso
        // told Story Producer to use during test startup/initialization).
        val WSDoc = Workspace.workdocfile

        // See if the video directory exists
        WSDoc.findFile(Constants.exportedVideosDirectory)?.let {
            // Video directory exists, grab a list of files in the video directory
            val videoList = it.listFiles()
            for (f in videoList){
                // iterate through the file list looking for the target video file
                // if file found, return true
                if(f.name!!.contains(videoTitle) && f.name!!.contains(extension)) return true
            }
        }
        return false  // file not found
    }

}

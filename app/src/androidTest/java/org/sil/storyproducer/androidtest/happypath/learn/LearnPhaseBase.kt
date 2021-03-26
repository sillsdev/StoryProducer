package org.sil.storyproducer.androidtest.happypath.learn

import androidx.appcompat.widget.AppCompatSeekBar
import android.view.View.VISIBLE
import android.widget.ImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.*
import junit.framework.Assert.assertTrue
import org.sil.storyproducer.androidtest.happypath.PhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.R

class LearnPhaseBase(sharedBase: SharedBase) : PhaseTestBase() {

    private var base = sharedBase

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.learn, base)
    }

    fun should_BeAbleToUsePlayButton() {
        val learnPhaseVideoSeekBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(base.getPlayerSeekbarId())
        // check progress of seek bar
        val firstProgress = learnPhaseVideoSeekBar!!.progress
        // click play button
        onView(withId(base.getPlaybackButtonId())).perform(click())
        // wait a few seconds for narration to play and story to move to next slide
        Thread.sleep(Constants.durationToPlayNarration)
        // click pause button
        onView(withId(base.getPlaybackButtonId())).perform(click())
        // check progress of seek bar
        val secondProgress = learnPhaseVideoSeekBar.progress
        //assert(secondProgress > firstProgress)
        assertTrue("Expected progress to increase.", secondProgress > firstProgress)
    }

    fun should_BeAbleToRecordAudioClip() {
        AnimationsToggler.withoutCustomAnimations {

            // check whether the triangle Play button exists. If so, there is a previous recording.
            val trianglePlayButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(R.id.play_recording_button)

            // Wait for toast to go away
            Thread.sleep(4000)

            // click 'mic' button to start recording
            onView(allOf(withId(R.id.start_recording_button))).perform(click())

            Thread.sleep(500)

            // if 'Overwrite' dialog box pops up, click 'YES'
            if (trianglePlayButton?.visibility == VISIBLE) {
                onView(withText("YES")).perform(click())
            }
            // wait a few seconds
            Thread.sleep(Constants.durationToRecordLearnClip)

            // click button to stop recording
            onView(allOf(withId(R.id.start_recording_button))).perform(click())
        }
    }
}

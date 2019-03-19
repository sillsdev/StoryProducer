package org.sil.storyproducer.androidtest.happypath

import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.view.View.VISIBLE
import android.widget.ImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import junit.framework.Assert.assertTrue
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@LargeTest
@RunWith(AndroidJUnit4::class)
class LearnPhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.learn)
    }

    @Test
    fun should_BeAbleToUsePlayButton() {
        val learnPhaseVideoSeekBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        // check progress of seek bar
        val firstProgress = learnPhaseVideoSeekBar!!.progress
        // click play button
        onView(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button)).perform(click())
        // wait a few seconds for narration to play and story to move to next slide
        Thread.sleep(Constants.durationToPlayNarration)
        // click pause button
        onView(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button)).perform(click())
        // check progress of seek bar
        val secondProgress = learnPhaseVideoSeekBar.progress
        //assert(secondProgress > firstProgress)
        assertTrue("Expected progress to increase.", secondProgress > firstProgress)
    }

    @Test
    fun should_BeAbleToRecordAudioClip() {
        // disable the color-changing recording toolbar because it trips up Espresso
        disableCustomAnimations()
        // check whether the triangle Play button exists. If so, there is a previous recording.
        val trianglePlayButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.play_recording_button)

        // click 'mic' button to start recording
        onView(allOf(isDisplayed(), withId(org.sil.storyproducer.R.id.start_recording_button))).perform(click())
        // if 'Overwrite' dialog box pops up, click 'YES'
        if (trianglePlayButton?.visibility == VISIBLE) {
            onView(withText("YES")).perform(click())
        }
        // wait a few seconds
        Thread.sleep(Constants.durationToRecordLearnClip)
        // click button to stop recording
        onView(allOf(isDisplayed(), withId(org.sil.storyproducer.R.id.start_recording_button))).perform(click())
        // re-enable the color-changing recording toolbar
        enableCustomAnimations()
    }

    private fun enableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(ActivityAccessor.getCurrentActivity()).edit()
        preferencesEditor.remove(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation))
    }

    private fun disableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(ActivityAccessor.getCurrentActivity()).edit()
        preferencesEditor.putBoolean(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), true)
        preferencesEditor.commit()
    }

}

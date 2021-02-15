package org.sil.storyproducer.androidtest.happypath

import androidx.appcompat.widget.AppCompatSeekBar
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.film.R

abstract class PlayerPhaseTestBase(sharedBase: SharedBase) : SwipablePhaseTestBase(sharedBase) {

    fun pressMicButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.start_recording_button), ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }

    fun giveAppTimeToRecordAudio() {
        Thread.sleep(Constants.durationToRecordTranslatedClip)
    }

    fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(base.getPlayerSeekbarId())
        return progressBar!!.progress
    }

    fun pressPlayPauseButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(base.getPlaybackButtonId()), ViewMatchers.isDisplayed())).perform(ViewActions.click())
    }

    fun giveAppTimeToPlayAudio() {
        Thread.sleep(Constants.durationToPlayTranslatedClip)
    }
}
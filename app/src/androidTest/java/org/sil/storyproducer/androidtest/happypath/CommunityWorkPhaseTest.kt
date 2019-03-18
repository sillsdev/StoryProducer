package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.RecyclerView
import android.view.View.INVISIBLE
import android.widget.ImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@LargeTest
@RunWith(AndroidJUnit4::class)
class CommunityWorkPhaseTest : SwipablePhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.communityWork)
    }

    @Test
    fun should_BeAbleToSwipeBetweenSlides() {
        testSwipingBetweenSlides()
    }

    @Test
    fun should_BeAbleToPlayTranslationOfASlide() {
        makeSureAnAudioClipIsAvailable(Constants.Phase.communityWork)

        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        Thread.sleep(Constants.durationToPlayTranslatedClip)
        pressPlayPauseButton()
        val progressAfterPausing = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected progress bar to increase in position.", progressAfterPausing > originalProgress)
    }

    @Test
    fun should_BeAbleToRecordFeedback() {
        var originalNumberOfRecordings = getCurrentNumberOfRecordings()

        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            Thread.sleep(Constants.durationToRecordFeedbackClip)
            pressMicButton()
        }

        var finalNumberOfRecordings = getCurrentNumberOfRecordings()
        Assert.assertEquals("Expected an additional feedback recording to exist", originalNumberOfRecordings + 1, finalNumberOfRecordings)
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        testSwipingToNextPhase(Constants.Phase.accuracyCheck)
    }

    private fun getCurrentNumberOfRecordings() =
            ActivityAccessor.getCurrentActivity()!!.findViewById<RecyclerView>(R.id.recordings_list)!!.childCount

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }
}
package org.sil.storyproducer.androidtest.happypath

import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.widget.ImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import junit.framework.Assert.assertTrue
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@LargeTest
@RunWith(AndroidJUnit4::class)
class AccuracyCheckPhaseTest : PhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToAccuracyCheckPhase()
    }

    @Test
    fun shouldBeAbleToPlayRecordedAudioForSpecificSlide() {
        //TODO
    }

    @Test
    fun shouldBeAbleToToggleApprovedState() {
        //TODO
    }

    @Test
    fun shouldBeAbleToSwipeBetweenSlides() {
        //TODO
    }

    @Test
    fun passwordConfirmationPopupShouldBehaveCorrectly() {
        //TODO
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

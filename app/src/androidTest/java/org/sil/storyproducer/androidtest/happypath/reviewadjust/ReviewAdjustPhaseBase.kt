package org.sil.storyproducer.androidtest.happypath.reviewadjust

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.sil.storyproducer.androidtest.happypath.PlayerPhaseTestBase
import org.sil.storyproducer.androidtest.happypath.SwipablePhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.Constants.durationToPlayTranslatedClip
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator.isDisplayed
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator.isNotDisplayed
import org.sil.storyproducer.film.R

class ReviewAdjustPhaseBase(sharedBase: SharedBase) : PlayerPhaseTestBase(sharedBase) {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToPhase(Constants.Phase.reviewAdjust, base)
    }

    fun should_beAbleToSwipeToNextPhase() {
        test_swipingToNextPhase(Constants.Phase.finalize)
    }

    fun test_swipingBetweenSlidesReviewAdjust() {
        swipeLeftOnSlide()
        swipeRightOnSlide()
        swipeLeftOnSlide()
        swipeRightOnSlide()
    }

    fun test_shouldBeAbleToAdjustAudio() {
        helper_setupReviewAdjust();

        var leftArrow = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.film_studio_narration_move_left), isDisplayed()))
        var rightArrow = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.film_studio_narration_move_right), isDisplayed()))

        for(i in 1..5) {
            rightArrow.perform(click())
        }
        for(i in 1..5) {
            leftArrow.perform(click())
        }
        for(i in 1..5) {
            rightArrow.perform(click())
            leftArrow.perform(click())
        }

        rightArrow.perform(longClick())
        leftArrow.perform(longClick())

        // Test Playing Audio Again
        pressPlayPauseButton()
        Thread.sleep(durationToPlayTranslatedClip)
        pressPlayPauseButton()
    }
    
    fun test_moveAudioWhilePlaying() {
        helper_setupReviewAdjust()
        pressPlayPauseButton()

        var leftArrow = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.film_studio_narration_move_left)))
        var rightArrow = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.film_studio_narration_move_right)))

        Assert.assertTrue("Left Arrow should be hidden when audio playing", leftArrow.isNotDisplayed())
        Assert.assertTrue("Right Arrow should be hidden when audio playing", rightArrow.isNotDisplayed())

        pressPlayPauseButton()
    }

    fun helper_setupReviewAdjust() {
        // Record Audio in Translate Phase
        PhaseNavigator.doInPhase(Constants.Phase.translate, {
            AnimationsToggler.withoutCustomAnimations {
                pressMicButton()
                giveAppTimeToRecordAudio()
                pressMicButton()
            }
        }, Constants.Phase.accuracyCheck)

        // Approve the slides so the Review + Adjust Phase is Unlocked
        PhaseNavigator.doInPhase(Constants.Phase.accuracyCheck, {
            approveSlides()
        }, Constants.Phase.reviewAdjust)
    }

}

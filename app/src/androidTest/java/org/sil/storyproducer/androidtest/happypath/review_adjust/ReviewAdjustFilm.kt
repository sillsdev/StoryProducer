package org.sil.storyproducer.androidtest.happypath.review_adjust

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.happypath.PhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.FilmBase
import org.sil.storyproducer.androidtest.happypath.base.annotation.FilmTest

@LargeTest
@FilmTest
@RunWith(AndroidJUnit4::class)
class ReviewAdjustFilm : FilmBase() {

    private var base: ReviewAdjustPhaseBase = ReviewAdjustPhaseBase(this)

    @Before
    fun setup() {
        PhaseTestBase.revertWorkspaceToCleanState(this)
        base.setUp()
    }

    @Test
    fun test_shouldBeAbleToAdjustAudio() {
        base.test_shouldBeAbleToAdjustAudio()
    }

    @Test
    fun test_moveAudioWhilePlaying() {
        base.test_moveAudioWhilePlaying()
    }

    @Test
    fun test_swipingBetweenSlidesReviewAdjust() {
        base.test_swipingBetweenSlidesReviewAdjust()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        base.should_beAbleToSwipeToNextPhase()
    }
}
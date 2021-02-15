package org.sil.storyproducer.androidtest.happypath.accuracy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.happypath.PhaseTestBase
import org.sil.storyproducer.androidtest.happypath.base.PhotoBase
import org.sil.storyproducer.androidtest.happypath.base.annotation.PhotoTest

@LargeTest
@PhotoTest
@RunWith(AndroidJUnit4::class)
class AccuracyPhoto() : PhotoBase() {
    private var base: AccuracyPhaseBase = AccuracyPhaseBase(this)

    @Before
    fun setup() {
        PhaseTestBase.revertWorkspaceToCleanState(this)
        base.setUp()
    }

    @Test
    fun shouldBeAbleToSwipeBetweenSlides() {
        base.shouldBeAbleToSwipeBetweenSlides()
    }

    @Test
    fun shouldBeAbleToPlayRecordedAudioForSpecificSlide() {
        base.shouldBeAbleToPlayRecordedAudioForSpecificSlide()
    }

    @Test
    fun shouldBeAbleToToggleApprovedState() {
        base.shouldBeAbleToToggleApprovedState()
    }

    @Test
    fun passwordConfirmationPopupShouldBehaveCorrectly() {
        base.passwordConfirmationPopupShouldBehaveCorrectly()
    }
}
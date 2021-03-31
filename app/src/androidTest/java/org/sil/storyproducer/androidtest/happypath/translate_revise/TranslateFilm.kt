package org.sil.storyproducer.androidtest.happypath.translate_revise

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
class TranslateFilm : FilmBase() {
    private var base: TranslatePhaseBase = TranslatePhaseBase(this)

    @Before
    fun setup() {
        PhaseTestBase.revertWorkspaceToCleanState(this)
        base.setUp()
    }

    @Test
    fun should_BeAbleToSwipeBetweenSlides() {
        base.should_BeAbleToSwipeBetweenSlides()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        base.should_beAbleToSwipeToNextPhase()
    }

    @Test
    fun should_BeAbleToPlayNarrationOfASlide() {
        base.should_BeAbleToPlayNarrationOfASlide()
    }

    @Test
    fun should_BeAbleToRecordTranslationForASlide() {
        base.should_BeAbleToRecordTranslationForASlide()
    }

}
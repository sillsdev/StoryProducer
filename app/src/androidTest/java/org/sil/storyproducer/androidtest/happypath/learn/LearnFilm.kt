package org.sil.storyproducer.androidtest.happypath.learn

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
class LearnFilm : FilmBase() {

    private var base: LearnPhaseBase = LearnPhaseBase(this)

    @Before
    fun setup() {
        PhaseTestBase.revertWorkspaceToCleanState(this)
        base.setUp()
    }

    @Test
    fun should_BeAbleToUsePlayButton() {
        base.should_BeAbleToUsePlayButton()
    }

    @Test
    fun should_BeAbleToRecordAudioClip() {
        base.should_BeAbleToRecordAudioClip()
    }

}

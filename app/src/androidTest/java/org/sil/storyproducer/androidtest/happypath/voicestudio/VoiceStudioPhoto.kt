package org.sil.storyproducer.androidtest.happypath.voicestudio

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
class VoiceStudioPhoto() : PhotoBase() {
    private var base: VoiceStudioPhaseBase = VoiceStudioPhaseBase(this)

    @Before
    fun setup() {
        PhaseTestBase.revertWorkspaceToCleanState(this)
        base.setUp()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        base.should_beAbleToSwipeToNextPhase()
    }

    @Test
    fun should_beAbleToPlaySlideAudio() {
        base.should_beAbleToPlaySlideAudio()
    }

    @Test
    fun should_beAbleToSwipeBetweenSlides() {
        base.should_beAbleToSwipeBetweenSlides()
    }

    @Test
    fun should_beAbleToRecordSequentialAudioSnippetsAsOneClip() {
        base.should_beAbleToRecordSequentialAudioSnippetsAsOneClip()
    }
}
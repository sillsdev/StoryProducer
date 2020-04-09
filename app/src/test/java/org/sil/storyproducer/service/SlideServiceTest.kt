package org.sil.storyproducer.service

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Story

@RunWith(MockitoJUnitRunner::class)
class SlideServiceTest {

    @Mock lateinit var context: Context
    @Mock lateinit var story: Story
    @Mock lateinit var fontCoverSlide: Slide
    @Mock lateinit var slide1: Slide

    @InjectMocks lateinit var slideService: SlideService

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        `when`(story.title).thenReturn("story title")
        `when`(story.slides).thenReturn(listOf(fontCoverSlide, slide1))
    }

    @Test
    fun testShouldShowDefaultImage_WhenStoryIsMissing() {
        `when`(story.title).thenReturn("")

        assertTrue(slideService.shouldShowDefaultImage(1, story, PhaseType.LEARN))
    }

    @Test
    fun testShouldShowDefaultImage_ForTheFirstSlideInLearnPhase() {
        `when`(fontCoverSlide.imageFile).thenReturn("0_Local.jpg")

        assertTrue(slideService.shouldShowDefaultImage(0, story, PhaseType.LEARN))
    }

    @Test
    fun testShouldNotShowDefaultImageForSecondSlideInLearnPhase() {
        `when`(slide1.imageFile).thenReturn("1.jpg")

        assertFalse(slideService.shouldShowDefaultImage(1, story, PhaseType.LEARN))
    }

    @Test
    fun testNotShouldShowDefaultImage_ForTheFirstSlideInDraftPhaseWhenImageFileExists() {
        `when`(fontCoverSlide.imageFile).thenReturn("0_Local.jpg")

        assertFalse(slideService.shouldShowDefaultImage(0, story, PhaseType.DRAFT))
    }

    @Test
    fun testNotShouldShowDefaultImage_ForTheFirstSlideInDraftPhaseWhenImageFileDoesNotExist() {
        `when`(fontCoverSlide.imageFile).thenReturn("")

        assertTrue(slideService.shouldShowDefaultImage(0, story, PhaseType.DRAFT))
    }

}
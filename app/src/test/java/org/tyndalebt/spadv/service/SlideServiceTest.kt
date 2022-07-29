package org.tyndalebt.spadv.service

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.tyndalebt.spadv.model.Slide
import org.tyndalebt.spadv.model.Story

@RunWith(MockitoJUnitRunner::class)
class SlideServiceTest {

    @Mock lateinit var context: Context
    @Mock lateinit var story: Story
    @Mock lateinit var fontCoverSlide: Slide
    @Mock lateinit var slide1: Slide

    @InjectMocks lateinit var slideService: SlideService

    @Before
    fun before() {
        `when`(story.title).thenReturn("story title")
        `when`(story.slides).thenReturn(listOf(fontCoverSlide, slide1))
    }

    @Test
    fun testShouldShowDefaultImage_WhenStoryIsMissing() {
        `when`(story.title).thenReturn("")

        assertTrue(slideService.shouldShowDefaultImage(1, story))
    }

    @Test
    fun testNotShouldShowDefaultImage_WhenImageFileExists() {
        `when`(fontCoverSlide.imageFile).thenReturn("0_Local.jpg")

        assertFalse(slideService.shouldShowDefaultImage(0, story))
    }

    @Test
    fun testNotShouldShowDefaultImage_WhenImageFileDoesNotExist() {
        `when`(fontCoverSlide.imageFile).thenReturn("")

        assertTrue(slideService.shouldShowDefaultImage(0, story))
    }

}
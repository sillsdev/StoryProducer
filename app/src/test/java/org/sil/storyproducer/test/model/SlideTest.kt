package org.sil.storyproducer.test.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.sil.storyproducer.model.Slide

@RunWith(MockitoJUnitRunner::class)
class SlideTest {

    // The 'first title idea' is the text we want to show.
    // Drop any content within square brackets.
    // When it ends in a period, drop the period.
    @Test
    fun testWidowsGiftFrontCoverTitle() {
        val slide = Slide().apply {
            content = "Title ideas:\n" +
                    "Jesus honored [praised] a poor widow.\n" +
                    "Can poor people please God?\n" +
                    "Little is much!\n" +
                    "Scripture Reference:\n" +
                    "Mark 12:41-44\n" +
                    "Lk 21:1-4"
        }

        assertEquals("Jesus honored a poor widow", slide.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_NoTitleIdea() {
        val slide = Slide().apply {
            content = "Title ideas:\n" +
                    "\n" +
                    "Scripture Reference:\n" +
                    "Mark 12:41-44\n" +
                    "Lk 21:1-4"
        }

        assertEquals("", slide.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_EmptyContent() {
        val slide = Slide().apply {
            content = ""
        }

        assertEquals("", slide.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_NoScriptureReference() {
        val slide = Slide().apply {
            content = "Title ideas:\n" +
                    "Jesus honored [praised] a poor widow.\n" +
                    "Can poor people please God?\n" +
                    "Little is much!\n"
        }

        assertEquals("Jesus honored a poor widow", slide.getFrontCoverTitle())
    }

    // The 2nd line is assumed to be the first title idea even when the "Title ideas:" heading is missing.
    @Test
    fun testWidowsGiftFrontCoverTitle_MissingTitleIdeasHeading() {
        val slide = Slide().apply {
            content = "Jesus honored [praised] a poor widow.\n" +
                    "Can poor people please God?\n" +
                    "Little is much!\n" +
                    "Scripture Reference:\n" +
                    "Mark 12:41-44\n" +
                    "Lk 21:1-4"
        }

        assertEquals("Can poor people please God?", slide.getFrontCoverTitle())
    }

}
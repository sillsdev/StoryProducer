package org.tyndalebt.spadv.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.tyndalebt.spadv.model.Slide

@RunWith(MockitoJUnitRunner::class)
class SlideViewModelBuilderTest {

    // The 'first title idea' is the text we want to show.
    // Drop any content within square brackets.
    // When it ends in a period, drop the period.
    @Test
    fun testWidowsGiftFrontCoverTitle() {
        val slide = Slide().apply {
            content = "Title ideas:\n" +
                    "Jesus honored [praised] a poor widow.\n" +
                    "Can poor people please God?\n" +
                    "Little is much!"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus honored a poor widow", viewModel.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_NoTitleIdea() {
        val slide = Slide().apply {
            content = "Title ideas:\n" +
                    ""
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("", viewModel.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_EmptyContent() {
        val slide = Slide().apply {
            content = ""
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("", viewModel.getFrontCoverTitle())
    }

}
package org.sil.storyproducer.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.sil.storyproducer.model.Slide

@RunWith(MockitoJUnitRunner::class)
class SlideViewModelBuilderTest {

    // The 'first title idea' is the text we want to show.
    // Drop any content within square brackets.
    // When it ends in a period, drop the period.
    @Test
    fun testWidowsGiftFrontCoverTitle() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Mark 12:41-44;Lk 21:1-4\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea=Jesus honored [praised] a poor widow.\n" +
                    "TitleIdea=Can poor people please God?\n" +
                    "TitleIdea=Little is much!"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus honored a poor widow", viewModel.getFrontCoverTitle())
    }

    @Test
    fun testWidowsGiftFrontCoverTitle_NoTitleIdea() {
        val slide = Slide().apply {
            content = "TitleIdeasHeading=Title ideas:\n" +
                    "\n" +
                    "ScriptureReference=Mark 12:41-44; Lk 21:1-4"
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

    @Test
    fun testWidowsGiftFrontCoverTitle_NoScriptureReference() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea=Jesus honored [praised] a poor widow.\n" +
                    "TitleIdea=Can poor people please God?\n" +
                    "TitleIdea=Little is much!"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus honored a poor widow", viewModel.getFrontCoverTitle())
    }

    // The 'first title idea' is still the first title idea when the "Title ideas:" heading is missing.
    @Test
    fun testWidowsGiftFrontCoverTitle_MissingTitleIdeasHeading() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Mark 12:41-44;Lk 21:1-4\n" +
                    "TitleIdea=Jesus honored [praised] a poor widow.\n" +
                    "TitleIdea=Can poor people please God?\n" +
                    "TitleIdea=Little is much!"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus honored a poor widow", viewModel.getFrontCoverTitle())
    }

    @Test
    fun testBuildFrontCoverTitleIdeas() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Matt 14.22-33; Mk 6.45-50; John 6.15-20\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea=Jesus walked on the water.\n" +
                    "TitleIdea=Jesus stopped a storm.\n" +
                    "TitleIdea=Jesus’s disciples understand who he is.\n" +
                    "TitleIdea=Is Jesus God?"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus walked on the water", viewModel.getFrontCoverTitle())
        assertEquals(
                "Title ideas:\n" +
                "Jesus walked on the water.\n" +
                "Jesus stopped a storm.\n" +
                "Jesus’s disciples understand who he is.\n" +
                "Is Jesus God?",
                viewModel.buildFrontCoverTitleIdeas()
        )
    }

    @Test
    fun testBuildFrontCoverTitleIdeas_WhenNumbered() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Matt 14.22-33; Mk 6.45-50; John 6.15-20\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea1=Jesus walked on the water.\n" +
                    "TitleIdea2=Jesus stopped a storm.\n" +
                    "TitleIdea3=Jesus’s disciples understand who he is.\n" +
                    "TitleIdea4=Is Jesus God?"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus walked on the water", viewModel.getFrontCoverTitle())
        assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
                viewModel.buildFrontCoverTitleIdeas()
        )
    }

    @Test
    fun testBuildFrontCoverTitleIdeas_WhenNumberedOutOfOrder() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Matt 14.22-33; Mk 6.45-50; John 6.15-20\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea3=Jesus walked on the water.\n" +
                    "TitleIdea2=Jesus stopped a storm.\n" +
                    "TitleIdea1=Jesus’s disciples understand who he is.\n" +
                    "TitleIdea4=Is Jesus God?"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("Jesus walked on the water", viewModel.getFrontCoverTitle())
        assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
                viewModel.buildFrontCoverTitleIdeas()
        )
    }

    @Test
    fun testBuildFrontCoverTitleIdeas_WhenSpaceAroundEquals() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic= gray\n" +
                    "ScriptureReference =Genesis 1 &amp; 2\n" +
                    "TitleIdeasHeading= Title ideas:\n" +
                    "TitleIdea=The true story / account about how God created / made the earth and the first people.\n" +
                    "TitleIdea2=God created everything\n" +
                    "TitleIdea = The beginning"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("The true story / account about how God created / made the earth and the first people", viewModel.getFrontCoverTitle())
        assertEquals(
                "Title ideas:\n" +
                        "The true story / account about how God created / made the earth and the first people.\n" +
                        "God created everything\n" +
                        "The beginning",
                viewModel.buildFrontCoverTitleIdeas()
        )
    }

    @Test
    fun testBuildFrontCoverTitleIdeas_WhenContainsZeroWidthCharacter() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic= gray\n" +
                    "ScriptureReference =Genesis 1 &amp; 2\n" +
                    "TitleIdeasHeading= Title ideas:\n" +
                    "\u200CTitleIdea=The true story / account about how God created / made the earth and the first people.\n" +
                    "TitleIdea2=God created everything\n" +
                    "TitleIdea = The beginning\u200C"
        }

        val viewModel = SlideViewModelBuilder(slide)
        assertEquals("The true story / account about how God created / made the earth and the first people", viewModel.getFrontCoverTitle())
        assertEquals(
                "Title ideas:\n" +
                        "The true story / account about how God created / made the earth and the first people.\n" +
                        "God created everything\n" +
                        "The beginning",
                viewModel.buildFrontCoverTitleIdeas()
        )
    }

}
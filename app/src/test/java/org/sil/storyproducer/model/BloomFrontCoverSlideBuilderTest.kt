package org.sil.storyproducer.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BloomFrontCoverSlideBuilderTest {

    val builder = BloomFrontCoverSlideBuilder()

    @Test
    fun testBuildFrontCoverTitleIdeas() {
        val slide = Slide().apply {
            content = "Video Title Slide Content\n" +
                    "Graphic=gray\n" +
                    "ScriptureReference=Matt 14.22-33; Mk 6.45-50; John 6.15-20\n" +
                    "TitleIdeasHeading=Title ideas:\n" +
                    "TitleIdea=\"Jesus walked on the water.\"\n" +
                    "TitleIdea=Jesus stopped a storm.\n" +
                    "TitleIdea=Jesus’s disciples understand who he is.\n" +
                    "TitleIdea='Is Jesus God?'"
        }

        Assert.assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
                builder.buildTitleIdeas(FrontCoverContent(slide.content, "", ""))
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

        Assert.assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
                builder.buildTitleIdeas(FrontCoverContent(slide.content, "", ""))
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

        Assert.assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
                builder.buildTitleIdeas(FrontCoverContent(slide.content, "", ""))
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
                    "TitleIdea     = The beginning"
        }

        Assert.assertEquals(
                "Title ideas:\n" +
                        "The true story / account about how God created / made the earth and the first people.\n" +
                        "God created everything\n" +
                        "The beginning",
                builder.buildTitleIdeas(FrontCoverContent(slide.content, "", ""))
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

        Assert.assertEquals(
                "Title ideas:\n" +
                        "The true story / account about how God created / made the earth and the first people.\n" +
                        "God created everything\n" +
                        "The beginning",
                builder.buildTitleIdeas(FrontCoverContent(slide.content, "", ""))
        )
    }


}
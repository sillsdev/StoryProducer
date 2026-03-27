package org.sil.storyproducer.model

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParseBloomImageLookupTest {

    @Test
    fun findPageImageElement_prefersCoverImageForFrontCover() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="outsideFrontCover">
                    <div class="branding"><img src="branding.svg" alt="branding" /></div>
                    <div class="bloom-canvas" data-initialrect="0.15 0.15 0.7 0.7">
                        <img data-book="coverImage" src="cover.jpg" alt="cover image" />
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "outsideFrontCover").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("cover.jpg", parseImageFromElement(Slide(), true, imageElement))
        assertEquals("0.15 0.15 0.7 0.7", findMotionRectElement(imageElement).attr("data-initialrect"))
    }

    @Test
    fun findPageImageElement_handlesBloom61CoverStructure() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="outsideFrontCover">
                    <div class="bloom-canvas bloom-has-canvas-element">
                        <div class="bloom-canvas-element bloom-backgroundImage" style="width:100%; height:100%;">
                            <div class="bloom-imageContainer">
                                <img data-book="coverImage" src="image.png" alt="" />
                            </div>
                        </div>
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "outsideFrontCover").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("image.png", parseImageFromElement(Slide(), true, imageElement))
    }

    @Test
    fun findPageImageElement_handlesBloom62BloomPubCoverStructure() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="outsideFrontCover">
                    <div class="bloom-canvas bloom-background-image-in-style-attr"
                         data-book="coverImage"
                         style="background-image:url('image.png')">
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "outsideFrontCover").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("image.png", parseImageFromElement(Slide(), true, imageElement))
    }

    @Test
    fun findPageImageElement_prefersBloom62CanvasImage() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-canvas" data-initialrect="0.25 0.25 0.5 0.5">
                        <img src="wrong.jpg" alt="wrong" />
                    </div>
                    <div class="bloom-leadingElement bloom-imageContainer" data-initialrect="0 0 1 1">
                        <img src="right.jpg" alt="right" />
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("wrong.jpg", parseImageFromElement(Slide(), true, imageElement))
        assertEquals("0.25 0.25 0.5 0.5", findMotionRectElement(imageElement).attr("data-initialrect"))
    }

    @Test
    fun findPageImageElement_fallsBackToBloomImageContainer() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-leadingElement bloom-imageContainer" data-initialrect="0.1 0.2 0.3 0.4">
                        <img src="container.jpg" alt="container image" />
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("container.jpg", parseImageFromElement(Slide(), true, imageElement))
        assertEquals("0.1 0.2 0.3 0.4", findMotionRectElement(imageElement).attr("data-initialrect"))
    }

    @Test
    fun findPageImageElement_handlesBloom61NumberedPageStructure() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-imageContainer bloom-leadingElement"
                         data-initialrect="0.08528784648187633 0.03125 0.7505330490405118 0.75"
                         data-finalrect="0.18976545842217485 0.09943181818181818 0.5010660980810234 0.5">
                        <img src="image1.png" alt="" />
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("image1.png", parseImageFromElement(Slide(), true, imageElement))
        assertEquals(
                "0.08528784648187633 0.03125 0.7505330490405118 0.75",
            findMotionRectElement(imageElement).attr("data-initialrect")
        )
    }

    @Test
    fun findPageImageElement_handlesBloom62NumberedPageStructure() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-canvas bloom-leadingElement bloom-has-canvas-element"
                         data-initialrect="0.11727078891257996 0.02556818181818182 0.7505330490405118 0.75"
                         data-finalrect="0.24520255863539445 0.10227272727272728 0.5010660980810234 0.5">
                        <div class="bloom-canvas-element bloom-backgroundImage">
                            <div class="bloom-leadingElement bloom-imageContainer">
                                <img src="image1.png" alt="" />
                            </div>
                        </div>
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("image1.png", parseImageFromElement(Slide(), true, imageElement))
        assertEquals(
                "0.11727078891257996 0.02556818181818182 0.7505330490405118 0.75",
            findMotionRectElement(imageElement).attr("data-initialrect")
        )
    }

    @Test
    fun findPageImageElement_handlesBloom62BloomPubNumberedPageStructure() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-canvas bloom-leadingElement bloom-background-image-in-style-attr"
                         data-initialrect="0.11727078891257996 0.02556818181818182 0.7505330490405118 0.75"
                         data-finalrect="0.24520255863539445 0.10227272727272728 0.5010660980810234 0.5"
                         style="background-image:url('image1.png')">
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("image1.png", parseImageFromElement(Slide(), true, imageElement))
        assertEquals(
                "0.11727078891257996 0.02556818181818182 0.7505330490405118 0.75",
                findMotionRectElement(imageElement).attr("data-initialrect")
        )
    }

    @Test
    fun parseImageFromElement_decodesStylePath() {
        val page = requireNotNull(Jsoup.parse(
                """
                <div class="numberedPage">
                    <div class="bloom-canvas bloom-leadingElement bloom-background-image-in-style-attr"
                         style="background-image:url('folder%20name/image+1.png')">
                    </div>
                </div>
                """.trimIndent()
            ).getElementsByAttributeValueContaining("class", "numberedPage").first())

        val imageElement = requireNotNull(findPageImageElement(page))

        assertEquals("folder name/image+1.png", parseImageFromElement(Slide(), true, imageElement))
    }
}
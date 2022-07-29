package org.tyndalebt.spadv.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BloomFrontCoverSlideBuilderTest {

    val builder = BloomFrontCoverSlideBuilder()

    @Mock lateinit var context: Context
    @Mock lateinit var file: DocumentFile


    @Test
    fun testBuildFrontCoverTitleIdeas() {
        val text = """
            <body>
                <div id="bloomDataDiv">
                    <div lang="*" data-book="contentLanguage1">
                        en
                    </div>
                    <div lang="en" data-book="spTitleIdeasHeading">
                        Title ideas:
                    </div>
                    <div lang="en" data-book="spTitleIdea1">
                        <p><span>Jesus walked on the water.</span></p>
                    </div>
                    <div lang="en" data-book="spTitleIdea2">
                        <p><span>Jesus stopped a storm.</span></p>
                    </div>
            
                    <div lang="en" data-book="spTitleIdea3">
                        <p><span>Jesus’s disciples understand who he is.</span></p>
                    </div>
                    <div lang="en" data-book="spTitleIdea4">
                        <p><span>Is Jesus God?</span></p>
                    </div>
                </div>
                <div class="outsideFrontCover" />
            </body>
        """.trimIndent()

        val slide = builder.build(context, file, Jsoup.parse(text))

        assertEquals(
                "Title ideas:\n" +
                        "Jesus walked on the water.\n" +
                        "Jesus stopped a storm.\n" +
                        "Jesus’s disciples understand who he is.\n" +
                        "Is Jesus God?",
              slide?.content
        )
    }


}
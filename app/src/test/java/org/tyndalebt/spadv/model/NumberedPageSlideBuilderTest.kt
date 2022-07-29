package org.tyndalebt.spadv.model

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NumberedPageSlideBuilderTest {

    val builder = NumberedPageSlideBuilder()

    @Mock lateinit var context: Context
    @Mock lateinit var file: DocumentFile

    @Test
    fun testBuildNumberedPageContent_TokPisinLang() {
        val text = """
            <div class="numberedPage">
                <div class="pageLabel"></div>
                <div class="pageDescription" lang="en"></div>
                <div class="marginBox">
                    <div class="split-pane horizontal-percent">
                        <div class="split-pane-component position-top">
                            <div class="split-pane-component-inner">
                                <div class="bloom-leadingElement">
                                    <div class="bloom-translationGroup bloom-imageDescription bloom-trailingElement">
                                        <div class="bloom-editable ImageDescriptionEdit-style bloom-content1 bloom-contentNational1 bloom-visibility-code-on" lang="tpi">
                                            <p></p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="split-pane-divider horizontal-divider"></div>
                        <div class="split-pane-component position-bottom">
                            <div class="split-pane-component-inner">
                                <div class="split-pane horizontal-percent">
                                    <div class="split-pane-component position-top">
                                        <div class="split-pane-component-inner">
                                            <div class="bloom-translationGroup bloom-trailingElement">
                                                <label class="bubble" lang="en">Narration text</label>
                                                
                                                <div data-languagetipcontent="Tok Pisin" lang="tpi" class="bloom-editable normal-style bloom-content1 bloom-contentNational1 bloom-visibility-code-on">
                                                    <p><span class="audio-sentence">Bipo, planti planti yia [ating 2700 yia i go pinis], wanpela man [poropet bilong Bikpela], nem bilong em Aisaia [Isaia] i autim wanpela tok em i kisim long God.</span> <span class="audio-sentence">Em i tok [na tu em i raitim],        </span></p>
                                                    <p><span class="audio-sentence">“Wanpela yangpela meri [husat i no marit yet] em bai i karim wanpela pikinini man.”</span> <span class="audio-sentence">As [o mining bilong] nem bilong em bai i olsem, God i Stap Wantaim Yumi.</span></p>
                                                    <p></p>
                                                </div>

                                                <div data-languagetipcontent="English" lang="en" class="bloom-editable normal-style audio-sentence">
                                                    <p>Many many [About 2700] years ago, a man named Isaiah [Ayzaya] spoke a message from God. He said, “A virgin [young woman who has not had a child] will birth a son.” His name will mean God Is With Us.</p>
                                                    <p>700 years later, a virgin birthed a son who came from God with no human father.</p>
                                                    <p>[This is the story about how that happened.]</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="split-pane-component position-bottom">
                                        <div class="split-pane-component-inner">
                                            <div class="bloom-translationGroup bloom-trailingElement">
                                                <label class="bubble" lang="en">Scripture reference</label>

                                                <div data-languagetipcontent="Tok Pisin" lang="tpi" class="bloom-editable normal-style bloom-content1 bloom-contentNational1 bloom-visibility-code-on">
                                                    <p>Matyu 1:22-23; Aisaia 7:14</p>
                                                </div>

                                                <div data-languagetipcontent="English"  lang="en" class="bloom-editable normal-style audio-sentence">
                                                    <p>Mt 1:22-23 Isaiah 7:14</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val slide = builder.build(context, file, Jsoup.parse(text), "tpi")

        Assert.assertEquals(
                "Bipo, planti planti yia [ating 2700 yia i go pinis], wanpela man [poropet bilong Bikpela], nem bilong em Aisaia [Isaia] i autim wanpela tok em i kisim long God. Em i tok [na tu em i raitim],\n" +
                        "“Wanpela yangpela meri [husat i no marit yet] em bai i karim wanpela pikinini man.” As [o mining bilong] nem bilong em bai i olsem, God i Stap Wantaim Yumi.",
                slide?.content
        )

        Assert.assertEquals(
                "Matyu 1:22-23; Aisaia 7:14",
                slide?.reference
        )
    }

}
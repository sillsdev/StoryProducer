package org.sil.storyproducer.viewmodel

import android.text.Layout
import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import java.text.SimpleDateFormat
import java.util.*

class SlideViewModelBuilder(
        val slide: Slide
) {

    val frontCoverContent by lazy { FrontCoverContent(slide.content) }

    fun build(): SlideViewModel {
        return SlideViewModel(
                buildOverlayText(),
                buildScriptureText(),
                buildScriptureReference()
        )
    }

    fun buildScriptureText(): String {
        return when (slide.slideType) {
            SlideType.FRONTCOVER -> buildFrontCoverTitleIdeas()
            else -> slide.content
        }
    }

    internal fun buildFrontCoverTitleIdeas(): String {
        return frontCoverContent.run {
            "$titleIdeasHeading\n${titleIdeas.joinToString("\n")}"
        }
    }

    fun buildScriptureReference(): String {
        return when (slide.slideType) {
            SlideType.FRONTCOVER -> arrayOf(frontCoverContent.scriptureReference, slide.reference, slide.subtitle, slide.title)
            else -> arrayOf(slide.reference, slide.subtitle, slide.title)
        }
                .firstOrNull { !it.isNullOrEmpty() }
                .orEmpty()
    }

    fun buildOverlayText(): TextOverlay? {
        return if (Workspace.activePhase.phaseType == PhaseType.LEARN)
            buildOverlayText(false, true)
        else
            buildOverlayText(false, false)
    }

    fun buildOverlayText(dispStory: Boolean = false, origTitle: Boolean = false) : TextOverlay? {
        //There is no text overlay on normal slides or "no slides"
        if(!dispStory){
            if(slide.slideType in arrayOf(SlideType.NUMBEREDPAGE, SlideType.NONE )) return null
        }
        val tOverlay = when(slide.slideType) {
            SlideType.FRONTCOVER -> getFrontCoverOverlayText(origTitle)
            SlideType.LOCALCREDITS -> TextOverlay("${slide.translatedContent}\n" +
                    "This video is licensed under a Creative Commons Attribution" +
                    "-NonCommercial-ShareAlike 4.0 International License " +
                    "© ${SimpleDateFormat("yyyy", Locale.US).format(GregorianCalendar().time)}")
            else -> TextOverlay(slide.translatedContent)
        }
        val fontSize : Int = when(slide.slideType){
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 32
            SlideType.LOCALCREDITS -> 14
            SlideType.COPYRIGHT -> 12
            SlideType.NUMBEREDPAGE, SlideType.LOCALSONG, SlideType.NONE -> 12
        }
        tOverlay.setFontSize(fontSize)

        if(slide.slideType in arrayOf(SlideType.NUMBEREDPAGE,SlideType.LOCALSONG))
            tOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
        return tOverlay
    }

    private fun getFrontCoverOverlayText(origTitle: Boolean): TextOverlay {
        return if (origTitle) TextOverlay(getFrontCoverTitle()) else TextOverlay(slide.translatedContent)
    }

    internal fun getFrontCoverTitle(): String {
        return frontCoverContent
                .titleIdeas.firstOrNull().orEmpty()                     // The 'first title idea' is the text we want to show.
                .let { "\\[[^\\]]*\\]?".toRegex().replace(it, "") }     // Drop any content within square brackets.
                .let { "[\\.].*".toRegex().replace(it, "") }            // When it ends in a period, drop the period.
                .let { "\\s+".toRegex().replace(it, " ") }              // Make all double spaces one space.
    }

}
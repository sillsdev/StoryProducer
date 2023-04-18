package org.tyndalebt.storyproduceradv.viewmodel

import android.text.Layout
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Slide
import org.tyndalebt.storyproduceradv.model.SlideType
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.tools.media.graphics.TextOverlay

class SlideViewModelBuilder(
        val slide: Slide
) {

    fun build(): SlideViewModel {
        return SlideViewModel(
                buildOverlayText(),
                buildScriptureText(),
                buildScriptureReference()
        )
    }

    fun buildScriptureText(): String {
        return slide.content
    }

    fun buildScriptureReference(): String {
        return arrayOf(slide.reference, slide.subtitle, slide.title)
                .firstOrNull { it.isNotEmpty() }
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
            else -> TextOverlay(slide.translatedContent)
        }
        val fontSize : Int = when(slide.slideType){
            // 4/18/23 TM - told to knock the text size down a couple notched to allow for long words - Hindi and such
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 24
            SlideType.LOCALCREDITS -> 0 // Not used, but needs to be here to cover reading of projects v3.0.2 & prior
            SlideType.COPYRIGHT -> 8
            SlideType.NUMBEREDPAGE, SlideType.LOCALSONG, SlideType.NONE -> 12
        }
        val outlineShow : Boolean = when(slide.slideType){
            SlideType.FRONTCOVER, SlideType.LOCALSONG -> true
            else -> false
        }

        tOverlay.setFontSize(fontSize)
        tOverlay.setTextOutline(outlineShow)

        // 0R17 - DKH 05/7/2022 Allow for text editing on the song slide
        // Remove LOCALSONG from if statement -  ALIGN_OPPOSITE starts at the bottom of the page
        // and scrolls up, which is used for NUMBERED PAGE.  LOCALSONG will be centered on the
        // middle of the page
        if(slide.slideType in arrayOf(SlideType.NUMBEREDPAGE)) {
            tOverlay.setVerticalAlign(Layout.Alignment.ALIGN_OPPOSITE)
            tOverlay.setHorizontalAlign(Layout.Alignment.ALIGN_NORMAL)
        }
        // TM - 6/6/2022 added this for left justify of following slide types
        if(slide.slideType in arrayOf(SlideType.LOCALSONG, SlideType.LOCALCREDITS)) {
            tOverlay.setHorizontalAlign(Layout.Alignment.ALIGN_NORMAL)
        }
        return tOverlay
    }

    private fun getFrontCoverOverlayText(origTitle: Boolean): TextOverlay {
        return if (origTitle) TextOverlay(getFrontCoverTitle()) else TextOverlay(slide.translatedContent)
    }

    internal fun getFrontCoverTitle(): String {
        return slide.content
                .replace("Title ideas:", "Title ideas:\n")
                .split("\n")
                .filterNot { it.isEmpty() }
                .elementAtOrNull(1).orEmpty().trim()                    // The 'first title idea' is the text we want to show.
                .let { "\\[[^\\]]*\\]?".toRegex().replace(it, "") }     // Drop any content within square brackets.
                .let { "[\\.\\!\\?].*".toRegex().replace(it, "") }      // remove everything after a .!? if there is one
                .let { "\\s+".toRegex().replace(it, " ") }              // Make all double spaces one space.
    }

}

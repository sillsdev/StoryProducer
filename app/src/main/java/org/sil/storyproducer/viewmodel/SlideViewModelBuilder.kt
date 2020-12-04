package org.sil.storyproducer.viewmodel

import android.text.Layout
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import java.text.SimpleDateFormat
import java.util.*

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
            SlideType.FRONTCOVER, SlideType.ENDPAGE -> 32
            SlideType.LOCALCREDITS -> 0 // Not used, but needs to be here to cov
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
package org.tyndalebt.storyproduceradv.viewmodel

import org.tyndalebt.storyproduceradv.tools.media.graphics.TextOverlay

data class SlideViewModel(
        val overlayText: TextOverlay?,
        val scriptureText: String,
        val scriptureReference: String
)
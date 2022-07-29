package org.tyndalebt.spadv.viewmodel

import org.tyndalebt.spadv.tools.media.graphics.TextOverlay

data class SlideViewModel(
        val overlayText: TextOverlay?,
        val scriptureText: String,
        val scriptureReference: String
)
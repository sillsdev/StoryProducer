package org.sil.storyproducer.viewmodel

import org.sil.storyproducer.tools.media.imagestory.graphics.TextOverlay

data class SlideViewModel(
        val overlayText: TextOverlay?,
        val scriptureText: String,
        val scriptureReference: String
)
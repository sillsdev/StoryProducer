package org.sil.storyproducer.model

import android.graphics.Rect

import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect

import java.io.File

/**
 * This class contains metadata pertinent to a given slide from a story template.
 */
data class TemplateSlide(
        val narrationAudio: File?,
        val image: File?,
        val imageDimensions: Rect,
        val kenBurnsEffect: KenBurnsEffect,
        val soundtrack: File?,
        val soundtrackVolume: Int)

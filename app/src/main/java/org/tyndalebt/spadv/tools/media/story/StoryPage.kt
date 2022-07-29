package org.tyndalebt.spadv.tools.media.story

import org.tyndalebt.spadv.model.SlideType
import org.tyndalebt.spadv.tools.media.graphics.KenBurnsEffect
import org.tyndalebt.spadv.tools.media.graphics.TextOverlay

/**
 * One StoryPage represents a unit of a story that will go into video format. Each StoryPage has
 * three parts: 1) image, 2) narration, and 3) Ken Burns effect. The narration dictates the length
 * of the page in the video, and the image and Ken Burns effect follow its queues.
 */
class StoryPage

(val imRelPath: String = "", val narrationAudioPath: String = "", private val mDuration: Long, val kenBurnsEffect: KenBurnsEffect? = null,
 val textOverlay: TextOverlay? = null, val soundtrackAudioPath: String = "", val soundtrackVolume: Float = 0.25f, val sType: SlideType = SlideType.NONE) {

    /**
     * Get the audio duration without any transition time.
     * @return duration in microseconds.
     */

    val audioDuration: Long
        get() = mDuration

    /**
     * Get the duration of a page. This duration includes audio transition time.
     */
    fun getDuration(audioTransition: Long): Long {
        //audio duration plus two half audio transition periods of silence on either end
        return audioDuration + audioTransition
    }
}
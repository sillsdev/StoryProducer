package org.sil.storyproducer.tools.media.story

import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect

import java.io.File

/**
 * One StoryPage represents a unit of a story that will go into video format. Each StoryPage has
 * three parts: 1) image, 2) narration, and 3) Ken Burns effect. The narration dictates the length
 * of the page in the video, and the image and Ken Burns effect follow its queues.
 */
class StoryPage
/**
 * Create page.
 * @param image picture for the video.
 * @param narrationAudioPath narration for the background of the video.
 * @param duration length of page in microseconds.
 * @param kbfx Ken Burns effect for the image.
 * @param text text for overlaying page.
 * @param soundtrackAudioPath soundtrack for page
 */
(val imRelPath: String = "", val narrationAudioPath: String = "", private val mDuration: Long, val kenBurnsEffect: KenBurnsEffect?,
 val text: String = "", val soundtrackAudioPath: String = "") {

    /**
     * Get the audio duration without any transition time.
     * @return duration in microseconds.
     */
    //FIXME
    //return MediaHelper.getAudioDuration(mNarrationAudio.getPath());
    val audioDuration: Long
        get() = mDuration

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    @JvmOverloads constructor(image: String, narrationAudio: String, kbfx: KenBurnsEffect, text: String = "") : this(image, narrationAudio, 0, kbfx, text) {}

    /**
     * Create page.
     * @param image picture for the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    @JvmOverloads constructor(image: String, duration: Long, kbfx: KenBurnsEffect? = null, text: String = "") : this(image, "", duration, kbfx, text) {}

    /**
     * Create page.
     * @param image picture for the video.
     * @param narrationAudio narration for the background of the video.
     * @param duration length of page in microseconds.
     * @param kbfx Ken Burns effect for the image.
     * @param text text for overlaying page.
     */
    private constructor(image: String, narrationAudio: String, duration: Long, kbfx: KenBurnsEffect?, text: String) : this(image, narrationAudio, duration, kbfx, text, "") {}

    /**
     * Get the duration of a page. This duration includes audio transition time.
     */
    fun getDuration(audioTransition: Long): Long {
        //audio duration plus two half audio transition periods of silence on either end
        return audioDuration + audioTransition
    }

    /**
     * Gets the duration this page is the only page showing.
     */
    fun getExclusiveDuration(audioTransition: Long, slideCrossFade: Long): Long {
        //page duration minus two half slide cross-fades on either end
        return getDuration(audioTransition) - slideCrossFade
    }

    /**
     * Gets the duration this page is visible, including overlap time with other slides.
     */
    fun getVisibleDuration(audioTransition: Long, slideCrossFade: Long): Long {
        //page duration plus two half slide cross-fades on either end
        return getDuration(audioTransition) + slideCrossFade
    }
}
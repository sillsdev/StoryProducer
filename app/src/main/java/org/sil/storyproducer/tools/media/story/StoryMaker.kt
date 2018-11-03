package org.sil.storyproducer.tools.media.story

import android.content.Context
import android.media.MediaFormat
import android.util.Log

import org.sil.storyproducer.tools.media.pipe.PipedAudioConcatenator
import org.sil.storyproducer.tools.media.pipe.PipedAudioMixer
import org.sil.storyproducer.tools.media.pipe.PipedMediaEncoder
import org.sil.storyproducer.tools.media.pipe.PipedMediaMuxer
import org.sil.storyproducer.tools.media.pipe.PipedVideoSurfaceEncoder

import java.io.Closeable
import java.io.File

/**
 * StoryMaker handles all the brunt work of constructing a media pipeline for a given set of StoryPages.
 */
class StoryMaker
/**
 * Create StoryMaker.
 * @param output output video file.
 * @param outputFormat the format of the output media file
 * (from [android.media.MediaMuxer.OutputFormat]).
 * @param videoFormat desired output video format.
 * @param audioFormat desired output audio format.
 * @param pages pages of this story.
 * @param audioTransitionUs transition duration, in microseconds, between narration segments.
 * Note: this helps drive length of video.
 * @param slideCrossFadeUs cross-fade duration, in microseconds, between page images.
 */
(private val context: Context, private val mOutputFile: File, private val mOutputFormat: Int, private val mVideoFormat: MediaFormat?, private val mAudioFormat: MediaFormat,
 private val mPages: Array<StoryPage>, private val mAudioTransitionUs: Long, private val mSlideCrossFadeUs: Long) : Closeable {

    private var mSoundtrackVolumeModifier = 0.25f

    private val mSampleRate: Int
    private val mChannelCount: Int

    val storyDuration: Long

    private var mMuxer: PipedMediaMuxer? = null
    var isDone = false
        private set

    val progress: Double
        get() {
            if (isDone) {
                return 1.0
            } else if (mMuxer != null) {
                val audioProgress = mMuxer!!.audioProgress
                val videoProgress = mMuxer!!.videoProgress

                val minProgress = Math.min(audioProgress, videoProgress)

                return minProgress / storyDuration.toDouble()
            }
            return 0.0
        }

    val audioProgress: Double
        get() {
            if (mMuxer != null) {
                val audioProgress = mMuxer!!.audioProgress
                return audioProgress / storyDuration.toDouble()
            }
            return 0.0
        }

    val videoProgress: Double
        get() {
            if (mMuxer != null) {
                val videoProgress = mMuxer!!.videoProgress
                return videoProgress / storyDuration.toDouble()
            }
            return 0.0
        }

    init {

        mSampleRate = mAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        mChannelCount = mAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        storyDuration = getStoryDuration(mPages, mAudioTransitionUs)
    }

    /**
     * Set the relative volume of the soundtrack to the narration for the video.
     * @param modifier between 0 (silent) and 1 (original volume)
     */
    fun setSoundtrackVolumeModifier(modifier: Float) {
        mSoundtrackVolumeModifier = modifier
    }

    /**
     * Set StoryMaker in motion. It is advisable to run this method from a separate thread.
     * @return whether the video creation process finished.
     */
    fun churn(): Boolean {
        if (isDone) {
            Log.e(TAG, "StoryMaker already finished!")
        }

        val soundtrackConcatenator = PipedAudioConcatenator(context,0, mSampleRate, mChannelCount)
        soundtrackConcatenator.setFadeOut(SOUNDTRACK_FADE_OUT_US)
        val narrationConcatenator = PipedAudioConcatenator(context, mAudioTransitionUs, mSampleRate, mChannelCount)
        val audioMixer = PipedAudioMixer()
        val audioEncoder = PipedMediaEncoder(mAudioFormat)
        var videoDrawer: StoryFrameDrawer? = null
        var videoEncoder: PipedVideoSurfaceEncoder? = null
        if (mVideoFormat != null) {
            videoDrawer = StoryFrameDrawer(context, mVideoFormat, mPages, mAudioTransitionUs, mSlideCrossFadeUs)
            videoEncoder = PipedVideoSurfaceEncoder()
        }
        //kill the generated file first - if not, it will make all files created at least as big
        //as the last one.
        mOutputFile.delete()
        mMuxer = PipedMediaMuxer(mOutputFile.absolutePath, mOutputFormat)

        var success = false

        try {
            mMuxer!!.addSource(audioEncoder)

            var soundtrackDuration: Long = 0
            var lastSoundtrack: String = ""
            for (page in mPages) {
                val narration = page.narrationAudioPath
                val audioDuration = page.audioDuration

                val soundtrack = page.soundtrackAudioPath
                val pageDuration = page.getDuration(mAudioTransitionUs)

                //If we encounter a new soundtrack, stop the current one and start the new one.
                //Otherwise, continue playing last soundtrack.
                if (soundtrack == "" || soundtrack != lastSoundtrack) {
                    if (lastSoundtrack != "") {
                        soundtrackConcatenator.addSourcePath(lastSoundtrack, soundtrackDuration)
                    } else if (soundtrackDuration > 0) {
                        soundtrackConcatenator.addSource(null, soundtrackDuration)
                    }

                    lastSoundtrack = soundtrack
                    soundtrackDuration = pageDuration
                } else {
                    soundtrackDuration += pageDuration
                }

                narrationConcatenator.addSourcePath(narration, audioDuration)
            }

            //Add last soundtrack
            if (lastSoundtrack != "") {
                soundtrackConcatenator.addLoopingSourcePath(lastSoundtrack, soundtrackDuration)
            }

            //Add soundtrack only if there is one!
            if(soundtrackConcatenator.anyNonNull()) {
                audioEncoder.addSource(audioMixer)
                audioMixer.addSource(narrationConcatenator)
                audioMixer.addSource(soundtrackConcatenator, mSoundtrackVolumeModifier)
            } else {
                //no mixing needed - bypass.
                audioEncoder.addSource(narrationConcatenator)
            }

            if (mVideoFormat != null) {
                mMuxer!!.addSource(videoEncoder!!)

                videoEncoder.addSource(videoDrawer!!)
            }
            success = mMuxer!!.crunch()
            Log.i(TAG, "Video saved to $mOutputFile")
        } catch (e: Exception) {
            Log.e(TAG, "Error in story making", e)
        } finally {
            //Everything should be closed automatically, but close everything just in case.
            soundtrackConcatenator.close()
            narrationConcatenator.close()
            audioMixer.close()
            audioEncoder.close()
            if (mVideoFormat != null) {
                videoDrawer!!.close()
                videoEncoder!!.close()
            }
            mMuxer!!.close()
        }

        isDone = true

        return success
    }

    override fun close() {
        if (mMuxer != null) {
            Log.i(TAG, "Closing media pipeline. Subsequent logged errors may not be cause for concern.")
            mMuxer!!.close()
        }
        isDone = true
    }

    companion object {
        private val TAG = "StoryMaker"
        private val SOUNDTRACK_FADE_OUT_US: Long = 1000000

        /**
         * Get the expected duration, in microseconds, of the produced video.
         * This value should be accurate to a few milliseconds for arbitrarily long stories.
         * @param pages pages of this story.
         * @param audioTransitionUs transition duration, in microseconds, between narration segments.
         * @return expected duration of the produced video in microseconds.
         */
        fun getStoryDuration(pages: Array<StoryPage>, audioTransitionUs: Long): Long {
            var durationUs: Long = 0

            for (page in pages) {
                durationUs += page.getDuration(audioTransitionUs)
            }

            return durationUs
        }
    }
}

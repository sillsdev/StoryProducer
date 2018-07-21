package org.sil.storyproducer.tools.media.story

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import org.sil.storyproducer.model.PROJECT_DIR
import org.sil.storyproducer.model.VIDEO_DIR

import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.media.graphics.KenBurnsEffect
import org.sil.storyproducer.tools.media.graphics.TextOverlay
import org.sil.storyproducer.tools.media.graphics.overlayJPEG

import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * AutoStoryMaker is a layer of abstraction above [StoryMaker] that handles all of the
 * parameters for StoryMaker according to some defaults, structure of projects/templates, and
 * minimal customization.
 */
class AutoStoryMaker(private val context: Context) : Thread(), Closeable {

    // size of a frame, in pixels
    // first size isn't great quality, but runs faster
    private var mWidth = 320
    private var mHeight = 240

    private var mOutputExt = ".mp4"
    private var videoRelPath: String = VIDEO_DIR + "/" +
            Workspace.activeStory.title.replace(' ', '_') + "_" + mWidth + "x" + mHeight + mOutputExt
    // bits per second for video
    private var videoTempFile: File = File(context.filesDir,"temp$mOutputExt")
    private var mVideoBitRate: Int = 0

    private var mIncludeBackgroundMusic = true
    private var mIncludePictures = true
    private var mIncludeText = false
    private var mIncludeKBFX = true

    private var mLogProgress = false

    private var mStoryMaker: StoryMaker? = null

    val isDone: Boolean
        get() = mStoryMaker != null && mStoryMaker!!.isDone

    val progress: Double
        get() = if (mStoryMaker == null) {
            0.0
        } else {
            mStoryMaker!!.progress
        }

    init {
        setResolution(mWidth, mHeight)
    }

    fun setOutputFile(relPath: String) {
        videoRelPath = relPath
    }

    fun setResolution(width: Int, height: Int) {
        mWidth = width
        mHeight = height

        val pixelRate = mWidth * mHeight * VIDEO_FRAME_RATE
        mVideoBitRate = (pixelRate.toFloat() * MOTION_FACTOR.toFloat() * KUSH_GAUGE_CONSTANT).toInt()
    }

    fun toggleBackgroundMusic(includeBackgroundMusic: Boolean) {
        mIncludeBackgroundMusic = includeBackgroundMusic
    }

    fun togglePictures(includePictures: Boolean) {
        mIncludePictures = includePictures
    }

    fun toggleText(includeText: Boolean) {
        mIncludeText = includeText
    }

    fun toggleKenBurns(includeKBFX: Boolean) {
        mIncludeKBFX = includeKBFX
    }

    /**
     * Set whether progress is periodically logged in console. Default is false (no console logging).
     * @param logProgress whether progress should be logged in console.
     */
    fun toggleLogProgress(logProgress: Boolean) {
        mLogProgress = logProgress
    }

    override fun start() {
        val outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4

        //TODO switch for smartphone and dumb phone.
        //The dumbphone format is:
        // video: H263, 176x144, frame rate = 15
        // audio: AMR (samr), mono, 8000 Hz, 32 bits per sample

        val audioFormat = generateAudioFormat()
        val videoFormat = generateVideoFormat()
        val pages = generatePages() ?: return

        //If pages weren't generated, exit.

        mStoryMaker = StoryMaker(context, videoTempFile, outputFormat, videoFormat, audioFormat,
                pages, AUDIO_TRANSITION_US, SLIDE_CROSS_FADE_US)

        watchProgress()

        super.start()
    }

    override fun run() {
        var duration = -System.currentTimeMillis()

        Log.i(TAG, "Starting video creation")
        val success = mStoryMaker!!.churn()

        if (success) {
            Log.v(TAG, "Moving completed video to " + videoRelPath)
            copyToStoryPath(context,Uri.fromFile(videoTempFile),videoRelPath)
        } else {
            Log.w(TAG, "Deleting incomplete temporary video")
            videoTempFile.delete()
        }

        duration += System.currentTimeMillis()

        Log.i(TAG, "Stopped making story after "
                + MediaHelper.getDecimal(duration / 1000.toDouble()) + " seconds")
    }



    private fun generateVideoFormat(): MediaFormat? {
        //If no video component, use null format.
        if (!mIncludePictures && !mIncludeText) {
            return null
        }

        val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoBitRate)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL)

        return videoFormat
    }

    private fun generatePages(): Array<StoryPage>? {
        //TODO: add hymn to count
        val pages: MutableList<StoryPage> = mutableListOf()

        val widthToHeight = mWidth / mHeight.toDouble()

        val tempTitle = "$PROJECT_DIR/temptitle.jpg"
        //TODO enable better title selection.
        val titleOverlay = TextOverlay(Workspace.activeStory.slides[0].translatedContent)
        titleOverlay.setFontSize(TITLE_FONT_SIZE)

        var titleRelPath = Workspace.activeStory.slides[0].imageFile

        try {
            overlayJPEG(context, Workspace.activeStory.slides[0].imageFile, tempTitle, titleOverlay)
            titleRelPath = tempTitle
        } catch (e: IOException) {
            Log.w(TAG, "Failed to create overlayed title slide!")
        }

        var lastSoundtrack = ""
        var iSlide = 0
        var image: String = ""
        //don't use the last image - it's the copyright.
        while (iSlide < (Workspace.activeStory.slides.size - 1)) {
            val slide = Workspace.activeStory.slides[iSlide]
            if (mIncludePictures) {
                if (iSlide == 0) {
                    image = titleRelPath
                } else {
                    image = slide.imageFile
                }
            }else image = ""
            var audio = slide.chosenDramatizationFile
            //fallback to draft audio
            if (audio == "") {
                audio = slide.chosenDraftFile
            }
            //fallback to LWC audio
            if (audio == "") {
                audio = slide.narrationFile
            }
            //error
            if (audio == "") {
                error("Audio missing for slide " + (iSlide + 1))
                return null
            }

            var soundtrack = slide.musicFile
            if (mIncludeBackgroundMusic) {

                if (soundtrack == "") {
                    //Try not to leave nulls in so null may be reserved for no soundtrack.
                    soundtrack = lastSoundtrack
                } else if (soundtrack == "") {
                    error("Soundtrack missing from template: " + soundtrack)
                }

                lastSoundtrack = soundtrack
            }

            var kbfx: KenBurnsEffect? = null
            if (mIncludePictures && mIncludeKBFX && iSlide != 0) {
                kbfx = KenBurnsEffect.fromSlide(slide)
            }

            var text = ""
            if (mIncludeText) {
                text = slide.translatedContent
            }

            val duration = MediaHelper.getAudioDuration(context, getStoryUri(audio))

            pages.add(StoryPage(image, audio, duration, kbfx, text, soundtrack))
            iSlide++
        }

        pages.add(StoryPage(Workspace.activeStory.slides.last().imageFile,COPYRIGHT_SLIDE_US))

        //TODO: add hymn

        return pages.toTypedArray()
    }

    private fun watchProgress() {
        val watcher = Thread(Runnable {
            while (!mStoryMaker!!.isDone) {
                val progress = mStoryMaker!!.progress
                val audioProgress = mStoryMaker!!.audioProgress
                val videoProgress = mStoryMaker!!.videoProgress
                Log.i(TAG, "StoryMaker progress: " + MediaHelper.getDecimal(progress * 100) + "% "
                        + "(audio " + MediaHelper.getDecimal(audioProgress * 100) + "% "
                        + " and video " + MediaHelper.getDecimal(videoProgress * 100) + "%)")
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        })

        if (mLogProgress) {
            watcher.start()
        }
    }

    private fun error(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun close() {
        if (mStoryMaker != null) {
            mStoryMaker!!.close()
        }
    }

    companion object {
        private val TAG = "AutoStoryMaker"
        //    private int mWidth = 1280;
        //    private int mHeight = 720;

        private val TITLE_FONT_SIZE = 20

        private val SLIDE_CROSS_FADE_US: Long = 3000000
        private val AUDIO_TRANSITION_US: Long = 500000

        private val COPYRIGHT_SLIDE_US: Long = 3000000

        // parameters for the video encoder
        private val VIDEO_MIME_TYPE = "video/avc"    // H.264 Advanced Video Coding
        private val VIDEO_FRAME_RATE = 30               // 30fps
        private val VIDEO_IFRAME_INTERVAL = 1           // 1 second between I-frames

        // using Kush Gauge for video bit rate
        private val MOTION_FACTOR = 2                   // 1, 2, or 4
        private val KUSH_GAUGE_CONSTANT = 0.07f

        // parameters for the audio encoder
        private val AUDIO_MIME_TYPE = "audio/mp4a-latm" //MediaFormat.MIMETYPE_AUDIO_AAC;
        private val AUDIO_SAMPLE_RATE = 48000
        private val AUDIO_CHANNEL_COUNT = 1
        private val AUDIO_BIT_RATE = 64000

        fun generateAudioFormat(): MediaFormat {
            val audioFormat = MediaHelper.createFormat(AUDIO_MIME_TYPE)
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, AUDIO_SAMPLE_RATE)
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT)

            return audioFormat
        }
    }
}

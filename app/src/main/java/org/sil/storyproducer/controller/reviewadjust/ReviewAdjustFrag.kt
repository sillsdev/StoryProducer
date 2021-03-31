package org.sil.storyproducer.controller.reviewadjust

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.filmstory.*
import java.io.File
import kotlin.math.max

class ReviewAdjustFrag : MultiRecordFrag() {

    class Clip {
        var duration = 0
        var startPosition = 0

        var visualWidth = 0
        var visualStartPosition = 0

        var exists = true
    }

    lateinit var currentSlide : Slide
    private lateinit var trackImageView : ImageView
    lateinit var videoView : VideoView
    private lateinit var playPauseButton : ImageButton
    private lateinit var leftNarrationMove : ImageButton
    private lateinit var rightNarrationMove : ImageButton

    private var canvasWidth = 1
    private var canvasHeight = 1
    private var tempo = 1.0

    var narrationClip = Clip()
    var videoClip = Clip()
    private var maxDuration = 0

    var playing = false
    var otherAudioPlaying = false
    var currentPlaybackPosition = 0

    private lateinit var waveform: Bitmap

    /**
     * There are lots of issues with the ViewPager that loads in these slides. One of them
     * is that it loads multiple fragments before they actually get added to the view. So,
     * we have multiple of these fragments on swiping left and right. So we need to add a
     * check to make sure that the TrackView is ready to be used.
     */
    private var isTrackViewInitialized = false

    companion object {
        private const val STEP_AMOUNT = 125
    }

    private var playbackThread : HandlerThread? = null
    private var playbackHandler : Handler? = null
    private var playbackUpdater = object : Runnable {
        //this updates the seekbar
        override fun run() {
            if(playing) {
                val isVideoLonger = videoClip.duration > narrationClip.duration
                if (currentPlaybackPosition + currentSlide.startTime >= currentSlide.endTime) {
                    if(isVideoLonger) {
                        stop()
                    } else {
                        if(videoView.isPlaying) {
                            videoView.pause()
                        }
                    }
                } else {
                    if (!otherAudioPlaying) {
                        if (currentPlaybackPosition >= narrationClip.startPosition) {
                            otherAudio.playAudio()
                            otherAudioPlaying = true
                        }
                    }

                }

                if(!isVideoLonger && currentPlaybackPosition >= narrationClip.duration) {
                    stop()
                }

                if(isVideoLonger) {
                    currentPlaybackPosition = videoView.currentPosition - currentSlide.startTime
                } else {
                    currentPlaybackPosition = otherAudio.currentPosition
                }

                activity!!.runOnUiThread {
                    updateTrackPositions()
                }
            }

            playbackHandler!!.postDelayed(this, 15)
        }
    }

    private val otherAudio = AudioPlayer()

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?): View? {
        return inflater.inflate(R.layout.fragment_review_adjust, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(Workspace.activeStory.isApproved){
            val lock = view.findViewById<RelativeLayout>(R.id.lock_overlay)
            lock.visibility = View.INVISIBLE
        }
        setup(view)
    }

    override fun onStop() {
        super.onStop()
        otherAudio.release()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setup(view: View) {
        currentSlide = Workspace.activeStory.slides[slideNum]

        val dir = context!!.filesDir
        val audioFile = File(dir, "audio${FilmStoryProducer.MP4_EXT}")
        audioFile.createNewFile()
        copyM4aStreamToMp4File(getStoryChildInputStream(context!!, slide.getFinalFileString()), audioFile)
        tempo = calculateSlideTempo(currentSlide, audioFile)
        audioFile.delete()

        trackImageView = view.findViewById(R.id.film_studio_audio_tracks)
        trackImageView.post {
            canvasWidth = trackImageView.width
            canvasHeight = trackImageView.height

            // Get the real lengths of the audio and video clips
            videoClip.duration = currentSlide.endTime - currentSlide.startTime
            if(videoClip.duration == 0) {
                videoClip.exists = false
            }
            // The video clip will always have the full width of the canvas
            videoClip.visualWidth = canvasWidth

            narrationClip.visualWidth = scaleToVisualSpace(narrationClip.duration)
            narrationClip.visualStartPosition = scaleToVisualSpace(currentSlide.audioPosition)

            maxDuration = max(narrationClip.duration, videoClip.duration)

            isTrackViewInitialized = true

            updateTrackPositions()
        }
        try {
            val aFile = File(context!!.getExternalFilesDir("ReviewPhase"), "audio.mp4")
            aFile.createNewFile()
            copyM4aStreamToMp4File(getStoryChildInputStream(context!!, slide.getFinalFileString()), aFile)
            // Make sure to scale based on the tempo
            narrationClip.duration = (getMp4Length(aFile) / tempo).toInt()

            if(narrationClip.duration == 0) {
                narrationClip.exists = false
            }

            val wfFile = File(context!!.getExternalFilesDir("ReviewPhase"), "wf.png")
            wfFile.createNewFile()
            // If narrationClip doesn't exist, then wfFile will be empty and will show nothing
            if(narrationClip.exists) {
                generateWaveformImage(aFile, wfFile)
            }
            waveform = BitmapFactory.decodeFile(wfFile.absolutePath)
            aFile.delete()
        } catch (e : Exception) {
            // Couldn't get the narration duration (likely it doesn't exist)
            // Set the clip as non-existent
            narrationClip.exists = false
        }
        narrationClip.startPosition = currentSlide.audioPosition

        videoView = view.findViewById(R.id.film_studio_video_view)
        videoView.setVideoURI(getStoryUri(Workspace.activeStory.fullVideo))
        videoView.setOnPreparedListener { mp ->
            mp.setVolume(0f, 0f)
        }
        videoView.seekTo(currentSlide.startTime)

        otherAudio.setSource(context!!, getStoryUri(Workspace.activePhase.getReferenceAudioFile(slideNum))!!)
        otherAudio.setTempo(tempo)

        leftNarrationMove = view.findViewById(R.id.film_studio_narration_move_left)
        leftNarrationMove.setOnTouchListener(RepeatListener(400, 100, View.OnClickListener {
            if(!playing && narrationClip.startPosition - STEP_AMOUNT >= 0) {
                narrationClip.startPosition -= STEP_AMOUNT
                currentSlide.audioPosition = narrationClip.startPosition
                narrationClip.visualStartPosition = scaleToVisualSpace(narrationClip.startPosition)
                updateTrackPositions()
            }
        }))

        rightNarrationMove = view.findViewById(R.id.film_studio_narration_move_right)
        rightNarrationMove.setOnTouchListener(RepeatListener(400, 100, View.OnClickListener {
            val maxDuration = max(narrationClip.duration, videoClip.duration)
            if(!playing && narrationClip.startPosition + narrationClip.duration + STEP_AMOUNT <= maxDuration) {
                narrationClip.startPosition += STEP_AMOUNT
                currentSlide.audioPosition = narrationClip.startPosition
                narrationClip.visualStartPosition = scaleToVisualSpace(narrationClip.startPosition)
                updateTrackPositions()
            }
        }))

        playPauseButton = view.findViewById(R.id.video_player_play_pause_button)
        playPauseButton.setOnClickListener {
            if(playing) {
                stop()
            } else {
                play()
            }
        }

        // If there is no audio, then disable all the elements.
        if(!narrationClip.exists) {
            // Disable Buttons
            playPauseButton.isEnabled = false
            rightNarrationMove.isEnabled = false
            leftNarrationMove.isEnabled = false

            playPauseButton.alpha = 0.4f
            rightNarrationMove.alpha = 0.4f
            leftNarrationMove.alpha = 0.4f
        }
    }

    override fun onResume() {
        super.onResume()

        playbackThread = HandlerThread("Review/Adjust playback updater")
        playbackThread!!.start()
        playbackHandler = Handler(playbackThread!!.looper)
        playbackHandler!!.post(playbackUpdater)
    }

    override fun onPause() {
        super.onPause()

        // If there actually is a narration, then pause it.
        if(narrationClip.exists) {
            stop()
        }
        playbackHandler!!.removeCallbacks(playbackUpdater)
        playbackThread!!.quit()
    }

    override fun stopPlayBack() {
        stop()
    }

    private fun play() {
        activity!!.runOnUiThread {
            playPauseButton.setImageResource(R.drawable.ic_stop_white_48dp)

            // Disable the left and right buttons
            leftNarrationMove.alpha = 0.4f
            rightNarrationMove.alpha = 0.4f
        }
        playing = true
        videoView.start()
    }

    private fun stop() {
        playing = false
        otherAudioPlaying = false
        currentPlaybackPosition = 0
        videoView.pause()
        videoView.seekTo(currentSlide.startTime)
        otherAudio.pauseAudio()
        otherAudio.seekTo(0)
        activity!!.runOnUiThread {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_48dp)
            updateTrackPositions()

            // Enable the left and right buttons
            leftNarrationMove.alpha = 1f
            rightNarrationMove.alpha = 1f
        }
    }

    private fun scaleToVisualSpace(realPosition: Int): Int {
        val maxDuration = max(narrationClip.duration, videoClip.duration)
        val ratio : Double = if(maxDuration == 0) {
            1.0
        } else {
            canvasWidth.toDouble() / maxDuration.toDouble()
        }

        return (ratio * realPosition).toInt()
    }

    private fun updateTrackPositions() {
        if(context == null) return

        if(isTrackViewInitialized) {
            val content = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(content)
            val paint = Paint()
            val scaleMetrics = resources.displayMetrics.density
            paint.textSize = 16 * scaleMetrics

            paint.color = Color.LTGRAY
            val imageLocation = RectF(narrationClip.visualStartPosition.toFloat(), 0.0f,
                    narrationClip.visualWidth.toFloat() + narrationClip.visualStartPosition,
                    canvasHeight / 2.0f - 10)

            if(::waveform.isInitialized) {
                canvas.drawBitmap(waveform, null, imageLocation, paint)
            }
            paint.color = Color.WHITE

            paint.color = if (videoClip.exists) {
                Color.GRAY
            } else {
                Color.BLACK
            }
            val clipDurationBoxTop = canvasHeight / 2 + 10
            canvas.drawRect(Rect(0, clipDurationBoxTop, canvasWidth, canvasHeight), paint)
            paint.color = Color.WHITE

            if(narrationClip.exists) {
                val clipDurationTextSize = paint.measureText(getString(R.string.video_duration))
                canvas.drawText(getString(R.string.video_duration), canvasWidth / 2.0f - clipDurationTextSize / 2, (clipDurationBoxTop / 2.0f) + clipDurationBoxTop, paint)

                // Draw Playback Line
                paint.color = Color.RED
                val tickPosition = scaleToVisualSpace(currentPlaybackPosition)
                val tickWidth = 5
                canvas.drawRect(Rect(tickPosition, 0, tickPosition + tickWidth, canvasHeight), paint)
            } else {
                val clipDurationTextSize = paint.measureText(getString(R.string.no_audio_exists))
                canvas.drawText(getString(R.string.no_audio_exists), canvasWidth / 2.0f - clipDurationTextSize / 2, (clipDurationBoxTop / 2.0f) + clipDurationBoxTop, paint)
            }

            trackImageView.setImageBitmap(content)
        }
    }
}

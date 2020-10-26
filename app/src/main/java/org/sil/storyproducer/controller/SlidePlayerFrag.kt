package org.sil.storyproducer.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import org.sil.storyproducer.film.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.media.MediaHelper
import kotlin.math.max

class SlidePlayerFrag : StoryPlayerFrag() {

    private var slideView : ImageView? = null
    private var seekBar : SeekBar? = null

    private var slidePlayerThread : HandlerThread? = null
    private var slidePlayerHandler : Handler? = null
    private var slidePlayerUpdater = object : Runnable {
        override fun run() {
            if(playing && audioPlayer.isAudioPrepared) {
                seekBar!!.progress = getPosition()
            }

            slidePlayerHandler!!.postDelayed(this, 33)
        }
    }

    private var slideEndTimes : MutableList<Int>? = null
    private var currentSlideIndex : Int = 0
    private var lastSlideIndex : Int = -1
    private var storySlidePosition : Int = 0
    private var firstSlideNum : Int = 0

    private var applicationContext : Context? = null

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?): View? {
        return inflater.inflate(R.layout.fragment_slide_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slideView = view.findViewById(R.id.slide_player_image_view)
        slideNumber = view.findViewById(R.id.slide_player_slide_number)

        seekBar = view.findViewById(R.id.slide_player_seekbar)
        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                seek(seekBar!!.progress)
                audioPlayer.playAudio()
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(bar : SeekBar?, progress : Int, fromUser : Boolean) {
                if(fromUser) {
                    seekBar!!.progress = progress
                }
            }
        })

        // Set the play/pause button to play or pause the animation
        playPauseButton = view.findViewById(R.id.slide_player_playback_button)
        playPauseButton!!.setOnClickListener {
            if(playing) {
                stop()
            } else {
                play()
                if(Workspace.activePhase.getReferenceAudioFile(storySlidePosition) == "") {
                    Toast.makeText(context!!, "No narration found!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setSlideRange(startSlide, startSlide+slideRange)
    }

    override fun onResume() {
        super.onResume()

        // Make sure that the thread's resources have been cleared. If not, clear them
        if(slidePlayerThread != null) {
            slidePlayerThread!!.quit()
            slidePlayerThread = null
        }

        // Create the slide player thread to update the slides
        slidePlayerThread = HandlerThread("Slide Player")
        slidePlayerThread!!.start()
        slidePlayerHandler = Handler(slidePlayerThread!!.looper)
        slidePlayerHandler!!.post(slidePlayerUpdater)
    }

    override fun onPause() {
        super.onPause()

        slidePlayerHandler!!.removeCallbacks(slidePlayerUpdater)
        // Free the slide player thread's resources
        slidePlayerThread!!.quit()
        slidePlayerThread = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        applicationContext = context!!.applicationContext

        val audioFile = Workspace.activePhase.getReferenceAudioFile(storySlidePosition)
        if(audioFile != "") {
            audioPlayer.setStorySource(applicationContext!!, Workspace.activePhase.getReferenceAudioFile(storySlidePosition))
        }
        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            if(currentSlideIndex < slideEndTimes!!.size - 1) {
                audioPlayer.setStorySource(applicationContext!!, Workspace.activeStory.slides[++storySlidePosition].narrationFile)
                audioPlayer.playAudio()
                currentSlideIndex++
                setSlidePic(storySlidePosition)
                slideNumber!!.text = storySlidePosition.toString()
            } else {
                if(playing) {
                    stop()
                }
            }
        })
    }

    override fun seek(milliseconds: Int) {
        setCurrentSlide(milliseconds)
        setAudioPosition(milliseconds)
        setSlidePic(storySlidePosition)
        seekBar!!.progress = milliseconds
        slideNumber!!.text = storySlidePosition.toString()
    }

    override fun getPosition() : Int {
        val previousEndTime = if(currentSlideIndex == 0) 0 else slideEndTimes!![currentSlideIndex - 1]
        return previousEndTime + audioPlayer.currentPosition
    }

    override fun getDuration() : Int {
        return slideEndTimes!!.last()
    }

    override fun saveLog(pauseTime : Long) {
        val durationPlayed = (pauseTime - startTime)
        if (durationPlayed<100 || startTime<0 || pauseTime <0){
            return
        }
        when (phaseType) {
            PhaseType.LEARN -> {
                val mResources = context!!.resources
                var ret = "Playback"

                ret += if (firstSlideNum == currentSlideIndex) {
                    mResources.getQuantityString(
                            R.plurals.logging_numSlides,
                            1
                    ) + " " + (firstSlideNum)
                } else {
                    mResources.getQuantityString(
                            R.plurals.logging_numSlides,
                            2
                    ) + " " + (firstSlideNum) + "-" + (currentSlideIndex)
                }
                // format duration:
                val secUnit = mResources.getString(R.string.SECONDS_ABBREVIATION)
                val minUnit = mResources.getString(R.string.MINUTES_ABBREVIATION)
                if (durationPlayed < 1000) {
                    ret += " (<1 $secUnit)"
                } else {
                    val roundedSecs = (durationPlayed / 1000.0 + 0.5).toInt()
                    val mins = roundedSecs / 60
                    var minString = ""
                    if (mins > 0) {
                        minString = "$mins $minUnit "
                    }
                    ret += " (" + minString + roundedSecs % 60 + " " + secUnit + ")"
                }
                org.sil.storyproducer.model.logging.saveLog(ret, firstSlideNum, currentSlideIndex)
            }
            PhaseType.DRAFT -> {
                org.sil.storyproducer.model.logging.saveLog(getString(R.string.LWC_PLAYBACK), firstSlideNum, currentSlideIndex)
            }
            PhaseType.COMMUNITY_CHECK -> {
                org.sil.storyproducer.model.logging.saveLog(getString(R.string.DRAFT_PLAYBACK), firstSlideNum, currentSlideIndex)
            }
            else -> {
                // Do nothing!
            }
        }
    }

    private fun setSlideRange(begin : Int, end : Int) {
        firstSlideNum = begin
        slideEndTimes = mutableListOf()
        var storySlides = mutableListOf<Slide>()

        // Get the slides we care about (only the actual slide content)
        // This is necessary to remove the extra slides, because they don't come in order
        // necessarily
        for(slide in Workspace.activeStory.slides) {
            if(slide.slideType in arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE)) {
                storySlides.add(slide)
            }
        }

        var endTime = 0
        for(i in begin until end) {
            if(applicationContext != null && i < storySlides.size) {
                var slideDuration = MediaHelper.getAudioDuration(
                        applicationContext!!,
                        getStoryUri(Workspace.activePhase.getReferenceAudioFile(storySlides[i]))!!
                ).toInt() / 1000

                endTime += slideDuration
                slideEndTimes!!.add(endTime)
            }
        }

        if(slideEndTimes!!.size == 0) {
            slideEndTimes!!.add(0)
        }

        seekBar!!.max = slideEndTimes!!.last()
        seek(0)
    }

    private fun setCurrentSlide(milliseconds : Int) {
        currentSlideIndex = 0
        while(currentSlideIndex < slideEndTimes!!.size - 1 && slideEndTimes!![currentSlideIndex] < milliseconds) {
            currentSlideIndex++
        }
        storySlidePosition = currentSlideIndex + firstSlideNum
        lastSlideIndex = currentSlideIndex - 1
    }

    private fun setAudioPosition(milliseconds : Int) {
        val slideBeginTime = if(currentSlideIndex == 0) 0 else slideEndTimes!![currentSlideIndex - 1]
        val slideAudioOffset = milliseconds - slideBeginTime
        val audioFile = Workspace.activePhase.getReferenceAudioFile(storySlidePosition)
        if(audioFile != "") {
            audioPlayer.setStorySource(applicationContext!!, Workspace.activePhase.getReferenceAudioFile(storySlidePosition))
            audioPlayer.currentPosition = slideAudioOffset
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideNum The slide number to grab the picture from the files.
     */
    private fun setSlidePic(slideNum: Int) {
        val downSample = 2
        var slidePicture: Bitmap = getStoryImage(context!!, slideNum, downSample)

        // Scale down image to not crash phone from memory error from displaying too large an image
        // Get the height of the phone.
        val phoneProperties = this.resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()
        val width = phoneProperties.widthPixels

        // scale bitmap
        slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)

        // draw the text overlay
        slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
        val canvas = Canvas(slidePicture)
        // only show the untranslated title in the Learn phase.
        val tOverlay = if (Workspace.activePhase.phaseType == PhaseType.LEARN)
            Workspace.activeStory.slides[slideNum].getOverlayText(false, true)
        else Workspace.activeStory.slides[slideNum].getOverlayText(false, false)
        // if overlay is null, it will not write the text.
        tOverlay?.setPadding(max(20, 20 + (canvas.width - phoneProperties.widthPixels) / 2))
        tOverlay?.draw(canvas)

        // Set the height of the image view
        slideView!!.requestLayout()

        slideView!!.setImageBitmap(slidePicture)
    }
}
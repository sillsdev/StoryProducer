package org.sil.storyproducer.controller

import android.media.MediaPlayer
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.Guideline
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.viewmodel.SlideViewModel
import org.sil.storyproducer.viewmodel.SlideViewModelBuilder
import timber.log.Timber
import java.util.*

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class SlidePhaseFrag : androidx.fragment.app.Fragment() {
    protected var rootView: View? = null

    protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    protected var referencePlayButton: ImageButton? = null
    protected var refPlaybackSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var refPlaybackProgress = 0
    private var refPlaybackDuration = 0
    private var wasAudioPlaying = false


    protected var slideNum: Int = 0 //gets overwritten
    protected lateinit var slide: Slide
    protected lateinit var viewModel: SlideViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            slideNum = this.arguments!!.getInt(SLIDE_NUM)
            viewModel = SlideViewModelBuilder(Workspace.activeStory.slides[slideNum]).build()
            slide = Workspace.activeStory.slides[slideNum]
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        rootView = inflater.inflate(R.layout.fragment_slide, container, false)

        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item = menu.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_mic_white_48dp)
    }


    override fun onResume() {
        super.onResume()

        referenceAudioPlayer = AudioPlayer()
        val refAudioFile = Workspace.activePhase.getReferenceAudioFile(slideNum)
        if (refAudioFile.isNotEmpty())
            referenceAudioPlayer.setStorySource(context!!, refAudioFile)

        referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
            referencePlayButton!!.contentDescription = getString(R.string.rec_toolbar_play_recording_button)
            referenceAudioPlayer.stopAudio()
        })

        refPlaybackSeekBar = rootView!!.findViewById(R.id.videoSeekBar)
        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                activity!!.runOnUiThread{
                    refPlaybackProgress = referenceAudioPlayer.currentPosition
                    refPlaybackSeekBar?.progress = refPlaybackProgress
                }
            }
        },0,33)

        setSeekBarListener()
    }

    private fun setSeekBarListener() {
        refPlaybackDuration = referenceAudioPlayer.audioDurationInMilliseconds
        refPlaybackSeekBar?.max = refPlaybackDuration
        referenceAudioPlayer.currentPosition = refPlaybackProgress
        refPlaybackSeekBar?.progress = refPlaybackProgress
        refPlaybackSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                referenceAudioPlayer.currentPosition = refPlaybackProgress
                if(wasAudioPlaying){
                    referenceAudioPlayer.resumeAudio()
                }
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasAudioPlaying = referenceAudioPlayer.isAudioPlaying
                referenceAudioPlayer.pauseAudio()
                referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                referencePlayButton!!.contentDescription = getString(R.string.rec_toolbar_play_recording_button)
            }
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    refPlaybackProgress = progress
                }
            }
        })
    }
    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        super.onPause()
        refPlaybackProgress = referenceAudioPlayer.currentPosition
        mSeekBarTimer.cancel()
        referenceAudioPlayer.release()
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     */

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        referenceAudioPlayer.stopAudio()
        referencePlayButton?.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
        referencePlayButton?.contentDescription = getString(R.string.rec_toolbar_play_recording_button)
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     */
    protected fun setPic(slideImage: ImageView) {

        // Adjust the Guideline control to scale the video view for 4:3 videos (also used for 16:9 videos)
        rootView?.findViewById<Guideline>(R.id.guideline)
            ?.setGuidelineBegin((resources.displayMetrics.widthPixels.toFloat() / 4f * 3f).toInt())

        (activity as PhaseBaseActivity).setPic(slideImage, slideNum)
        //Set up the reference audio and slide number overlays
        referencePlayButton = rootView?.findViewById(R.id.fragment_reference_audio_button)
        setReferenceAudioButton()

        val slideNumberText = rootView?.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText?.text = slideNum.toString()
    }

    private fun setReferenceAudioButton() {
        referencePlayButton!!.setOnClickListener {
            if (!storyRelPathExists(context!!,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                // Now using an appropriate "no audio" string for all phases
                if (Workspace.activePhase.phaseType == PhaseType.TRANSLATE_REVISE)
                    Snackbar.make(rootView!!,
                            getString(R.string.translate_revise_playback_no_lwc_audio, Workspace.activeStory.langCode),
                            Snackbar.LENGTH_LONG).show()    // Tell user Translate phase has no audio for this slide
                else
                    Snackbar.make(rootView!!, R.string.other_playback_no_local_audio, Snackbar.LENGTH_LONG).show()
            } else {
                //stop other playback streams.
                if (referenceAudioPlayer.isAudioPlaying) {
                    stopSlidePlayBack()
                    refPlaybackProgress = referenceAudioPlayer.currentPosition
                    refPlaybackSeekBar?.progress = refPlaybackProgress
                } else {
                    stopSlidePlayBack()
                    onStartedSlidePlayBack()
                    referenceAudioPlayer.currentPosition = refPlaybackProgress
                    referenceAudioPlayer.resumeAudio()

                    referencePlayButton!!.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                    referencePlayButton!!.contentDescription = getString(R.string.rec_toolbar_pause_recording_button)
                    Toast.makeText(context, R.string.translate_revise_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    when(Workspace.activePhase.phaseType){
                        PhaseType.TRANSLATE_REVISE -> saveLog(activity!!.getString(R.string.LWC_PLAYBACK))
                        PhaseType.COMMUNITY_WORK -> saveLog(activity!!.getString(R.string.DRAFT_PLAYBACK))
                        else -> {}
                    }
                }
            }
        }
    }

    protected fun stopSlidePlayBack() {
        referenceAudioPlayer.pauseAudio()
        referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
        referencePlayButton!!.contentDescription = getString(R.string.rec_toolbar_play_recording_button)
    }

    open fun onStartedSlidePlayBack() {}

    /**
     * Sets the main text of the layout.
     * The text will be ran through and checked if any of the words are a wordlink.
     * These matching strings will be turned into a link that can be clicked to open WordLinksActivity, showing the user more about the wordlink.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    protected fun setScriptureText(rootView: View?, textView: TextView) {
        val phrases = Workspace.WLSTree.splitOnWordLinks(slide.content)
        textView.text = phrases.fold(SpannableStringBuilder()) {
            result, phrase -> result.append(stringToWordLink(phrase, activity))
        }
        // this method provides cursor positioning, scrolling and text selection functionality
        textView.movementMethod = LinkMovementMethod.getInstance()

        if (rootView != null) {
            textView.setOnTouchListener() { view: View, motionEvent: MotionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rootView?.findViewById<View>(R.id.concheck_logs_button)?.visibility = View.INVISIBLE
                        rootView?.findViewById<View>(R.id.concheck_checkmark_button)?.visibility = View.INVISIBLE
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        rootView?.findViewById<View>(R.id.concheck_logs_button)?.visibility = View.VISIBLE
                        rootView?.findViewById<View>(R.id.concheck_checkmark_button)?.visibility = View.VISIBLE
                    }
                }
                false
            }
        }
    }

    protected fun setReferenceText(textView: TextView) {
        val titleNamePriority = arrayOf(slide.reference, slide.subtitle, slide.title)
        for (title in titleNamePriority) {
            if (title != "") {
                textView.text = title
                return
            }
        }
        //There is no reference text.
        textView.text = ""
    }

}

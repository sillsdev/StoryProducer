package org.sil.storyproducer.controller

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Guideline
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.model.stringToWordLink
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.viewmodel.SlideViewModel
import org.sil.storyproducer.viewmodel.SlideViewModelBuilder
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask

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
    protected var mPopupHelpUtils: PopupHelpUtils? = null


    companion object {
        /**
         * Checks each slide of the story to see if all slides have been approved
         * @return true if all approved, otherwise false
         */
        public fun checkAllMarked(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context);
            // confirm all if setting set and not a bible story
            val confirmAll = prefs.getBoolean("accuracy_check_skip", false) and
                    !Workspace.activeStory.isSPAuthored
            for (slide in Workspace.activeStory.slides) {
                if (!slide.isChecked && slide.slideType in
                    arrayOf(SlideType.FRONTCOVER, SlideType.NUMBEREDPAGE, SlideType.LOCALSONG)
                ) {
                    if (confirmAll)
                        slide.isChecked = true
                    else
                        return false
                }
            }
            if (confirmAll)
                Workspace.activeStory.isApproved = true
            return true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            slideNum = this.requireArguments().getInt(SLIDE_NUM)
            viewModel = SlideViewModelBuilder(Workspace.activeStory.slides[slideNum]).build()
            slide = Workspace.activeStory.slides[slideNum]
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        // The last two arguments ensure LayoutParams are inflated properly.
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
            referenceAudioPlayer.setStorySource(requireContext(), refAudioFile)

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

        stopAndDeletePopupMenus()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndDeletePopupMenus()
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
        slideNumberText?.text = when (Workspace.activeStory.slides[slideNum].slideType) {
            SlideType.FRONTCOVER -> getString(R.string.slide_type_title)
            SlideType.LOCALSONG -> getString(R.string.slide_type_song)
            else -> getString(R.string.slide_type_number) + " $slideNum"
        }
    }

    private fun setReferenceAudioButton() {
        referencePlayButton!!.setOnClickListener {
            if (!storyRelPathExists(requireContext(),Workspace.activePhase.getReferenceAudioFile(slideNum))) {
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
                        rootView.findViewById<View>(R.id.concheck_logs_button).visibility = View.INVISIBLE
                        rootView.findViewById<View>(R.id.concheck_checkmark_button).visibility = View.INVISIBLE
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        rootView.findViewById<View>(R.id.concheck_logs_button).visibility = View.VISIBLE
                        rootView.findViewById<View>(R.id.concheck_checkmark_button).visibility = View.VISIBLE
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

    private fun stopAndDeletePopupMenus() {

        if (mPopupHelpUtils != null) {
            mPopupHelpUtils?.dismissPopup()
            mPopupHelpUtils = null
        }

    }

}

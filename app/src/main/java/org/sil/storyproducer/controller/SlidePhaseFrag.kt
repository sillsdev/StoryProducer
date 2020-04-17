package org.sil.storyproducer.controller

import android.media.MediaPlayer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.*
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.media.AudioPlayer
import java.util.*
import android.util.Log

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class SlidePhaseFrag : Fragment() {
    protected lateinit var rootView: View

    protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    protected lateinit var referencePlayButton: ImageButton
    protected lateinit var refPlaybackSeekBar: SeekBar
    private lateinit var seekBar: ConstraintLayout
    private var mSeekBarTimer = Timer()

    private var refPlaybackProgress = 0
    private var wasAudioPlaying = false


    protected var slideNumber: Int = 0 // gets overwritten
    protected lateinit var phaseType: PhaseType
    protected lateinit var slide: Slide

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNumber = arguments!!.getInt(SLIDE_NUM)
        phaseType = PhaseType.ofInt(arguments!!.getInt(PHASE_TYPE))
        slide = Workspace.activeStory.slides[slideNumber]
    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_slide, container, false)
        initializeViews()
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_mic_white_48dp)
    }

    protected open fun setPic() {
        PhaseBaseActivity.setPic(context!!, rootView.findViewById<View>(R.id.fragment_image_view) as ImageView, slideNumber)
    }

    private fun setupAudioPlayer(): Boolean {
        val referenceRecording = phaseType.getReferenceRecording(slideNumber)
        referenceAudioPlayer = AudioPlayer()
        if (referenceRecording != null) {
            referenceAudioPlayer.setStorySource(context!!, referenceRecording.fileName)
            refPlaybackSeekBar.max = referenceAudioPlayer.audioDurationInMilliseconds
            refPlaybackSeekBar.progress = refPlaybackProgress
            
            referencePlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)

            referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                referencePlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                referenceAudioPlayer.stopAudio()
            })
            return true
        } else {
            return false
        }
    }

    // This is an alternative to onCreateView. The trouble with onCreateView is
    // that we want to have a default view that gets inflated, but allow
    // inheriting fragments to inflate a different view. Inheriting fragments
    // should just override this function if they want to add extra
    // initialization. They could also override both this function and
    // onCreateView if they want to have a different layout get inflated.
    protected open fun initializeViews() {
        setPic()
        
        refPlaybackSeekBar = rootView.findViewById(R.id.videoSeekBar)
        seekBar = rootView.findViewById(R.id.seek_bar)
        
        val slideNumberText = rootView.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText.text = slideNumber.toString()

        referencePlayButton = rootView.findViewById(R.id.fragment_reference_audio_button)
        referencePlayButton.setOnClickListener {
            if (referenceAudioPlayer.isAudioPlaying) {
                stopSlidePlayBack()
                refPlaybackProgress = referenceAudioPlayer.currentPosition
                refPlaybackSeekBar.progress = refPlaybackProgress
            } else {
                if (referenceAudioPlayer.currentPosition == 0) {
                    if (!setupAudioPlayer()) {
                        Snackbar.make(rootView, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
                    }
                }
                if (referenceAudioPlayer.isAudioPrepared) {
                    onStartedSlidePlayBack()
                    referenceAudioPlayer.currentPosition = refPlaybackProgress
                    referenceAudioPlayer.resumeAudio()

                    referencePlayButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    when(phaseType) {
                        PhaseType.DRAFT -> saveLog(activity!!.getString(R.string.LWC_PLAYBACK))
                        PhaseType.COMMUNITY_CHECK -> saveLog(activity!!.getString(R.string.DRAFT_PLAYBACK))
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setSeekBarListener() {
        referenceAudioPlayer.currentPosition = refPlaybackProgress
        refPlaybackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                referenceAudioPlayer.currentPosition = refPlaybackProgress
                if(wasAudioPlaying){
                    referenceAudioPlayer.resumeAudio()
                    referencePlayButton.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                }
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasAudioPlaying = referenceAudioPlayer.isAudioPlaying
                if (wasAudioPlaying) {
                    referenceAudioPlayer.pauseAudio()
                    referencePlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                }
            }
            
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    refPlaybackProgress = progress
                }
            }
        })
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (::rootView.isInitialized) {
            if (isVisibleToUser) {
                if (referenceAudioPlayer.isAudioPlaying) {
                    stopSlidePlayBack()
                }
            
                setupAudioPlayer()
                
                // If it is the local credits slide, do not show the audio stuff at all.
                if (Workspace.activeStory.slides[slideNumber].slideType == SlideType.LOCALCREDITS) {
                    seekBar.visibility = View.GONE
                } else {
                    mSeekBarTimer = Timer()
                    mSeekBarTimer.schedule(object : TimerTask() {
                        override fun run() {
                            activity?.runOnUiThread{
                                refPlaybackProgress = referenceAudioPlayer.currentPosition
                                refPlaybackSeekBar.progress = refPlaybackProgress
                            }
                        }
                    }, 0, 33)

                    setSeekBarListener()
                }

            } else {
                refPlaybackProgress = referenceAudioPlayer.currentPosition
                mSeekBarTimer.cancel()
                referenceAudioPlayer.release()
            }
        }

    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    protected fun setScriptureText(textView: TextView) {
        textView.text = slide.content
    }

    /**
     * This function sets the reference text.
     *
     * @param textView The view that will be populated with the reference text.
     */
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

    protected fun stopSlidePlayBack() {
        referenceAudioPlayer.pauseAudio()
        referencePlayButton.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
    }

    open fun onStartedSlidePlayBack() {}
}

package org.sil.storyproducer.controller

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.KeyTermActivity.Companion.stringToKeytermLink
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Slide
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.getStoryImage
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import java.util.*

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
abstract class SlidePhaseFrag : Fragment() {
    protected var rootView: View? = null

    protected var referenceAudioPlayer: AudioPlayer = AudioPlayer()
    protected var referncePlayButton: ImageButton? = null
    protected var refPlaybackSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var refPlaybackProgress = 0
    private var refPlaybackDuration = 0
    private var wasAudioPlaying = false


    protected var slideNum: Int = 0 //gets overwritten
    protected var slide: Slide = Workspace.activeSlide!! //this is a placeholder that gets overwritten in onCreate.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = this.arguments!!.getInt(SLIDE_NUM)
        slide = Workspace.activeStory.slides[slideNum]
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_mic_white_48dp)
    }


    override fun onResume() {
        super.onResume()

        referenceAudioPlayer = AudioPlayer()
        referenceAudioPlayer.setStorySource(context!!,Workspace.activePhase.getReferenceAudioFile(slideNum))

        referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            referncePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
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
                referncePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
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
        referncePlayButton?.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    protected fun setPic(slideImage: ImageView) {
        val slidePicture: Bitmap = getStoryImage(context!!,slideNum)

        slideImage.setImageBitmap(slidePicture)

        //Set up the reference audio and slide number overlays
        referncePlayButton = rootView?.findViewById(R.id.fragment_reference_audio_button)
        setReferenceAudioButton()

        val slideNumberText = rootView?.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText?.text = slideNum.toString()
    }

    /**
     * Sets the main text of the layout.
     *
     * @param textView The text view that will be filled with the verse's text.
     */
    protected fun setScriptureText(textView: TextView) {
        val words = slide.content.split(" ")
        textView.text = words.fold(SpannableStringBuilder()){
            result, word -> result.append(stringToKeytermLink(word, activity)).append(" ")
        }
        textView.movementMethod = LinkMovementMethod.getInstance()
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

    protected fun setReferenceAudioButton() {
        referncePlayButton!!.setOnClickListener {
            if (!storyRelPathExists(context!!,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                if (referenceAudioPlayer.isAudioPlaying) {
                    referenceAudioPlayer.pauseAudio()
                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                    refPlaybackProgress = referenceAudioPlayer.currentPosition
                    refPlaybackSeekBar?.progress = refPlaybackProgress
                } else {
                    //stop other playback streams.
                    referenceAudioPlayer.currentPosition = refPlaybackProgress
                    referenceAudioPlayer.resumeAudio()

                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                    when(Workspace.activePhase.phaseType){
                        PhaseType.DRAFT -> saveLog(activity!!.getString(R.string.LWC_PLAYBACK))
                        PhaseType.COMMUNITY_CHECK -> saveLog(activity!!.getString(R.string.DRAFT_PLAYBACK))
                        else -> {}
                    }
                }
            }
        }
    }
    companion object {
        const val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
    }
}

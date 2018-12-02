package org.sil.storyproducer.controller.dramatization

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.LayoutInflater
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.controller.ToolbarFrag
import org.sil.storyproducer.tools.file.getStoryImage
import java.util.*


class DramatizationFrag : SlidePhaseFrag() {

    private var slideText: EditText? = null
    private var draftPlaybackSeekBar: SeekBar? = null
    private var mSeekBarTimer = Timer()

    private var draftPlaybackProgress = 0
    private var draftPlaybackDuration = 0
    private var wasAudioPlaying = false
    private var referencePlayButton : ImageButton? = null
    private var listener : DramatizationFrag.OnAudioPlayListener? = null

    interface OnAudioPlayListener {
        //fun onPlayButtonClicked(path: String, image : ImageButton, stopImage: Int, playImage : Int)
        fun onPauseButtonClicked(path: String, image : ImageButton, stopImage: Int, playImage : Int) : Int?
        fun getCurrentAudioPosition() : Int?
        fun getDuration() : Int?
        fun setPosition(pos: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is DramatizationFrag.OnAudioPlayListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_dramatization, container, false)

        val slidePicture: Bitmap = getStoryImage(activity!!,slideNum)
        rootView?.findViewById<ImageView>(R.id.fragment_image_view)?.setImageBitmap(slidePicture)

        slideText = rootView?.findViewById(R.id.fragment_dramatization_edit_text)
        slideText?.setText(Workspace.activeStory.slides[slideNum].translatedContent, TextView.BufferType.EDITABLE)

        val arguments = Bundle()
        arguments.putBoolean("enablePlaybackButton", true)
        arguments.putBoolean("enableDeleteButton", false)
        arguments.putBoolean("enableMultiRecordButton", true)
        arguments.putBoolean("enableSendAudioButton", false)
        arguments.putInt("slideNum", 0)

        val toolbarFrag = childFragmentManager.findFragmentById(R.id.bottom_toolbar) as ToolbarFrag
        toolbarFrag.arguments = arguments
        toolbarFrag.setupToolbarButtons()

        referencePlayButton = rootView?.findViewById(R.id.fragment_reference_audio_button)
        setReferenceAudioButton()

        if (Workspace.activeStory.isApproved) {
            closeKeyboardOnTouch(rootView)
            rootView?.findViewById(R.id.lock_overlay)?.visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView!!)
        }

        draftPlaybackSeekBar = rootView?.findViewById(R.id.videoSeekBar)

        //Make the text bigger if it is the front Page.
        if(Workspace.activeStory.slides[slideNum].slideType == SlideType.FRONTCOVER){
            slideText!!.setTextSize(COMPLEX_UNIT_DIP,24f)
            slideText!!.hint = context!!.getString(R.string.dramatization_edit_title_text_hint)
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()

        mSeekBarTimer = Timer()
        mSeekBarTimer.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread{
                    draftPlaybackProgress = listener?.getCurrentAudioPosition()!!
                    draftPlaybackSeekBar?.progress = draftPlaybackProgress
                }
            }
        },0,33)

        setSeekBarListener()
    }


    private fun setSeekBarListener() {
        draftPlaybackDuration = listener?.getDuration()!!
        draftPlaybackSeekBar?.max = draftPlaybackDuration
        listener?.setPosition(draftPlaybackProgress)
        draftPlaybackSeekBar?.progress = draftPlaybackProgress
        draftPlaybackSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                listener?.setPosition(draftPlaybackProgress)
//                if(wasAudioPlaying){
//                    referenceAudioPlayer.resumeAudio()
//                }
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {
//                wasAudioPlaying = referenceAudioPlayer.isAudioPlaying
//                referenceAudioPlayer.pauseAudio()
                referencePlayButton?.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
            }
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    draftPlaybackProgress = progress
                }
            }
        })
    }
    /**
     * This function serves to stop the audio streams from continuing after dramatization has been
     * put on pause.
     */
    override fun onPause() {
        draftPlaybackProgress = listener?.getCurrentAudioPosition()!!
        mSeekBarTimer.cancel()
        super.onPause()
        closeKeyboard(rootView)
    }

    /**
     * This function serves to handle draft page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
//                if (recordingToolbar != null) {
//                    recordingToolbar!!.onPause()
//                }
                closeKeyboard(rootView)
            }
        }
    }

    private fun setReferenceAudioButton() {
        referencePlayButton?.setOnClickListener {
            if (!storyRelPathExists(context!!,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                draftPlaybackProgress = listener?.onPauseButtonClicked(Workspace.activePhase.getReferenceAudioFile(slideNum), referencePlayButton!!, R.drawable.ic_pause_white_48dp, R.drawable.ic_play_arrow_white_36dp)!!
                draftPlaybackSeekBar?.progress = draftPlaybackProgress
            }
        }
    }

    /**
     * This function will set a listener to the passed in view so that when the passed in view
     * is touched the keyboard close function will be called see: [.closeKeyboard].
     *
     * @param touchedView The view that will have an on touch listener assigned so that a touch of
     * the view will close the softkeyboard.
     */
    private fun closeKeyboardOnTouch(touchedView: View?) {
        touchedView?.setOnClickListener { closeKeyboard(touchedView) }
    }

    /**
     * This function closes the keyboard. The passed in view will gain focus after the keyboard is
     * hidden. The reestablished focus allows the removal of a cursor or any other focus indicator
     * from the previously focused view.
     *
     * @param viewToFocus The view that will gain focus after the keyboard is hidden.
     */
    private fun closeKeyboard(viewToFocus: View?) {
        if (viewToFocus != null) {
            val imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
        Workspace.activeStory.slides[slideNum].translatedContent = slideText?.text.toString()
    }

}

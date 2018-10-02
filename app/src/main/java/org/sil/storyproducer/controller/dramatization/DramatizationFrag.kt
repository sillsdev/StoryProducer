package org.sil.storyproducer.controller.dramatization

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsList
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.StorySharedPreferences
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.toolbar.PausingRecordingToolbar
import org.sil.storyproducer.tools.toolbar.RecordingToolbar.RecordingListener


class DramatizationFrag : MultiRecordFrag() {

    private var phaseUnlocked: Boolean = false
    private var slideText: EditText? = null
    private var draftPlaybackSeekBar: SeekBar? = null
    private var draftPlaybackProgress = 0
    private var draftPlaybackDuration = 0
    private var wasAudioPlaying = false


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_dramatization, container, false)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)
        slideText = rootView!!.findViewById(R.id.fragment_dramatization_edit_text)
        slideText!!.setText(Workspace.activeStory.slides[slideNum].translatedContent, TextView.BufferType.EDITABLE)

        phaseUnlocked = StorySharedPreferences.isApproved(Workspace.activeStory.title, context)

        if (phaseUnlocked) {
            rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
            setToolbar(rootViewToolbar)
            closeKeyboardOnTouch(rootView)
            rootView!!.findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            PhaseBaseActivity.disableViewAndChildren(rootView!!)
        }

        draftPlaybackSeekBar = rootView!!.findViewById(R.id.videoSeekBar)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        setSeekBarListener()
    }


    private fun setSeekBarListener() {
        draftPlaybackDuration = referenceAudioPlayer.audioDurationInMilliseconds
        draftPlaybackSeekBar!!.max = draftPlaybackDuration
        referenceAudioPlayer.currentPosition = draftPlaybackProgress
        draftPlaybackSeekBar!!.progress = draftPlaybackProgress
        draftPlaybackSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                referenceAudioPlayer.currentPosition = draftPlaybackProgress
                if(wasAudioPlaying){
                    referenceAudioPlayer.resumeAudio()
                }
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasAudioPlaying = referenceAudioPlayer.isAudioPlaying
                referenceAudioPlayer.pauseAudio()
                referncePlayButton!!.setBackgroundResource(R.drawable.ic_menu_play)
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
        draftPlaybackProgress = referenceAudioPlayer.currentPosition
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
                if (recordingToolbar != null) {
                    recordingToolbar!!.onPause()
                }
                closeKeyboard(rootView)
            }
        }
    }

    override fun setReferenceAudioButton() {
        referncePlayButton!!.setOnClickListener {
            if (!storyRelPathExists(context,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                //TODO make "no audio" string work for all phases
                Snackbar.make(rootView!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
            } else {
                if (referenceAudioPlayer.isAudioPlaying) {
                    referenceAudioPlayer.pauseAudio()
                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_menu_play)
                    draftPlaybackProgress = referenceAudioPlayer.currentPosition
                    draftPlaybackSeekBar!!.setProgress(draftPlaybackProgress)
                } else {
                    //stop other playback streams.
                    referenceAudioPlayer.currentPosition = draftPlaybackProgress
                    referenceAudioPlayer.playAudio()

                    referncePlayButton!!.setBackgroundResource(R.drawable.ic_stop_white_36dp)
                    Toast.makeText(context, R.string.draft_playback_lwc_audio, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Initializes the toolbar and toolbar buttons.
     */
    private fun setToolbar(toolbar: View?) {
        if (rootView is RelativeLayout) {
            val recordingListener = object : RecordingListener {
                override fun onStoppedRecording() {}
                override fun onStartedRecordingOrPlayback(isRecording: Boolean) {}
            }

            val rList = RecordingsList(context, this)

            //TODO re-enable the pausing recording toolbar when wav saving and concatentation are working again.
            recordingToolbar = PausingRecordingToolbar(activity, toolbar!!, rootView as RelativeLayout,
                    true, false, true, false, rList, recordingListener, slideNum)
            recordingToolbar!!.keepToolbarVisible()
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
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(viewToFocus.windowToken, 0)
            viewToFocus.requestFocus()
        }
        Workspace.activeStory.slides[slideNum].translatedContent = slideText!!.text.toString()
    }

}

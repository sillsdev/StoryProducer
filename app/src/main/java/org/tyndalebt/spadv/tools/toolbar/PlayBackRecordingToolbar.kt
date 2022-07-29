package org.tyndalebt.spadv.tools.toolbar

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.model.PhaseType
import org.tyndalebt.spadv.model.SLIDE_NUM
import org.tyndalebt.spadv.model.Workspace
import org.tyndalebt.spadv.model.logging.saveLog
import org.tyndalebt.spadv.tools.file.getChosenFilename
import org.tyndalebt.spadv.tools.file.storyRelPathExists
import org.tyndalebt.spadv.tools.media.AudioPlayer

/**
 * A class responsible for the playback of audio recordings for a recording toolbar.
 *
 * This class extends the recording functionality of its base class. A playback button is added to
 * the UI in addition to the recording button.
 */
open class PlayBackRecordingToolbar: RecordingToolbar() {
    private lateinit var playButton: ImageButton

    override lateinit var toolbarMediaListener: RecordingToolbar.ToolbarMediaListener
    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isAudioPlaying : Boolean
        get() {return audioPlayer.isAudioPlaying}
    private var slideNum : Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)

        toolbarMediaListener = try {
            context as ToolbarMediaListener
        }
        catch (e : ClassCastException){
            parentFragment as ToolbarMediaListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundleArguments = arguments
        if (bundleArguments != null) {
            slideNum = bundleArguments.getInt(SLIDE_NUM)
        }

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            stopToolbarAudioPlaying()
        })
    }

    override fun onPause() {
        audioPlayer.release()

        super.onPause()
    }

    interface ToolbarMediaListener: RecordingToolbar.ToolbarMediaListener{
        fun onStoppedToolbarPlayBack(){
            onStoppedToolbarMedia()
        }
        fun onStartedToolbarPlayBack(){
            onStartedToolbarMedia()
        }
    }

    override fun stopToolbarMedia() {
        super.stopToolbarMedia()

        if (audioPlayer.isAudioPlaying) {
            stopToolbarAudioPlaying()
        }
    }

    private fun stopToolbarAudioPlaying()   {
        audioPlayer.stopAudio()

        playButton.setBackgroundResource(R.drawable.ic_play_arrow_white_48dp)

        (toolbarMediaListener as ToolbarMediaListener).onStoppedToolbarPlayBack()
    }

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        playButton = toolbarButton(R.drawable.ic_play_arrow_white_48dp, R.id.play_recording_button)
        rootView?.addView(playButton)
        
        rootView?.addView(toolbarButtonSpace())
    }

    /*
     * This function sets the visibility of any inherited buttons based on the existence of
     * a playback file.
     */
    override fun updateInheritedToolbarButtonVisibility(){
        val playBackFileExist = storyRelPathExists(activity!!, getChosenFilename(slideNum))
        if(playBackFileExist){
            showInheritedToolbarButtons()
        }
        else{
            hideInheritedToolbarButtons()
        }
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        playButton.visibility = View.VISIBLE
    }

    override fun hideInheritedToolbarButtons() {
        super.hideInheritedToolbarButtons()

        playButton.visibility = View.INVISIBLE
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

        playButton.setOnClickListener(playButtonOnClickListener())
    }

    private fun playButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val wasPlaying = audioPlayer.isAudioPlaying

            stopToolbarMedia()

            if (!wasPlaying) {
                (toolbarMediaListener as ToolbarMediaListener).onStartedToolbarPlayBack()

                if (audioPlayer.setStorySource(this.appContext, getChosenFilename())) {
                    audioPlayer.playAudio()

                    playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
                    
                    //TODO: make this logging more robust and encapsulated
                    when (Workspace.activePhase.phaseType){
                        PhaseType.TRANSLATE_REVISE -> saveLog(appContext.getString(R.string.DRAFT_PLAYBACK))
                        PhaseType.COMMUNITY_WORK-> saveLog(appContext.getString(R.string.COMMENT_PLAYBACK))
                        else ->{}
                    }
                } else {
                    Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
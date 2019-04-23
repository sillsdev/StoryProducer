package org.sil.storyproducer.tools.toolbar

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer

open class PlayBackRecordingToolbar: RecordingToolbar() {
    private lateinit var playButton: ImageButton

    override lateinit var toolbarMediaListener: RecordingToolbar.ToolbarMediaListener
    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isAudioPlaying : Boolean
        get() {return audioPlayer.isAudioPlaying}
    private var slideNum : Int = 0

    override fun onAttach(context: Context?) {
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
            slideNum = bundleArguments.get(SLIDE_NUM) as Int
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            stopToolbarAudioPlaying()
        })

        return rootView
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
        val playBackFileExist = storyRelPathExists(activity!!, Workspace.activePhase.getChosenFilename(slideNum))
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

                if (audioPlayer.setStorySource(this.appContext, Workspace.activePhase.getChosenFilename())) {
                    audioPlayer.playAudio()

                    Toast.makeText(appContext, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show()

                    playButton.setBackgroundResource(R.drawable.ic_stop_white_48dp)
                    
                    //TODO: make this logging more robust and encapsulated
                    when (Workspace.activePhase.phaseType){
                        PhaseType.DRAFT -> saveLog(appContext.getString(R.string.DRAFT_PLAYBACK))
                        PhaseType.COMMUNITY_CHECK-> saveLog(appContext.getString(R.string.COMMENT_PLAYBACK))
                        else ->{}
                    }
                } else {
                    Toast.makeText(appContext, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
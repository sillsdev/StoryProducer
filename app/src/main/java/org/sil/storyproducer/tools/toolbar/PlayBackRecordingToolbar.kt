package org.sil.storyproducer.tools.toolbar

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.media.AudioPlayer

open class PlayBackRecordingToolbar: RecordingToolbar() {
    private lateinit var playButton: ImageButton

    private var audioPlayer: AudioPlayer = AudioPlayer()
    val isAudioPlaying : Boolean
        get() {return audioPlayer.isAudioPlaying}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            stopToolbarAudioPlaying()
        })
    }

    override fun onPause() {
        super.onPause()

        audioPlayer.release()
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
        recordingListener.onStoppedRecordingOrPlayback(false)
    }

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        playButton = toolbarButton(R.drawable.ic_play_arrow_white_48dp, org.sil.storyproducer.R.id.play_recording_button)
        rootView?.addView(playButton)
        rootView?.addView(toolbarButtonSpace())
    }

    override fun showSecondaryButtons() {
        super.showSecondaryButtons()

        playButton.visibility = View.VISIBLE
    }

    override fun hideSecondaryButtons() {
        super.hideSecondaryButtons()

        playButton.visibility = View.INVISIBLE
    }

    override fun setToolbarOnClickListeners() {
        super.setToolbarOnClickListeners()

        playButton.setOnClickListener(playButtonOnClickListener())
    }

    private fun playButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            stopToolbarMedia()
            if (!audioPlayer.isAudioPlaying) {
                recordingListener.onStartedRecordingOrPlayback(false)
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
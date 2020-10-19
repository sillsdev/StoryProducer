package org.sil.storyproducer.controller.learn

import android.media.MediaPlayer
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.res.ResourcesCompat
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.SlidePlayerFrag
import org.sil.storyproducer.controller.StoryPlayerFrag
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.SLIDE_NUM
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLearnLog
import org.sil.storyproducer.tools.file.getStoryUri
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.util.*
import kotlin.math.min

class LearnActivity : PhaseBaseActivity(), PlayBackRecordingToolbar.ToolbarMediaListener {
    lateinit var audioSwitch : Switch
    private var isWatchedOnce = false

    private var recordingToolbar: PlayBackRecordingToolbar = PlayBackRecordingToolbar()
    lateinit var storyPlayer : StoryPlayerFrag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        audioSwitch = findViewById(R.id.learn_volume_switch)
        audioSwitch.setOnCheckedChangeListener { _, checked ->
            if(checked) {
                storyPlayer.unmute()
            } else {
                storyPlayer.mute()
            }
        }

        setToolbar()
        invalidateOptionsMenu()
    }

    public override fun onPause() {
        super.onPause()

        storyPlayer.stop()
        // Remove the story player so that we can add it in when we switch phases
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.remove(storyPlayer).commit()
    }

    public override fun onResume() {
        super.onResume()

//        storyPlayer = if(Workspace.activeStory.isVideoStory) VideoPlayerFrag() else SlidePlayerFrag()
        storyPlayer = SlidePlayerFrag()
        // FIXME: add film back in
        storyPlayer.startSlide = 0
        storyPlayer.slideRange = Workspace.activeStory.numSlides
        storyPlayer.phaseType = Workspace.activePhase.phaseType
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.activity_learn, storyPlayer).commit()
    }

    private fun setToolbar(){
        val bundle = Bundle()
        bundle.putInt(SLIDE_NUM, 0)
        recordingToolbar.arguments = bundle
        supportFragmentManager?.beginTransaction()?.replace(R.id.toolbar_for_recording_toolbar, recordingToolbar)?.commit()

        recordingToolbar.keepToolbarVisible()
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user that they should practice saying the story
     */
    private fun showStartPracticeSnackBar() {
        if (!isWatchedOnce) {
            val snackbar = Snackbar.make(findViewById(R.id.drawer_layout),
                    R.string.learn_phase_practice, Snackbar.LENGTH_LONG)
            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.lightWhite, null))
            val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            snackbar.show()
        }
        isWatchedOnce = true
    }
}

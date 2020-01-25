package org.sil.storyproducer.tools.toolbar

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.PHASE_TYPE
import org.sil.storyproducer.model.PhaseType

/**
 * A class responsible for listing recorded audio files from a recording toolbar.
 *
 * This class extends both recording and playback functionality of its base classes. A third button
 * is added that can bring up a modal listing the audio recording created with this toolbar.
 */
open class MultiRecordRecordingToolbar: PlayBackRecordingToolbar() {
    protected lateinit var multiRecordButton: ImageButton

    protected lateinit var phaseType: PhaseType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phaseType = PhaseType.ofInt(arguments!!.getInt(PHASE_TYPE))

    }
    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        multiRecordButton = toolbarButton(R.drawable.ic_playlist_play_white_48dp, R.id.list_recordings_button)
        rootView?.addView(multiRecordButton)
        rootView?.addView(toolbarButtonSpace())
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        multiRecordButton.visibility = View.VISIBLE
    }

    override fun hideInheritedToolbarButtons() {
        super.hideInheritedToolbarButtons()

        multiRecordButton.visibility = View.INVISIBLE
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

        multiRecordButton.setOnClickListener(multiRecordButtonOnClickListener())
    }

    protected open fun multiRecordButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()

            toolbarMediaListener.onStartedToolbarMedia()
            
            RecordingsListAdapter.RecordingsListModal(activity!!, this, phaseType).show()
        }
    }
}

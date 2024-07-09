package org.sil.storyproducer.tools.toolbar

import android.view.View
import android.widget.ImageButton
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter

/**
 * A class responsible for listing recorded audio files from a recording toolbar.
 *
 * This class extends both recording and playback functionality of its base classes. A third button
 * is added that can bring up a modal listing the audio recording created with this toolbar.
 */
open class MultiRecordRecordingToolbar: PlayBackRecordingToolbar() {
    protected lateinit var multiRecordButton: ImageButton

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        multiRecordButton = toolbarButton(R.drawable.ic_playlist_play_white_48dp, R.id.list_recordings_button, R.string.rec_toolbar_play_playlist_button)
        rootView?.addView(multiRecordButton)
        
        rootView?.addView(toolbarButtonSpace())
    }

    override fun showInheritedToolbarButtons() {
        super.showInheritedToolbarButtons()

        multiRecordButton.visibility = View.VISIBLE
        multiRecordButton.alpha = 1.0f
        multiRecordButton.isEnabled = true
    }

    override fun hideInheritedToolbarButtons(animated: Boolean) {
        super.hideInheritedToolbarButtons(animated)

        if (animated)
            multiRecordButton.visibility = View.INVISIBLE   // hide when record flashing
        multiRecordButton.alpha = 0.5f
        multiRecordButton.isEnabled = false
    }

    override fun setToolbarButtonOnClickListeners() {
        super.setToolbarButtonOnClickListeners()

        multiRecordButton.setOnClickListener(multiRecordButtonOnClickListener())
    }

    protected open fun multiRecordButtonOnClickListener(): View.OnClickListener{
        return View.OnClickListener {
            stopToolbarMedia()

            toolbarMediaListener.onStartedToolbarMedia()
            
            RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
        }
    }
}
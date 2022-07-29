package org.tyndalebt.spadv.tools.toolbar

import android.view.View
import android.widget.ImageButton
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.adapter.RecordingsListAdapter

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
            
            RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
        }
    }
}
package org.sil.storyproducer.tools.toolbar

import android.view.View
import android.widget.ImageButton
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter

open class MultiRecordRecordingToolbar: PlayBackRecordingToolbar() {
    protected lateinit var multiRecordButton: ImageButton

    override fun setupToolbarButtons() {
        super.setupToolbarButtons()

        multiRecordButton = toolbarButton(R.drawable.ic_playlist_play_white_48dp, org.sil.storyproducer.R.id.list_recordings_button)
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

            recordingListener.onStartedRecordingOrPlayback(false)
            
            RecordingsListAdapter.RecordingsListModal(activity!!, this).show()
        }
    }
}
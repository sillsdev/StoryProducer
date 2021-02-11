/*
package org.sil.storyproducer.controller.wordlink

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.hideKeyboard
import org.sil.storyproducer.tools.toolbar.MultiRecordRecordingToolbar

/**
 * A class responsible for keyterm specific functionality for audio recording and playback.
 *
 * This class extends the recording, playback, and multi-recording listing functionality of its base
 * classes. The class overrides the multi-record playlist button to display a bottom sheet that
 * lists audio recordings instead of a modal that lists the audio recordings.
 */
class WordLinkRecordingToolbar : MultiRecordRecordingToolbar() {
    lateinit var bottomSheet: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bottomSheet = (activity as KeyTermActivity).bottomSheet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)

        from(bottomSheet).setBottomSheetCallback(bottomSheetCallback())
        setKeytermMultiRecordIcon(from(bottomSheet).state)

        return rootView
    }

    private fun bottomSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(view: View, newState: Int) {
                setKeytermMultiRecordIcon(newState)

                if(newState == STATE_COLLAPSED){
                    view.let { activity?.hideKeyboard(it) }
                }
                // Disables opening recording list when no recordings are available
                if(Workspace.activeKeyterm.keytermRecordings.isEmpty()){
                    from(bottomSheet).state = STATE_COLLAPSED
                }
            }

            override fun onSlide(view: View, newState: Float) {}
        }
    }

    /**
     * The state of the bottom sheet will determine the icon used.
     */
    private fun setKeytermMultiRecordIcon(state: Int){
        if(state == STATE_EXPANDED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_keyboard_arrow_down_white_48dp)
        }
        else if(state == STATE_COLLAPSED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_playlist_play_white_48dp)
        }
    }

    override fun multiRecordButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            stopToolbarMedia()

            toolbarMediaListener.onStartedToolbarMedia()

            if (from(bottomSheet).state == STATE_EXPANDED) {
                from(bottomSheet).state = STATE_COLLAPSED
            } else {
                from(bottomSheet).state = STATE_EXPANDED
            }
        }
    }
}
*/
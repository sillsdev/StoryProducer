package org.sil.storyproducer.controller.keyterm

import android.support.design.widget.BottomSheetBehavior
import android.view.View
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.hideKeyboard
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class KeytermRecordingToolbar : RecordingToolbar(){
    override fun setOnClickListeners(){
        super.setOnClickListeners()

        val bottomSheet = (activity as KeyTermActivity).bottomSheet
        BottomSheetBehavior.from(bottomSheet).setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(view: View, newState: Int) {
                setKeytermMultiRecordIcon(newState)
                if(newState == BottomSheetBehavior.STATE_COLLAPSED){
                    view.let { activity?.hideKeyboard(it) }
                }
                // Disables opening recording list when no recordings are available
                if(Workspace.activeKeyterm.keytermRecordings.isEmpty()){
                    BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            override fun onSlide(view: View, newState: Float) {}
        })
        setKeytermMultiRecordIcon(BottomSheetBehavior.from(bottomSheet).state)
    }

    override fun multiRecordButtonOnClickListener(): View.OnClickListener {
        return View.OnClickListener {
            stopToolbarMedia()
            val bottomSheet = (activity as KeyTermActivity).bottomSheet
            if (BottomSheetBehavior.from(bottomSheet).state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    /**
     * The state of the bottom sheet will determine the icon used.
     */
    private fun setKeytermMultiRecordIcon(state: Int){
        if(state == BottomSheetBehavior.STATE_EXPANDED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_keyboard_arrow_down_white_48dp)
        }
        else if(state == BottomSheetBehavior.STATE_COLLAPSED){
            multiRecordButton.setBackgroundResource(R.drawable.ic_playlist_play_white_48dp)
        }
    }
}
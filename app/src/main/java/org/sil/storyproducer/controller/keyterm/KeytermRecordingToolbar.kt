package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.hideKeyboard
import org.sil.storyproducer.tools.toolbar.MultiRecordRecordingToolbar

class KeytermRecordingToolbar : MultiRecordRecordingToolbar(){
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

    private fun bottomSheetCallback(): BottomSheetCallback{
        return object : BottomSheetCallback(){
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

            if (from(bottomSheet).state == STATE_EXPANDED) {
                from(bottomSheet).state = STATE_COLLAPSED
            } else {
                from(bottomSheet).state = STATE_EXPANDED
            }
        }
    }
}
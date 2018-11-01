package org.sil.storyproducer.controller.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsList
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityCheckFrag : MultiRecordFrag() {
    private var dispList : RecordingsList? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_community_check, container, false)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)
        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
        setToolbar()
        dispList = RecordingsList(context!!, this)
        dispList!!.embedList(rootView!! as ViewGroup)
        dispList!!.setSlideNum(slideNum)
        dispList!!.show()
        return rootView
    }

    override fun onPause() {
        super.onPause()
        dispList!!.stopAudio()
    }

    override fun setToolbar() {
        val recordingListener = object : RecordingToolbar.RecordingListener {
            override fun onStoppedRecording() {
                dispList!!.createRecordingList()
            }

            override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                //not used here
            }
        }
        val rList = RecordingsList(context!!, this)

        recordingToolbar = RecordingToolbar(this.activity!!, rootViewToolbar!!, rootView as RelativeLayout,
                false, false, false, false,  rList , recordingListener, slideNum);
        recordingToolbar!!.keepToolbarVisible()
        recordingToolbar!!.stopToolbarMedia()
    }

    /**
     * Stops the toolbar from recording or playing back media.
     * Used in [DraftListRecordingsModal]
     */
    override fun stopPlayBackAndRecording() {
        super.stopPlayBackAndRecording()
        dispList!!.stopAudio()
    }

}

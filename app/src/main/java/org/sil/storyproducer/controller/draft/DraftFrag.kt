package org.sil.storyproducer.controller.draft

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.ScriptureFrag
import org.sil.storyproducer.controller.SlidePhaseFrag
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class DraftFrag : Fragment(), RecordingToolbar.RecordingListener, SlidePhaseFrag.PlaybackListener {
    private var slideNum: Int = 0
    private val recordingToolbar = RecordingToolbar()
    private val slidePhaseFrag = SlidePhaseFrag()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_draft_layout, container, false)

        slideNum = arguments!!.getInt(SlidePhaseFrag.SLIDE_NUM)
        setSlide()
        setScripture()

        if (Workspace.activeStory.slides[slideNum].slideType != SlideType.LOCALCREDITS) {
            setToolbar()
        }

        return rootView
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     *
     * @param isVisibleToUser whether fragment is currently visible to user
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // Make sure that we are currently visible
        if (this.isVisible) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                recordingToolbar.stopToolbarMedia()
                slidePhaseFrag.stopPlayBackAndRecording()
            }
        }
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {}

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        recordingToolbar.stopToolbarMedia()
        slidePhaseFrag.stopPlayBackAndRecording()
    }

    private fun setToolbar() {
        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,false,true,false))
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()

        recordingToolbar.keepToolbarVisible()
    }

    private fun setScripture(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        val scriptureFrag = ScriptureFrag()
        scriptureFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.scripture_text, scriptureFrag).commit()
    }

    private fun setSlide(){
        val bundle = Bundle()
        bundle.putInt(SlidePhaseFrag.SLIDE_NUM, slideNum)
        slidePhaseFrag.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.slide_phase, slidePhaseFrag).commit()
    }

    override fun onStoppedPlayback() {}

    override fun onStartedPlayback() {
        recordingToolbar.stopToolbarMedia()
    }
}

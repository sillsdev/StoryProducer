package org.sil.storyproducer.controller.draft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.SlidePlayerFrag
import org.sil.storyproducer.controller.VideoPlayerFrag
import org.sil.storyproducer.model.Workspace

/**
 * The fragment for the Draft view. This is where a user can draft out the story slide by slide
 */
class TranslateReviseFrag : MultiRecordFrag() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        super.onCreateView(inflater, container, savedInstanceState)
        setScriptureText(rootView!!.findViewById(R.id.fragment_scripture_text))
        setReferenceText(rootView!!.findViewById(R.id.fragment_reference_text))
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storyPlayer = if(Workspace.activeStory.isVideoStory) VideoPlayerFrag() else SlidePlayerFrag()
        storyPlayer?.startSlide = slideNum
        storyPlayer?.slideRange = 1
        storyPlayer?.phaseType = Workspace.activePhase.phaseType
        var transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.phase_player, storyPlayer!!).commit()
    }

}

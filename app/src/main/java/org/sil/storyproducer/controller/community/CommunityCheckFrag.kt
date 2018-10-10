package org.sil.storyproducer.controller.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MultiRecordFrag
import org.sil.storyproducer.controller.adapter.RecordingsList

/**
 * Fragment for the community check view. The purpose of this phase is for the community to make
 * sure the draft is okay and leave any comments should they feel the need
 */
class CommunityCheckFrag : MultiRecordFrag() {
    private var dispList : RecordingsList? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_community_check, container, false)

        setUiColors()
        setPic(rootView!!.findViewById<View>(R.id.fragment_image_view) as ImageView)
        rootViewToolbar = inflater.inflate(R.layout.toolbar_for_recording, container, false)
        setToolbar()
        dispList = RecordingsList(context, this)
        dispList!!.embedList(rootView!! as ViewGroup)
        dispList!!.show()
        return rootView
    }

}

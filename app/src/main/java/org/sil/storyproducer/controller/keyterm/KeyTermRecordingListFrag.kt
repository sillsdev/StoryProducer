package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson

class KeyTermRecordingListFrag : Fragment() {

    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)

        val adapter = RecyclerDataAdapter(context, Workspace.activeKeyterm.backTranslations)
        val viewManager = LinearLayoutManager(context)

        val recordingExpandableListView = view.findViewById<RecyclerView>(R.id.recording_list)
        recordingExpandableListView.setHasFixedSize(true)
        recordingExpandableListView.adapter = adapter
        recordingExpandableListView.layoutManager = viewManager


        dispList = RecordingsListAdapter.RecordingsListModal(view, context!!, null, recordingExpandableListView)
        dispList?.embedList(view!! as ViewGroup)
        dispList?.show()

        return view
    }
}
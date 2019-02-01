package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.Workspace

class KeyTermRecordingListFrag : Fragment() {

    private val adapter = RecyclerDataAdapter(context, Workspace.activeKeyterm.backTranslations)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)

        val viewManager = LinearLayoutManager(context)

        val recordingExpandableListView = rootView?.findViewById<RecyclerView>(R.id.recording_list)
        recordingExpandableListView?.setHasFixedSize(true)
        recordingExpandableListView?.adapter = adapter
        recordingExpandableListView?.layoutManager = viewManager

        val toolbar = (activity?.supportFragmentManager?.findFragmentById(R.id.keyterm_info) as KeyTermMainFrag).recordingToolbar
        val dispList : RecordingsListAdapter.RecordingsListModal = RecordingsListAdapter.RecordingsListModal(context!!, toolbar, recordingExpandableListView)
        dispList.embedList(rootView as ViewGroup)
        dispList.show()

        return rootView
    }

    fun updateListsInsert(pos: Int){
        adapter.notifyItemInserted(pos)
    }

    fun updateListsModified(pos: Int){
        adapter.notifyItemChanged(pos)
    }
}
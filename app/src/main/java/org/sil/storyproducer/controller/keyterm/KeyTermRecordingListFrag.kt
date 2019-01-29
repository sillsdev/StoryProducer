package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.Workspace

class KeyTermRecordingListFrag : Fragment() {

    private val adapter = RecyclerDataAdapter(context, Workspace.activeKeyterm.backTranslations)
    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        rootView = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)

        val viewManager = LinearLayoutManager(context)

        val recordingExpandableListView = rootView?.findViewById<RecyclerView>(R.id.recording_list)
        recordingExpandableListView?.setHasFixedSize(true)
        recordingExpandableListView?.adapter = adapter
        recordingExpandableListView?.layoutManager = viewManager

        val otherFragment = fragmentManager?.findFragmentById(R.id.keyterm_info)
        val dispList : RecordingsListAdapter.RecordingsListModal = RecordingsListAdapter.RecordingsListModal(otherFragment?.view, context!!, null, recordingExpandableListView)
        dispList.embedList(rootView as ViewGroup)
        dispList.show()

        showRecordings(Workspace.activeKeyterm.backTranslations.size != 0)

        return rootView
    }

    fun updateListsInsert(pos: Int){
        adapter.notifyItemInserted(pos)
        showRecordings(true)
    }

    fun updateListsModified(pos: Int){
        adapter.notifyItemChanged(pos)
    }

    private fun showRecordings(isVisible: Boolean) {
        if(!isVisible) {
            rootView?.findViewById<TextView>(R.id.no_recordings_title)?.visibility = View.VISIBLE
            rootView?.findViewById<TextView>(R.id.no_recordings_message)?.visibility = View.VISIBLE
            rootView?.findViewById<RecyclerView>(R.id.recording_list)?.visibility = View.GONE
        }
        else{
            rootView?.findViewById<TextView>(R.id.no_recordings_title)?.visibility = View.GONE
            rootView?.findViewById<TextView>(R.id.no_recordings_message)?.visibility = View.GONE
            rootView?.findViewById<RecyclerView>(R.id.recording_list)?.visibility = View.VISIBLE
        }
    }
}
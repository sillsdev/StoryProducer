package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView

import org.sil.storyproducer.R
import org.sil.storyproducer.model.RecordingBacktranslationsPair

class KeyTermRecordingListFrag : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)
        val recordingExpandableListView = fragmentView.findViewById(R.id.recording_list) as ExpandableListView
        val adapter = RecordingExpandableListAdapter(context, getData())
        recordingExpandableListView.setAdapter(adapter)
        expandAllGroups(recordingExpandableListView)

        return fragmentView
    }

    private fun expandAllGroups(expandableListView: ExpandableListView){
        val groupCount = expandableListView.count
        for(position in 0 until groupCount) {
            expandableListView.expandGroup(position)
        }
    }

    // TODO Replace this function with dynamic recordings
    private fun getData() : List<RecordingBacktranslationsPair>{
        val expandableListDetail: MutableList<RecordingBacktranslationsPair> = mutableListOf()

        var recording = ArrayList<String>()
        recording.add("Backtranslation 1")
        recording.add("Backtranslation 2\nMore information")
        recording.add("Backtranslation 3")
        var pair = RecordingBacktranslationsPair("recording 1", recording)
        expandableListDetail.add(pair)

        recording = ArrayList()
        pair = RecordingBacktranslationsPair("recording 2", recording)
        expandableListDetail.add(pair)

        recording = ArrayList()
        recording.add("Backtranslation 1")
        pair = RecordingBacktranslationsPair("recording 3", recording)
        expandableListDetail.add(pair)

        return expandableListDetail
    }
}
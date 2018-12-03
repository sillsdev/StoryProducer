package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.ImageButton

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.Workspace

class KeyTermRecordingListFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val fragmentView = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)
        val recordingExpandableListView = fragmentView.findViewById(R.id.recording_list) as ExpandableListView
        val keyterm : Keyterm = arguments?.getParcelable("Keyterm")!!
        Workspace.activeKeyterm = keyterm
        val adapter = RecordingExpandableListAdapter(context, keyterm.backTranslations)
        recordingExpandableListView.setAdapter(adapter)
        expandAllGroups(recordingExpandableListView)

        val addBacktranslation = fragmentView.findViewById<ImageButton>(R.id.submit_backtranslation_button)
        val editText = fragmentView.findViewById<EditText>(R.id.backtranslation_edit_text)
        addBacktranslation.setOnClickListener {
            if(editText.text.toString() != ""){
                Workspace.activeKeyterm.backTranslations[0].textBackTranslation.add(editText.text.toString())
            }
        }


        return fragmentView
    }

    private fun expandAllGroups(expandableListView: ExpandableListView){
        val groupCount = expandableListView.count
        for(position in 0 until groupCount) {
            expandableListView.expandGroup(position)
        }
    }
}
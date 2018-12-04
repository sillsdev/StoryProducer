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

    private var keyterm : Keyterm? = null
    private var dispList : RecordingsListAdapter.RecordingsListModal? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_keyterm_recording_list, container, false)

        keyterm  = arguments?.getParcelable("Keyterm")!!
        Workspace.activeKeyterm = keyterm!!

        val adapter = RecyclerDataAdapter(context, Workspace.activeKeyterm.backTranslations)
        val viewManager = LinearLayoutManager(context)

        val recordingExpandableListView = view.findViewById<RecyclerView>(R.id.recording_list)
        recordingExpandableListView.setHasFixedSize(true)
        recordingExpandableListView.adapter = adapter
        recordingExpandableListView.layoutManager = viewManager


        dispList = RecordingsListAdapter.RecordingsListModal(view, context!!, null, recordingExpandableListView)
        dispList?.embedList(view!! as ViewGroup)
        dispList?.show()

        val addBacktranslation = view.findViewById<ImageButton>(R.id.submit_backtranslation_button)
        val editText = view.findViewById<EditText>(R.id.backtranslation_edit_text)
        addBacktranslation.setOnClickListener {
            if(editText.text.toString() != ""){
                Workspace.activeKeyterm.backTranslations[adapter.currentPosition].textBackTranslation.add(editText.text.toString())
                recordingExpandableListView.adapter = adapter
                editText.setText("")
            }
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        Workspace.termsToKeyterms[keyterm?.term!!] = Workspace.activeKeyterm
        Thread(Runnable{ activity?.let { Workspace.activeKeyterm.toJson(it) } }).start()
    }

    override fun onResume() {
        super.onResume()
        Workspace.activeKeyterm = arguments?.getParcelable("Keyterm")!!
    }
}
package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace

class KeyTermAudioFrag : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyTermAudioLayout = KeyTermRecordingListFrag()
        activity?.supportFragmentManager?.beginTransaction()?.add(R.id.keyterm_info_audio, keyTermAudioLayout)?.addToBackStack(Workspace.activeKeyterm.term)?.commit()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_keyterm_audio, container, false)
    }
}

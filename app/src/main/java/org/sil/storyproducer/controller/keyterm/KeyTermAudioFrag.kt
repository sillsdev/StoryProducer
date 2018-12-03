package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Keyterm

class KeyTermTextFrag : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyTerm = arguments?.getParcelable<Keyterm>("Keyterm")
        val bundle = Bundle()
        bundle.putParcelable("Keyterm", keyTerm)
        val keyTermLayout = KeyTermMainFrag()
        keyTermLayout.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()?.add(R.id.keyterm_info, keyTermLayout)?.commit()

        val keyTermAudioLayout = KeyTermRecordingListFrag()
        keyTermAudioLayout.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()?.add(R.id.keyterm_audio, keyTermAudioLayout)?.commit()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_keyterm_text, container, false)
    }
}

package org.sil.storyproducer.controller.keyterm


import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.ToolbarFrag
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson

/**
 * This layout file is for the keyterms information to update each time a new one is clicked
 *
 */
class KeyTermLayout : Fragment() {

    private var keyTerm : Keyterm? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_key_term, container, false)

        keyTerm = arguments?.getParcelable("Keyterm")
        Workspace.activeKeyterm = keyTerm!!
        rootView.findViewById<TextView>(R.id.term_text).text = keyTerm?.term
        rootView.findViewById<TextView>(R.id.alternateRenderings_text).text = keyTerm?.alternateRenderings.toString()
        rootView.findViewById<TextView>(R.id.explanation_text).text = keyTerm?.explanation

        val relatedTermsView = rootView.findViewById<TextView>(R.id.relatedTerms_text)
        relatedTermsView.text = stringToSpannableString(keyTerm?.relatedTerms!!, activity!!)
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        val arguments = Bundle()
        arguments.putBoolean("enablePlaybackButton", true)
        arguments.putBoolean("enableDeleteButton", false)
        arguments.putBoolean("enableMultiRecordButton", true)
        arguments.putBoolean("enableSendAudioButton", false)
        arguments.putInt("slideNum", 0)

        val toolbarFrag = childFragmentManager.findFragmentById(R.id.bottom_toolbar) as ToolbarFrag
        toolbarFrag.arguments = arguments
        toolbarFrag.setupToolbarButtons()

        return rootView
    }

    override fun onPause() {
        super.onPause()
        Workspace.termsToKeyterms[keyTerm?.term!!] = Workspace.activeKeyterm
        Thread(Runnable{ activity?.let { Workspace.activeKeyterm.toJson(it) } }).start()
    }

    override fun onResume() {
        super.onResume()
        Workspace.activeKeyterm = arguments?.getParcelable("Keyterm")!!
    }
}

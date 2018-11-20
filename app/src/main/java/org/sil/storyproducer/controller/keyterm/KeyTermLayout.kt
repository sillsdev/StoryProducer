package org.sil.storyproducer.controller.keyterm


import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Keyterm

/**
 * A simple [Fragment] subclass.
 *
 */
class KeyTermLayout : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_key_term, container, false)

        val keyTerm = arguments?.getParcelable<Keyterm>("Keyterm")
        if(keyTerm != null) {
            rootView.findViewById<TextView>(R.id.term_text).text = keyTerm.term
            rootView.findViewById<TextView>(R.id.alternateRenderings_text).text = keyTerm.alternateRenderings.toString()
            rootView.findViewById<TextView>(R.id.explanation_text).text = keyTerm.explanation

            val relatedTermsView = rootView.findViewById<TextView>(R.id.relatedTerms_text)
            relatedTermsView.text = stringToSpannableString(keyTerm.relatedTerms, activity!!)
            relatedTermsView.movementMethod = LinkMovementMethod.getInstance()
        }

        return rootView
    }

//    override fun onPause() {
//        super.onPause()
//        view?.findViewById<TextView>(R.id.term_text)?.text = ""
//        view?.findViewById<TextView>(R.id.alternateRenderings_text)?.text = ""
//        view?.findViewById<TextView>(R.id.explanation_text)?.text = ""
//    }
}

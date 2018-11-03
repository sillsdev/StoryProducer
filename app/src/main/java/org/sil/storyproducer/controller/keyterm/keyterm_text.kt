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

class keyterm_text : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_keyterm_text, container, false)
        val keyterm = arguments?.getParcelable<Keyterm>("Keyterm")
        if(keyterm != null) {
            view.findViewById<TextView>(R.id.term_text).text = keyterm.term
            view.findViewById<TextView>(R.id.alternateRenderings_text).text = keyterm.alternateRenderings
            view.findViewById<TextView>(R.id.explanation_text).text = keyterm.explanation

            val relatedTermsView = view.findViewById<TextView>(R.id.relatedTerms_text)
            relatedTermsView.text = stringToSpannableString(keyterm.relatedTerms, context)
            relatedTermsView.movementMethod = LinkMovementMethod.getInstance()
        }
        return view
    }
}

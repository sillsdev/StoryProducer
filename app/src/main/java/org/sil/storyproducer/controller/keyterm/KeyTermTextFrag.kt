package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.KeyTermActivity.Companion.stringToKeytermLink
import org.sil.storyproducer.model.Keyterm

class KeyTermTextFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_keyterm_text, container, false)
        val keyterm = arguments?.getParcelable<Keyterm>("Keyterm")
        if(keyterm != null) {
            val relatedTermsView = view.findViewById<TextView>(R.id.relatedTerms_text)
            relatedTermsView.text = keyterm.relatedTerms.fold(SpannableStringBuilder()){
                result, relatedTerm -> result.append(stringToKeytermLink(relatedTerm, context)).append("   ")
            }
            relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

            view.findViewById<TextView>(R.id.alternateRenderings_text).text = keyterm.alternateRenderings.fold(""){
                result, alternateRendering -> result + "\u2022 $alternateRendering\n"
            }.removeSuffix("\n")

            view.findViewById<TextView>(R.id.explanation_text).text = keyterm.explanation
        }
        return view
    }
}

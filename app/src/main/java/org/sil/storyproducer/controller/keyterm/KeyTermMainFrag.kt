package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.ToolbarFrag
import org.sil.storyproducer.controller.keyterm.KeyTermActivity.Companion.stringToKeytermLink
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson

class KeyTermMainFrag : Fragment() {

    private var keyterm : Keyterm? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_keyterm_main, container, false)
        keyterm = arguments?.getParcelable("Keyterm")
        Workspace.activeKeyterm = keyterm!!

        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title = keyterm?.term

        val relatedTermsView = view.findViewById<TextView>(R.id.related_terms_text)
        relatedTermsView.text = keyterm?.relatedTerms?.fold(SpannableStringBuilder()){
            result, relatedTerm -> result.append(stringToKeytermLink(relatedTerm, activity)).append("   ")
        }
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()
        if(keyterm?.relatedTerms?.isEmpty() == true){
            relatedTermsView.visibility = View.GONE
        }

        val alternateRenderingsView = view.findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = keyterm?.alternateRenderings?.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }?.removeSuffix("\n")
        if(keyterm?.alternateRenderings?.isEmpty()!!){
            alternateRenderingsView.visibility = View.GONE
        }

        val explanationView = view.findViewById<TextView>(R.id.explanation_text)
        explanationView.text = keyterm?.explanation
        if(keyterm?.explanation == ""){
            explanationView.visibility = View.GONE
        }

        val arguments = Bundle()
        arguments.putBoolean("enablePlaybackButton", true)
        arguments.putBoolean("enableCheckButton", false)
        arguments.putBoolean("enableMultiRecordButton", true)
        arguments.putBoolean("enableSendAudioButton", false)
        arguments.putInt("slideNum", 0)

        val toolbarFrag = childFragmentManager.findFragmentById(R.id.bottom_toolbar) as ToolbarFrag
        toolbarFrag.arguments = arguments
        toolbarFrag.setupToolbarButtons()
        toolbarFrag.view?.findViewById<ImageButton>(0)?.setOnClickListener {
            toolbarFrag.micListener()

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

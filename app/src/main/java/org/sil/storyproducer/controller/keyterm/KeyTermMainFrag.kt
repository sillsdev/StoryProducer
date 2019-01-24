package org.sil.storyproducer.controller.keyterm

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.KeyTermActivity.Companion.stringToKeytermLink
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class KeyTermMainFrag : Fragment() {

    private var recordingToolbar: RecordingToolbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_keyterm_main, container, false)

        val actionBar = (activity as AppCompatActivity).supportActionBar

        val title = arguments?.getString("ClickedTerm")
        if(title != null) {
            actionBar?.title = title
        }
        else{
            actionBar?.title = Workspace.activeKeyterm.term
        }

        val keyTermTitleView = view.findViewById<TextView>(R.id.keytermTitle)
        var titleText = ""
        if(Workspace.activeKeyterm.term.toLowerCase() != title) {
            titleText = Workspace.activeKeyterm.term
        }
        for (termForm in Workspace.activeKeyterm.termForms){
            if(termForm != title) {
                if (titleText.isNotEmpty()) {
                    titleText += " / $termForm"
                }
                else{
                    titleText = termForm
                }
            }
        }
        if(titleText == ""){
            keyTermTitleView.visibility = View.GONE
        }
        else {
            keyTermTitleView.text = titleText
        }


        val relatedTermsView = view.findViewById<TextView>(R.id.related_terms_text)
        relatedTermsView.text = Workspace.activeKeyterm.relatedTerms.fold(SpannableStringBuilder()){
            result, relatedTerm -> result.append(stringToKeytermLink(relatedTerm, activity)).append("   ")
        }
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()
        if(Workspace.activeKeyterm.relatedTerms.isEmpty()){
            relatedTermsView.visibility = View.GONE
        }

        val alternateRenderingsView = view.findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = Workspace.activeKeyterm.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }.removeSuffix("\n")
        if(Workspace.activeKeyterm.alternateRenderings.isEmpty()){
            alternateRenderingsView.visibility = View.GONE
        }

        val explanationView = view.findViewById<TextView>(R.id.explanation_text)
        explanationView.text = Workspace.activeKeyterm.explanation
        if(Workspace.activeKeyterm.explanation == ""){
            explanationView.visibility = View.GONE
        }

        val recyclerView = (activity as AppCompatActivity).supportFragmentManager.findFragmentById(R.id.keyterm_info_audio)?.view?.findViewById<RecyclerView>(R.id.recording_list)
        val backTranslationLayout = view.findViewById<FrameLayout>(R.id.backtranslation_comment)
        recordingToolbar = RecordingToolbar(activity!!,
                view!!, true, false, true, false,
                object : RecordingToolbar.RecordingListener {
            override fun onStoppedRecording() {
                recyclerView?.adapter?.notifyItemInserted(recyclerView.adapter?.itemCount!!-1)
                //show must recent recording and backtranslation below the toolbar
                val recentRecording = inflater.inflate(R.layout.submit_backtranslation_item, container, false)
                val editText = recentRecording.findViewById<EditText>(R.id.backtranslation_edit_text)
                val backTranslationButton = recentRecording.findViewById<ImageButton>(R.id.submit_backtranslation_button)
                backTranslationButton.setOnClickListener {
                    if(editText.text.toString() != ""){
                        Workspace.activeKeyterm.backTranslations[Workspace.activeKeyterm.backTranslations.size-1].textBackTranslation = editText.text.toString()
                        recyclerView?.adapter?.notifyItemChanged(recyclerView.adapter?.itemCount!!-1)
                        backTranslationLayout.removeAllViews()
                    }
                }
                backTranslationLayout.addView(recentRecording)

            }
            override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
                backTranslationLayout.removeAllViews()
            }
        }, 0)

        return view
    }
}

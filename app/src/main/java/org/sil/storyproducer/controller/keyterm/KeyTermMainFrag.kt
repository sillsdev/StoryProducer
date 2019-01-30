package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class KeyTermMainFrag : Fragment(), RecordingToolbar.RecordingListener {

    private lateinit var backTranslationLayout : FrameLayout

    private lateinit var tellAudioListFragment: RecordClicked

    interface RecordClicked{
        fun audioListInserted(pos: Int)
        fun audioListChanged(pos: Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_keyterm_main, container, false)

        val actionBar = (activity as AppCompatActivity).supportActionBar

        val title = arguments?.getString("ClickedTerm")
        actionBar?.title = title ?: Workspace.activeKeyterm.term

        val keyTermTitleView = view.findViewById<TextView>(R.id.keyterm_title)
        var titleText = ""
        if(Workspace.activeKeyterm.term.toLowerCase() != title?.toLowerCase()) {
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

        val explanationView = view.findViewById<TextView>(R.id.explanation_text)
        explanationView.text = Workspace.activeKeyterm.explanation

        val relatedTermsView = view.findViewById<TextView>(R.id.related_terms_text)
        relatedTermsView.text = Workspace.activeKeyterm.relatedTerms.fold(SpannableStringBuilder()){
            result, relatedTerm -> result.append(stringToKeytermLink(context!!, relatedTerm, activity)).append("   ")
        }
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        val alternateRenderingsView = view.findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = Workspace.activeKeyterm.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }.removeSuffix("\n")

        backTranslationLayout = view.findViewById(R.id.backtranslation_comment)

        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true,false,true,false))
        bundle.putInt("slideNum", 0)
        (activity as KeyTermActivity).recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().add(R.id.toolbar_for_recording_toolbar, (activity as KeyTermActivity).recordingToolbar).commit()

        return view
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        if(isRecording){
            backTranslationLayout.removeAllViews()
        }
    }

    override fun onStoppedRecording() {
        tellAudioListFragment.audioListInserted(Workspace.activeKeyterm.backTranslations.size-1)
        //show most recent recording and backtranslation below the toolbar
        val inflater = LayoutInflater.from(context)
        val recentRecording = inflater.inflate(R.layout.submit_backtranslation_item, null, false)
        val editText = recentRecording.findViewById<EditText>(R.id.backtranslation_edit_text)
        val backTranslationButton = recentRecording.findViewById<ImageButton>(R.id.submit_backtranslation_button)
        backTranslationButton.setOnClickListener {
            if(editText.text.toString() != ""){
                Workspace.activeKeyterm.backTranslations[Workspace.activeKeyterm.backTranslations.size-1].textBackTranslation = editText.text.toString()
                tellAudioListFragment.audioListChanged(Workspace.activeKeyterm.backTranslations.size-1)
                backTranslationLayout.removeAllViews()
            }
        }
        backTranslationLayout.addView(recentRecording)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        tellAudioListFragment = context as RecordClicked
    }
}

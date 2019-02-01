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
import android.widget.FrameLayout
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

class KeyTermMainFrag : Fragment(), RecordingToolbar.RecordingListener {

    var recordingToolbar : RecordingToolbar = RecordingToolbar()

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

        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true, false, true, false))
        bundle.putInt("slideNum", 0)
        recordingToolbar.arguments = bundle
        childFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).addToBackStack("").commit()

        return view
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {}

    override fun onStoppedRecordingOrPlayback(isRecordingFinished: Boolean) {
        if(isRecordingFinished) {
            tellAudioListFragment.audioListInserted(Workspace.activeKeyterm.backTranslations.size - 1)
            (activity as KeyTermActivity).findViewById<FrameLayout>(R.id.keyterm_info_audio).visibility = View.VISIBLE
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        tellAudioListFragment = context as KeyTermMainFrag.RecordClicked
    }
}

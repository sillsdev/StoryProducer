package org.sil.storyproducer.model

import android.content.Intent
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.squareup.moshi.JsonClass
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.keyterm.KeyTermActivity
import org.sil.storyproducer.controller.keyterm.PHASE

@JsonClass(generateAdapter = true)
class KeytermList (val keyterms: List<Keyterm>) {
    companion object
}

@JsonClass(generateAdapter = true)
class Keyterm (var term: String = "",
               var termForms: List<String> = listOf(),
               var alternateRenderings: List<String> = listOf(),
               var explanation: String = "",
               var relatedTerms: List<String> = listOf(),

               var backTranslations: MutableList<BackTranslation> = mutableListOf(),
               var chosenKeytermFile: String = "") {
    companion object
}

@JsonClass(generateAdapter = true)
class BackTranslation (var textBackTranslation : String = "",
                       var audioBackTranslation : String = "") {
    companion object
}


fun stringToKeytermLink(string: String, fragmentActivity: FragmentActivity?): SpannableString {
    val spannableString = SpannableString(string)
    if (Workspace.termFormToTerm.containsKey(string.toLowerCase())) {
        val clickableSpan = createKeytermClickableSpan(string, fragmentActivity)
        spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannableString
}

private fun createKeytermClickableSpan(term: String, fragmentActivity: FragmentActivity?): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                //Save the active keyterm to the workspace
                Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
                //Save the active keyterm to a json file
                val keytermList = KeytermList(Workspace.termToKeyterm.values.toList())
                Thread(Runnable{ fragmentActivity?.let { keytermList.toJson(it) } }).start()
                //Set keyterm from link as active keyterm
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
                //Add new keyterm fragments to stack
                (fragmentActivity as KeyTermActivity).keytermHistory.push(term)
                (fragmentActivity).setupNoteView()
                (fragmentActivity).setupRecordingList()
                BottomSheetBehavior.from((fragmentActivity).bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
            }
            else {
                //Set keyterm from link as active keyterm
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
                //Start a new keyterm activity and keep a reference to the parent phase
                val intent = Intent(fragmentActivity, KeyTermActivity::class.java)
                intent.putExtra(PHASE, Workspace.activePhase.phaseType)
                intent.putExtra("ClickedTerm", term)
                fragmentActivity?.startActivity(intent)
            }
        }

        override fun updateDrawState(drawState: TextPaint) {
            val keyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]

            val hasRecording = keyterm?.backTranslations?.isNotEmpty()

            if(hasRecording != null && hasRecording){
                drawState.linkColor = ContextCompat.getColor(fragmentActivity!!.applicationContext, R.color.lightGray)
            }

            super.updateDrawState(drawState)
        }
    }
}

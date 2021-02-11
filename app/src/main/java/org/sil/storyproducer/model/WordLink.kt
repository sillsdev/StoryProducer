package org.sil.storyproducer.model

import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import androidx.fragment.app.FragmentActivity
import org.sil.storyproducer.controller.wordlink.WordLinkActivity

/**
 * @since 3.1
 * @authors Aaron Cannon, Justin Stallard, Jake Allinson
 **/

/**
 * A list of all the wordlinks (used for saving all wordlinks in a single file)
 **/
class WordLinkList (val wordLinks: List<WordLink>) {
    companion object
}

data class WordLinkRecording (
    var audioRecordingFilename : String = "",
    var textBackTranslation : String = "",
    var isTextBackTranslationSubmitted: Boolean = false) {
        companion object
}

data class WordLink (
        var term: String = "",
        var termForms: List<String> = listOf(),
        var alternateRenderings: List<String> = listOf(),
        var explanation: String = "",
        var relatedTerms: List<String> = listOf(),
        var wordLinkRecordings: MutableList<WordLinkRecording> = mutableListOf(),
        var chosenWordLinkFile: String = "") {
    companion object
}

/**
 * Takes a string and returns a spannable string with links to open wordlink Activity
 *
 * @param string The string that contains wordlinks
 * @param fragmentActivity The current activity
 * @return A spannable string
 **/
fun stringToWordLink (string: String, fragmentActivity: FragmentActivity?) : SpannableString {
    val spannableString = SpannableString(string)
    if (Workspace.termFormToTermMap.containsKey(string.toLowerCase())) {
        val clickableSpan = createWordLinkClickableSpan(string, fragmentActivity)
        spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        // spannableString.setSpan(ForegroundColorSpan(Color.RED), 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannableString
}

/**
 * Turns the string passed in into a clickableSpan that will open the WordLinkActivity when clicked
 *
 * @param term the wordlink
 * @param fragmentActivity the current activity
 * @return The link to open the wordlink
 **/
private fun createWordLinkClickableSpan(term: String, fragmentActivity: FragmentActivity?): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if (Workspace.activePhase.phaseType == PhaseType.WORDLINK && fragmentActivity is WordLinkActivity) {
                fragmentActivity.replaceActivityWordLink(term)
            }
            else if (Workspace.activePhase.phaseType != PhaseType.WORDLINK) {
                //Start a new keyterm activity and keep a reference to the parent phase
                val intent = Intent(fragmentActivity, WordLinkActivity::class.java)
                intent.putExtra(PHASE, Workspace.activePhase.phaseType)
                intent.putExtra(WORDLINKS_CLICKED_TERM, term)
                fragmentActivity?.startActivity(intent)
            }
        }

//        override fun updateDrawState(drawState: TextPaint) {
//            val keyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]
//
//            val hasRecording = keyterm?.keytermRecordings?.isNotEmpty()
//
//            if(hasRecording != null && hasRecording){
//                drawState.linkColor = ContextCompat.getColor(fragmentActivity!!.applicationContext, R.color.lightGray)
//            }
//
//            super.updateDrawState(drawState)
//        }
    }
}

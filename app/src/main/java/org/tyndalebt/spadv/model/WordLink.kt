package org.tyndalebt.spadv.model

import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.squareup.moshi.JsonClass
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.wordlink.WordLinksActivity

/**
 * A list of all the word links (used for saving all word links in a single file)
 **/
@JsonClass(generateAdapter = true)
class WordLinkList (val wordLinks: List<WordLink>) {
    companion object
}

@JsonClass(generateAdapter = true)
data class WordLinkRecording (
    var audioRecordingFilename : String = "",
    var textBackTranslation : String = "",
    var isTextBackTranslationSubmitted: Boolean = false) {
        companion object
}

@JsonClass(generateAdapter = true)
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
    }
    return spannableString
}

/**
 * Converts a string to a clickableSpan that will open the WordLinksActivity when clicked
 *
 * @param term the wordlink
 * @param fragmentActivity the current activity
 * @return The link to open the wordlink
 **/
private fun createWordLinkClickableSpan(term: String, fragmentActivity: FragmentActivity?): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if (Workspace.activePhase.phaseType == PhaseType.WORD_LINKS && fragmentActivity is WordLinksActivity) {
                fragmentActivity.replaceActivityWordLink(term)
            }
            else if (Workspace.activePhase.phaseType != PhaseType.WORD_LINKS) {
                //Start a new word links activity and keep a reference to the parent phase
                val intent = Intent(fragmentActivity, WordLinksActivity::class.java)
                intent.putExtra(PHASE, Workspace.activePhase.phaseType)
                intent.putExtra(WORD_LINKS_CLICKED_TERM, term)
                fragmentActivity?.startActivity(intent)
            }
        }

        override fun updateDrawState(drawState: TextPaint) {
            val wordLink = Workspace.termToWordLinkMap[Workspace.termFormToTermMap[term.toLowerCase()]]
            val hasRecording = wordLink?.wordLinkRecordings?.isNotEmpty()

            if(hasRecording != null && hasRecording){
                drawState.linkColor = ContextCompat.getColor(fragmentActivity!!.applicationContext, R.color.lightGray)
            }
            super.updateDrawState(drawState)
        }
    }
}

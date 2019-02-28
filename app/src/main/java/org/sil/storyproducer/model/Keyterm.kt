package org.sil.storyproducer.model

import android.content.Context
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

/**
 * @since 2.6 Keyterm
 * @author Aaron Cannon and Justin Stallard
 **/

/**
 * A list of all the keyterms (used for saving all keyterms in a single file)
 **/
@JsonClass(generateAdapter = true)
class KeytermList (val keyterms: List<Keyterm>) {
    companion object
}

/**
 * @param term the keyterm
 * @param termForms a list of the different forms of the keyterm
 * @param alternateRenderings
 * @param explanation a definition of the keyterm
 * @param relatedTerms a list of keyterms that are similar to the keyterm
 * @param chosenKeytermFile the active
 **/
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

/**
 * Saves the active keyterm to the workspace and exports an up-to-date json file for all keyterms
 *
 * @param context Activity's contexts
 **/
fun saveKeyterm(context: Context){
    Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
    val keytermList = KeytermList(Workspace.termToKeyterm.values.toList())
    Thread(Runnable{ context.let { keytermList.toJson(it) } }).start()
}

/**
 * Takes a string and returns a spannable string with links to open keyterm Activity
 *
 * @param string The string that contains keyterms
 * @param fragmentActivity The current activity
 * @return A spannable string
 **/
fun stringToKeytermLink(string: String, fragmentActivity: FragmentActivity?): SpannableString {
    val spannableString = SpannableString(string)
    if (Workspace.termFormToTerm.containsKey(string.toLowerCase())) {
        val clickableSpan = createKeytermClickableSpan(string, fragmentActivity)
        spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannableString
}

/**
 * Turns the string passed in into a clickableSpan that will open the keyterm Activity when clicked
 *
 * @param term the keyterm
 * @param fragmentActivity the current activity
 * @return The link to open the keyterm
 **/
private fun createKeytermClickableSpan(term: String, fragmentActivity: FragmentActivity?): ClickableSpan {
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM && fragmentActivity is KeyTermActivity){
                //Save the active keyterm
                saveKeyterm(fragmentActivity.applicationContext)
                //Set keyterm from link as active keyterm
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
                //Add new keyterm fragments to stack
                fragmentActivity.keytermHistory.push(term)
                fragmentActivity.setupNoteView()
                fragmentActivity.setupRecordingList()
                BottomSheetBehavior.from(fragmentActivity.bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
            }
            else if(Workspace.activePhase.phaseType != PhaseType.KEYTERM){
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

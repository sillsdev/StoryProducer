package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Keyterm
import org.sil.storyproducer.model.Workspace
import android.support.v4.app.NavUtils
import android.view.MenuItem


class KeyTermActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)

        val keyterm: Keyterm = intent.getParcelableExtra("Keyterm")
        findViewById<TextView>(R.id.term_text).text = keyterm.term
        findViewById<TextView>(R.id.termForms_text).text = keyterm.termForms.toString()
        findViewById<TextView>(R.id.alternateRenderings_text).text = keyterm.alternateRenderings
        findViewById<TextView>(R.id.explanation_text).text = keyterm.explanation

        val relatedTermsView = findViewById<TextView>(R.id.relatedTerms_text)
        relatedTermsView.text = stringToSpannableString(keyterm.relatedTerms, this)
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        setSupportActionBar(findViewById(R.id.keyterm_toolbar))
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Respond to the action bar's Up/Home button
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

fun stringToSpannableString(strings : MutableList<String>, applicationContext: Context?): SpannableStringBuilder {
    val newString = SpannableStringBuilder()
    for(text in strings){
        val tempString = SpannableString(text)
        if(Workspace.termsToKeyterms.containsKey(text)){
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(textView : View) {
                    //bundle up the key term to send to new activity
                    val intent = Intent(applicationContext, KeyTermActivity::class.java)
                    intent.putExtra("Keyterm", Workspace.termsToKeyterms[text])
                    applicationContext?.startActivity(intent)
                }
            }
            tempString.setSpan(clickableSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tempString.setSpan(ForegroundColorSpan(Color.BLUE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        newString.append(tempString).append(" ")
    }
    return newString
}
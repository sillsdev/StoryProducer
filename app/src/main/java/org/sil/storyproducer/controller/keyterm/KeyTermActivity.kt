package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewPager
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


class KeyTermActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)

        val keyterm: Keyterm = intent.getParcelableExtra("Keyterm")
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(supportFragmentManager, keyterm)

        //set action bar to have back button (android_manifest is where parent is set)
        setSupportActionBar(findViewById(R.id.keyterm_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}

fun stringToSpannableString(strings : List<String>, applicationContext: Context?): SpannableStringBuilder {
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
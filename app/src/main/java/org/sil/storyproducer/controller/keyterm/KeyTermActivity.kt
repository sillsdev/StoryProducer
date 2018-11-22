package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
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
        val toolBar: android.support.v7.widget.Toolbar = findViewById(R.id.keyterm_toolbar)
        toolBar.title = keyterm.term
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        fun stringToKeytermLink(string: String, context: Context?): SpannableString{
            val spannableString = SpannableString(string)
            if(Workspace.termsToKeyterms.containsKey(string)){
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(textView : View) {
                        //bundle up the key term to send to new activity
                        val intent = Intent(context, KeyTermActivity::class.java)
                        intent.putExtra("Keyterm", Workspace.termsToKeyterms[string])
                        context?.startActivity(intent)
                    }

                    /* TODO If keyterm has a recording, make text stand out less (ex. set text color to white)
                    override fun updateDrawState(ds: TextPaint) {
                        ds.linkColor = Color.WHITE
                        super.updateDrawState(ds)
                    }*/
                }
                spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return spannableString
        }
    }
}

package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
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
        val toolbar: android.support.v7.widget.Toolbar = findViewById(R.id.keyterm_toolbar)
        toolbar.title = keyterm.term
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_keyterm_view, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.closeKeyterm -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            R.id.helpButton -> {
                val dialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.help))
                        .setMessage(R.string.keyterm_help)
                        .create()
                dialog.show()
                true
            }
            else -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
        }
    }

    companion object {
        fun stringToKeytermLink(string: String, context: Context?): SpannableString{
            val spannableString = SpannableString(string)
            if(Workspace.termsToKeyterms.containsKey(string.toLowerCase())){
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(textView : View) {
                        //bundle up the key term to send to new activity
                        val intent = Intent(context, KeyTermActivity::class.java)
                        val keyterm = Workspace.termsToKeyterms[string.toLowerCase()]
                        intent.putExtra("Keyterm", keyterm)
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

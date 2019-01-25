package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.app.NavUtils
import android.support.v4.content.res.ResourcesCompat.getColor
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.sil.storyproducer.R
import org.sil.storyproducer.model.*


class KeyTermActivity : AppCompatActivity() {

    private var viewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)
        Workspace.activePhase = Phase(PhaseType.KEYTERM)
        viewPager = findViewById(R.id.viewPager)
        viewPager?.adapter = ViewPagerAdapter(supportFragmentManager, intent.getStringExtra("ClickedTerm"))

        setupStatusBar()
        val toolbar: android.support.v7.widget.Toolbar = findViewById(R.id.keyterm_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(resources,
                Workspace.activePhase.getColor(), null)))
    }

    override fun onPause() {
        super.onPause()
        //return the phase to what it was previously
        if(intent.hasExtra("Phase")) {
            Workspace.activePhase = Phase(intent.getSerializableExtra("Phase") as PhaseType)
        }
        //save the current term to the workspace
        Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
        Thread(Runnable{ this.let { Workspace.activeKeyterm.toJson(it) } }).start()
    }

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv : FloatArray = floatArrayOf(0.0f,0.0f,0.0f)
            Color.colorToHSV(ContextCompat.getColor(this, Workspace.activePhase.getColor()), hsv)
            hsv[2] *= 0.8f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
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

    override fun onBackPressed() {
        if(viewPager?.currentItem == 0) {
            //if there are two we are at the beginning and need to stop
            if (supportFragmentManager.backStackEntryCount == 2) {
                this.finish()
            }
            //otherwise set the term to the term four earlier
            else if (supportFragmentManager.backStackEntryCount >= 4) {
                val keytermName = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 4).name
                Workspace.activeKeyterm = Workspace.termToKeyterm[keytermName]!!
                super.onBackPressed()
                supportFragmentManager.popBackStack()
            }
        }
        else{
            viewPager?.currentItem = 0
        }
    }
}

fun stringToKeytermLink(context: Context, string: String, fragmentActivity: FragmentActivity?): SpannableString {
    val spannableString = SpannableString(string)
    if (Workspace.termFormToTerm.containsKey(string.toLowerCase())) {
        val clickableSpan = createKeytermClickableSpan(context, string, fragmentActivity)
        spannableString.setSpan(clickableSpan, 0, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannableString
}

private fun createKeytermClickableSpan(context: Context, term: String, fragmentActivity: FragmentActivity?): ClickableSpan{
    return object : ClickableSpan() {
        override fun onClick(textView: View) {
            if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                //Save the active keyterm to the workspace
                Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
                //Save the active keyterm to a json file
                Thread(Runnable{ fragmentActivity?.let { Workspace.activeKeyterm.toJson(it) } }).start()
                //Set keyterm from link as active keyterm
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
                //Create new fragments
                val keyTermAudioLayout = KeyTermRecordingListFrag()
                val keyTermLayout = KeyTermMainFrag()
                //Add clicked term to keyTermLayout for titleBar
                val bundle = Bundle()
                bundle.putString("ClickedTerm", term)
                keyTermLayout.arguments = bundle
                //Add new keyterm fragments to stack
                fragmentActivity?.supportFragmentManager?.beginTransaction()?.replace(R.id.keyterm_info_audio, keyTermAudioLayout)?.addToBackStack(Workspace.activeKeyterm.term)?.commit()
                fragmentActivity?.supportFragmentManager?.beginTransaction()?.replace(R.id.keyterm_info, keyTermLayout)?.addToBackStack("")?.commit()
            }
            else {
                //Set keyterm from link as active keyterm
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
                //Start a new keyterm activity and keep a reference to the parent phase
                val intent = Intent(fragmentActivity, KeyTermActivity::class.java)
                intent.putExtra("Phase", Workspace.activePhase.phaseType)
                intent.putExtra("ClickedTerm", term)
                fragmentActivity?.startActivity(intent)
            }
        }

        override fun updateDrawState(drawState: TextPaint) {
            val keyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]

            val backTranslationWithRecording = keyterm?.backTranslations?.find {
                it.audioBackTranslation != ""
            }

            if(backTranslationWithRecording != null){
                drawState.linkColor = ContextCompat.getColor(context, R.color.lightGray)
            }

            super.updateDrawState(drawState)
        }
    }
}

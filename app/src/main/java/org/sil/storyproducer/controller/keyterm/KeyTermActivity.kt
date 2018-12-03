package org.sil.storyproducer.controller.keyterm

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
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
import android.widget.ImageButton
import android.widget.Toast
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.ToolbarFrag
import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.media.AudioPlayer


class KeyTermActivity : AppCompatActivity(), ToolbarFrag.OnAudioPlayListener {

    private var viewPager: ViewPager? = null
    private var mediaPlayer: AudioPlayer? = null
    private var oldImage : ImageButton? = null
    private var previousPhase : String? = null
    private var keyterm : Keyterm? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)
        Workspace.activePhase = Phase(PhaseType.KEYTERM)
        if(intent.hasExtra("Keyterm")) {
            keyterm = intent.getParcelableExtra("Keyterm")
            viewPager = findViewById(R.id.viewPager)
            viewPager?.adapter = ViewPagerAdapter(supportFragmentManager, keyterm!!)
        }
        if(intent.hasExtra("Phase")) {
            previousPhase = intent.getStringExtra("Phase")
        }

        mediaPlayer = AudioPlayer()

        setupStatusBar()
        val toolbar: android.support.v7.widget.Toolbar = findViewById(R.id.keyterm_toolbar)
        toolbar.title = keyterm?.term
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                Workspace.activePhase.getColor(), null)))
    }

    override fun onPause() {
        super.onPause()
        for(type in PhaseType.values()){
            if(type.name == previousPhase?.toUpperCase()){
                Workspace.activePhase = Phase(type)
            }
        }
    }

    override fun onPlayButtonClicked(path: String, image: ImageButton, stopImage: Int, playImage: Int) {
        mediaPlayer?.onPlayBackStop(MediaPlayer.OnCompletionListener {
            image.setBackgroundResource(playImage)
        })
        if(mediaPlayer?.isAudioPlaying!!){
            oldImage?.setBackgroundResource(playImage)
            mediaPlayer?.stopAudio()
            mediaPlayer?.reset()
        }
        else{
            oldImage = null
        }
        if(oldImage != image) {
            mediaPlayer?.reset()
            if (mediaPlayer?.setStorySource(this, path) == true) {
                oldImage = image
                mediaPlayer?.playAudio()
                Toast.makeText(this, R.string.recording_toolbar_play_back_recording, Toast.LENGTH_SHORT).show()
                image.setBackgroundResource(stopImage)
            } else {
                Toast.makeText(this, R.string.recording_toolbar_no_recording, Toast.LENGTH_SHORT).show()
            }
        }
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

    companion object {
        fun stringToKeytermLink(string: String, fragmentActivity: FragmentActivity?): SpannableString {
            val spannableString = SpannableString(string)
            if (Workspace.termsToKeyterms.containsKey(string.toLowerCase())) {
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(textView: View) {
                        if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                            //bundle up the key term to update fragment if in keyterm view already
                            val keyTermLayout = KeyTermMainFrag()
                            var bundle = Bundle()
                            val keyterm = Workspace.termsToKeyterms[Workspace.termsToKeyterms[string.toLowerCase()]?.term]
                            bundle.putParcelable("Keyterm", keyterm)
                            keyTermLayout.arguments = bundle
                            fragmentActivity?.supportFragmentManager?.beginTransaction()?.replace(R.id.keyterm_info, keyTermLayout)?.addToBackStack("")?.commit()
                        }
                        else {
                            //bundle up the key term to send to new keyterm activity
                            val intent = Intent(fragmentActivity, KeyTermActivity::class.java)
                            val keyterm = Workspace.termsToKeyterms[Workspace.termsToKeyterms[string.toLowerCase()]?.term]
                            intent.putExtra("Keyterm", keyterm)
                            intent.putExtra("Phase", Workspace.activePhase.getName())
                            fragmentActivity?.startActivity(intent)
                        }
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

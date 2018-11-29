package org.sil.storyproducer.controller.keyterm

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewPager
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)
        Workspace.activePhase = Phase(PhaseType.KEYTERM)
        if(intent.hasExtra("Keyterm")) {
            val keyTerm : Keyterm = intent.getParcelableExtra("Keyterm")
            viewPager = findViewById(R.id.viewPager)
            viewPager?.adapter = ViewPagerAdapter(supportFragmentManager, keyTerm)
        }
        if(intent.hasExtra("Phase")) {
            previousPhase = intent.getStringExtra("Phase")
        }

        //set action bar to have back button (android_manifest is where parent is set)
        setSupportActionBar(findViewById(R.id.keyterm_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                Workspace.activePhase.getColor(), null)))

        mediaPlayer = AudioPlayer()
        setupStatusBar()
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
}

fun stringToSpannableString(strings : List<String>, fragmentActivity : FragmentActivity): SpannableStringBuilder {
    val newString = SpannableStringBuilder()
    for(text in strings){
        val tempString = SpannableString(text)
        if(Workspace.termsToKeyterms.containsKey(text)){
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(textView : View) {

                    if(Workspace.activePhase.phaseType == PhaseType.KEYTERM){
                        //bundle up the key term to update fragment if in keyterm view already
                        var keyTermLayout = KeyTermLayout()
                        val bundle = Bundle()
                        bundle.putParcelable("Keyterm", Workspace.termsToKeyterms[Workspace.termsToKeyterms[text]?.term])
                        keyTermLayout.arguments = bundle
                        fragmentActivity.supportFragmentManager.beginTransaction().replace(R.id.keyterm_info, keyTermLayout).addToBackStack("").commit()
                        Workspace.activeKeyterm = Workspace.termsToKeyterms[Workspace.termsToKeyterms[text]?.term]!!
                    }
                    else {
                        //bundle up the key term to send to new keyterm activity
                        val intent = Intent(fragmentActivity, KeyTermActivity::class.java)
                        intent.putExtra("Keyterm", Workspace.termsToKeyterms[Workspace.termsToKeyterms[text]?.term])
                        intent.putExtra("Phase", Workspace.activePhase.getName())
                        fragmentActivity?.startActivity(intent)
                        Workspace.activeKeyterm = Workspace.termsToKeyterms[Workspace.termsToKeyterms[text]?.term]!!
                    }
                }
            }
            tempString.setSpan(clickableSpan, 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            tempString.setSpan(ForegroundColorSpan(Color.BLUE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        newString.append(tempString).append(" ")
    }
    return newString
}
package org.sil.storyproducer.controller.wordlink

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.toolbar.PlayBackRecordingToolbar
import java.util.*

class WordLinkActivity : AppCompatActivity(), PlayBackRecordingToolbar.ToolbarMediaListener {

    // private lateinit var recordingToolbar : WordLinkRecordingToolbar
    private lateinit var displayList : RecordingsListAdapter.RecordingsListModal
    lateinit var bottomSheet: ConstraintLayout
    private val wordLinkHistory: Stack<String> = Stack()

    // TODO Refactor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wordlink)
        Workspace.activePhase = Phase(PhaseType.WORDLINK)
        val term = intent.getStringExtra("ClickedTerm")
        Workspace.activeWordLink = Workspace.termToWordLinkMap[Workspace.termFormToTermMap[term.toLowerCase()]]!!
        wordLinkHistory.push(term)

        setupStatusBar()
        setupToolbar(getApplicationContext())
        // setupBottomSheet()
        setupNoteView()
        // setupRecordingList()

        // Keeps keyboard from automatically popping up on opening activity
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv : FloatArray = floatArrayOf(0.0f,0.0f,0.0f)
            Color.colorToHSV(ContextCompat.getColor(this, Workspace.activePhase.getColor()), hsv)
            hsv[2] *= 0.8f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
    }

    private fun setupToolbar(context: Context){
        val toolbar: androidx.appcompat.widget.Toolbar? = findViewById(R.id.wordlink_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context,
                Workspace.activePhase.getColor())))
    }

//    private fun setupBottomSheet(){
//        bottomSheet = findViewById(R.id.bottom_sheet)
//
//        from(bottomSheet).isFitToContents = false
//        from(bottomSheet).peekHeight = dpToPx(48, this)
//
//        if(Workspace.activeWordLink.wordLinkRecordings.isNotEmpty()){
//            from(bottomSheet).state = STATE_EXPANDED
//        }
//        else {
//            from(bottomSheet).state = STATE_COLLAPSED
//        }
//    }

//    private fun setupRecordingList(){
//        displayList = RecordingsListAdapter.RecordingsListModal(this, recordingToolbar)
//        displayList.embedList(findViewById(android.R.id.content))
//        displayList.setParentFragment(null)
//        displayList.show()
//    }
//
    /**
     * Updates the textViews with the current wordlink information
     */
    private fun setupNoteView() {
        val actionBar = supportActionBar

        actionBar?.title = wordLinkHistory.peek().toUpperCase();

        val wordLinkTitleView = findViewById<TextView>(R.id.wordlink_title)
        var titleText = ""
        if(Workspace.activeWordLink.term.toLowerCase() != wordLinkHistory.peek().toLowerCase()) {
            titleText = Workspace.activeWordLink.term
        }
        // format
        for (termForm in Workspace.activeWordLink.termForms){
            if(termForm != wordLinkHistory.peek()) {
                if (titleText.isNotEmpty()) {
                    titleText += " / $termForm"
                }
                else{
                    titleText = termForm
                }
            }
        }
        if(titleText == ""){
            wordLinkTitleView.visibility = View.GONE
        }
        else {
            wordLinkTitleView.visibility = View.VISIBLE
            wordLinkTitleView.text = titleText
        }

        val explanationView = findViewById<TextView>(R.id.explanation_text)
        explanationView.text = Workspace.activeWordLink.explanation

        val relatedTermsView = findViewById<TextView>(R.id.related_terms_text)
        if (Workspace.activeWordLink.relatedTerms.isEmpty()) {
            relatedTermsView.setText(R.string.wordlink_none);
        } else {
            relatedTermsView.text = Workspace.activeWordLink.relatedTerms.fold(SpannableStringBuilder()) {
                result, relatedTerm -> result.append(stringToWordLink(relatedTerm, this)).append("   ")
            }
            relatedTermsView.movementMethod = LinkMovementMethod.getInstance()
        }

        val alternateRenderingsView = findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = Workspace.activeWordLink.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }.removeSuffix("\n")

//        val bundle = Bundle()
//        bundle.putInt(WORDLINKS_SLIDE_NUM, 0)
//        recordingToolbar = KeytermRecordingToolbar()
//        recordingToolbar.arguments = bundle
//        supportFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()
    }
//
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_wordlink_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.closeWordLink -> {
                // saveKeyterm()
                if(intent.hasExtra(PHASE)) {
                    Workspace.activePhase = Phase(intent.getSerializableExtra(PHASE) as PhaseType)
                }
                finish()
                true
            }
            R.id.helpButton -> {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("${Workspace.activePhase.getPrettyName()} Help")

                val wv = WebView(this)
                val iStream = assets.open(Phase.getHelpName(Workspace.activePhase.phaseType))
                val text = iStream.reader().use {
                    it.readText()
                }
                wv.loadDataWithBaseURL(null,text,"text/html",null,null)
                alert.setView(wv)
                alert.setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
                }
                alert.show()
                true
            }
            else -> {
                onBackPressed()
                true
            }
        }
    }
//
//    override fun onStoppedToolbarRecording() {
//        val recordingExpandableListView = findViewById<RecyclerView>(R.id.recordings_list)
//        recordingExpandableListView.adapter?.notifyDataSetChanged()
//        if(from(bottomSheet).state == STATE_COLLAPSED) {
//            from(bottomSheet).state = STATE_EXPANDED
//        }
//        recordingExpandableListView.smoothScrollToPosition(0)
//    }
//
//    override fun onStartedToolbarMedia() {
//        displayList.stopAudio()
//    }
//
//    override fun onStartedToolbarRecording() {
//        super.onStartedToolbarRecording()
//        displayList.resetRecordingList()
//    }
//
    /**
     * When the back button is pressed, the bottom sheet will close if currently opened or return to
     * the previous keyterm or close the activity if there is no previous keyterm to return to
     */
    override fun onBackPressed() {
//        if( from(bottomSheet).state == STATE_EXPANDED){
//            from(bottomSheet).state = STATE_COLLAPSED
//        }
//        else {
//            saveKeyterm()
//            keytermHistory.pop()
//            if (keytermHistory.isEmpty()) {
//                if(intent.hasExtra(PHASE)) {
//                    Workspace.activePhase = Phase(intent.getSerializableExtra(PHASE) as PhaseType)
//                }
                super.onBackPressed()
                finish()
//            } else {
//                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[keytermHistory.peek().toLowerCase()]]!!
//                setupNoteView()
//                setupRecordingList()
//            }
//        }
    }

    fun replaceActivityWordLink(term: String) {
        // saveWordLink()
        // Set keyterm from link as active keyterm
        Workspace.activeWordLink = Workspace.termToWordLinkMap[Workspace.termFormToTermMap[term.toLowerCase()]]!!
        //Add new keyterm fragments to stack
        wordLinkHistory.push(term)
        setupNoteView()
        //setupRecordingList()
        //from(bottomSheet).state = STATE_COLLAPSED
    }
//
//    /**
//     * Saves the active keyterm to the workspace and exports an up-to-date json file for all keyterms
//     **/
//    private fun saveWordLink () {
//        Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
//        val keytermList = KeytermList(Workspace.termToKeyterm.values.toList())
//        Thread(Runnable{ let { keytermList.toJson(it) } }).start()
//    }
}
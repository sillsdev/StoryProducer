package org.sil.storyproducer.controller.keyterm

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior.*
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat.getColor
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.*
import org.sil.storyproducer.tools.dpToPx
import org.sil.storyproducer.tools.helpDialog
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*

/**
 * This activity shows information about the  active keyterm to the user where they can learn more
 * about the keyterm as well as record an audio translation and give a text backtranslation
 *
 * @since 2.6 Keyterm
 * @author Aaron Cannon and Justin Stallard
 */

class KeyTermActivity : AppCompatActivity(), RecordingToolbar.RecordingListener {

    private lateinit var recordingToolbar : RecordingToolbar
    private lateinit var displayList : RecordingsListAdapter.RecordingsListModal
    lateinit var bottomSheet: ConstraintLayout
    private val keytermHistory: Stack<String> = Stack()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)
        Workspace.activePhase = Phase(PhaseType.KEYTERM)
        val term = intent.getStringExtra("ClickedTerm")
        Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
        keytermHistory.push(term)

        setupStatusBar()

        setupToolbar()

        setupBottomSheet()

        setupNoteView()

        setupRecordingList()

        // Keeps keyboard from automatically popping up on opening activity
        this.window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv : FloatArray = floatArrayOf(0.0f,0.0f,0.0f)
            Color.colorToHSV(ContextCompat.getColor(this, Workspace.activePhase.getColor()), hsv)
            hsv[2] *= 0.8f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
    }

    private fun setupToolbar(){
        val toolbar: android.support.v7.widget.Toolbar = findViewById(R.id.keyterm_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(resources,
                Workspace.activePhase.getColor(), null)))
    }

    private fun setupBottomSheet(){
        bottomSheet = findViewById(R.id.bottom_sheet)

        from(bottomSheet).isFitToContents = false
        from(bottomSheet).peekHeight = dpToPx(48, this)

        if(Workspace.activeKeyterm.keytermRecordings.isNotEmpty()){
            from(bottomSheet).state = STATE_EXPANDED
        }
        else {
            from(bottomSheet).state = STATE_COLLAPSED
        }
    }

    private fun setupRecordingList(){
        displayList = RecordingsListAdapter.RecordingsListModal(this, recordingToolbar)
        displayList.embedList(findViewById(android.R.id.content))
        displayList.setParentFragment(null)
        displayList.show()
    }

    /**
     * Updates the textViews with the current keyterm information
     */
    private fun setupNoteView(){
        val actionBar = supportActionBar

        actionBar?.title = keytermHistory.peek()

        val keyTermTitleView = findViewById<TextView>(R.id.keyterm_title)
        var titleText = ""
        if(Workspace.activeKeyterm.term.toLowerCase() != keytermHistory.peek().toLowerCase()) {
            titleText = Workspace.activeKeyterm.term
        }
        for (termForm in Workspace.activeKeyterm.termForms){
            if(termForm != keytermHistory.peek()) {
                if (titleText.isNotEmpty()) {
                    titleText += " / $termForm"
                }
                else{
                    titleText = termForm
                }
            }
        }
        if(titleText == ""){
            keyTermTitleView.visibility = View.GONE
        }
        else {
            keyTermTitleView.visibility = View.VISIBLE
            keyTermTitleView.text = titleText
        }

        val explanationView = findViewById<TextView>(R.id.explanation_text)
        explanationView.text = Workspace.activeKeyterm.explanation

        val relatedTermsView = findViewById<TextView>(R.id.related_terms_text)
        relatedTermsView.text = Workspace.activeKeyterm.relatedTerms.fold(SpannableStringBuilder()){
            result, relatedTerm -> result.append(stringToKeytermLink(relatedTerm, this)).append("   ")
        }
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        val alternateRenderingsView = findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = Workspace.activeKeyterm.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }.removeSuffix("\n")

        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true, false, true, false))
        bundle.putInt("slideNum", 0)
        recordingToolbar = RecordingToolbar()
        recordingToolbar.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.toolbar_for_recording_toolbar, recordingToolbar).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_keyterm_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.closeKeyterm -> {
                saveKeyterm()
                if(intent.hasExtra(PHASE)) {
                    Workspace.activePhase = Phase(intent.getSerializableExtra(PHASE) as PhaseType)
                }
                finish()
                true
            }
            R.id.helpButton -> {
                helpDialog(this, "${Workspace.activePhase.getPrettyName()} Help").show()
                true
            }
            else -> {
                onBackPressed()
                true
            }
        }
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {
        recordingToolbar.stopToolbarMedia()
        displayList.stopAudio()
    }

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        if(isRecording) {
            val recordingExpandableListView = findViewById<RecyclerView>(R.id.recordings_list)
            recordingExpandableListView.adapter?.notifyItemInserted(0)
            if(from(bottomSheet).state == STATE_COLLAPSED) {
                from(bottomSheet).state = STATE_EXPANDED
            }
            recordingExpandableListView.smoothScrollToPosition(0)
        }
    }

    /**
     * When the back button is pressed, the bottom sheet will close if currently opened or return to
     * the previous keyterm or close the activity if there is no previous keyterm to return to
     */
    override fun onBackPressed() {
        if( from(bottomSheet).state == STATE_EXPANDED){
            from(bottomSheet).state = STATE_COLLAPSED
        }
        else {
            saveKeyterm()
            keytermHistory.pop()
            if (keytermHistory.isEmpty()) {
                if(intent.hasExtra(PHASE)) {
                    Workspace.activePhase = Phase(intent.getSerializableExtra(PHASE) as PhaseType)
                }
                super.onBackPressed()
                finish()
            } else {
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[keytermHistory.peek().toLowerCase()]]!!
                setupNoteView()
                setupRecordingList()
            }
        }
    }
    
    fun replaceActivityKeyterm(term: String){
        saveKeyterm()
        //Set keyterm from link as active keyterm
        Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[term.toLowerCase()]]!!
        //Add new keyterm fragments to stack
        keytermHistory.push(term)
        setupNoteView()
        setupRecordingList()
        from(bottomSheet).state = STATE_COLLAPSED
    }

    /**
     * Saves the active keyterm to the workspace and exports an up-to-date json file for all keyterms
     **/
    private fun saveKeyterm(){
        Workspace.termToKeyterm[Workspace.activeKeyterm.term] = Workspace.activeKeyterm
        val keytermList = KeytermList(Workspace.termToKeyterm.values.toList())
        Thread(Runnable{ let { keytermList.toJson(it) } }).start()
    }
}

package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat.getColor
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.adapter.RecordingsListAdapter
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson
import org.sil.storyproducer.tools.dpToPx
import org.sil.storyproducer.tools.toolbar.RecordingToolbar
import java.util.*

class KeyTermActivity : AppCompatActivity(), RecordingToolbar.RecordingListener {

    private lateinit var recordingExpandableListView: RecyclerView
    private var recordingToolbar : RecordingToolbar = RecordingToolbar()
    var bottomSheet: LinearLayout? = null
    val keytermHistory: Stack<String> = Stack()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term)
        Workspace.activePhase = Phase(PhaseType.KEYTERM)
        keytermHistory.push(intent?.getStringExtra("ClickedTerm"))

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

        BottomSheetBehavior.from(bottomSheet).isFitToContents = false
        BottomSheetBehavior.from(bottomSheet).peekHeight = dpToPx(48, this)

        if(Workspace.activeKeyterm.backTranslations.isNotEmpty()){
            BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
        else {
            BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun setupRecordingList(){
        val viewManager = LinearLayoutManager(this)

        recordingExpandableListView = findViewById(R.id.recording_list)
        recordingExpandableListView.adapter = RecyclerDataAdapter(this, Workspace.activeKeyterm.backTranslations, bottomSheet!!)
        recordingExpandableListView.layoutManager = viewManager
        val displayList : RecordingsListAdapter.RecordingsListModal = RecordingsListAdapter.RecordingsListModal(this, recordingToolbar, recordingExpandableListView)
        displayList.embedList(findViewById(android.R.id.content))
        displayList.show()
    }

    fun setupNoteView(){
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
            result, relatedTerm -> result.append(stringToKeytermLink(this, relatedTerm, this)).append("   ")
        }
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        val alternateRenderingsView = findViewById<TextView>(R.id.alternate_renderings_text)
        alternateRenderingsView.text = Workspace.activeKeyterm.alternateRenderings.fold(""){
            result, alternateRendering -> "$result\u2022 $alternateRendering\n"
        }.removeSuffix("\n")

        val bundle = Bundle()
        bundle.putBooleanArray("buttonEnabled", booleanArrayOf(true, false, true, false))
        bundle.putInt("slideNum", 0)
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
                onBackPressed()
                true
            }
            R.id.helpButton -> {
                val dialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.help))
                        .setMessage("${Workspace.activePhase.getPrettyName()} Help")
                        .create()
                dialog.show()
                true
            }
            else -> {
                onBackPressed()
                true
            }
        }
    }

    override fun onStartedRecordingOrPlayback(isRecording: Boolean) {}

    override fun onStoppedRecordingOrPlayback(isRecording: Boolean) {
        if(isRecording) {
            recordingExpandableListView.adapter?.notifyItemInserted(0)
            if(BottomSheetBehavior.from(bottomSheet).state == BottomSheetBehavior.STATE_COLLAPSED) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
            recordingExpandableListView.smoothScrollToPosition(0)
        }
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

    override fun onBackPressed() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        if( bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED || bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED){
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        else {
            keytermHistory.pop()
            if (keytermHistory.isEmpty()) {
                super.onBackPressed()
                finish()
            } else {
                Workspace.activeKeyterm = Workspace.termToKeyterm[Workspace.termFormToTerm[keytermHistory.peek().toLowerCase()]]!!
                setupNoteView()
                setupRecordingList()
            }
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
                //Add new keyterm fragments to stack
                (fragmentActivity as KeyTermActivity).keytermHistory.push(term)
                (fragmentActivity).setupNoteView()
                (fragmentActivity).setupRecordingList()
                BottomSheetBehavior.from((fragmentActivity).bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
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

            val hasRecording = keyterm?.backTranslations?.isNotEmpty()

            if(hasRecording != null && hasRecording){
                drawState.linkColor = ContextCompat.getColor(context, R.color.lightGray)
            }

            super.updateDrawState(drawState)
        }
    }
}

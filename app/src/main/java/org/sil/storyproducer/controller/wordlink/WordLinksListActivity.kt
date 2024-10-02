
package org.sil.storyproducer.controller.wordlink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.controller.SnackbarManager
import org.sil.storyproducer.model.PHASE
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.WORD_LINKS_CLICKED_TERM
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.Workspace.termToWordLinkMap

/**
 * This activity shows all Word Links, clickable to go to the WordLinksActivity
 */
class WordLinksListActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private lateinit var recyclerView: RecyclerView
    private fun checkNoDatabaseMsg() {
        if (termToWordLinkMap.count() == 0) {

            SnackbarManager.show(findViewById(R.id.drawer_layout),
                getString(R.string.wordlinks_no_database_installed),
                60 * 1000   // display for 60 seconds
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wordlink_list)

        val wordLinkList = termToWordLinkMap.keys.toTypedArray()
        wordLinkList.sortWith(String.CASE_INSENSITIVE_ORDER)

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = WordLinkListAdapter(wordLinkList, this)

        recyclerView = findViewById<RecyclerView>(R.id.wordlink_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        checkNoDatabaseMsg()

        if (mDrawerList == null) {
            setupDrawer()
        }

        supportActionBar?.setTitle(R.string.title_activity_wordlink_list)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_wordlink_list_view, menu)
        val searchItem = menu.findItem(R.id.search_button)
        (searchItem.actionView as SearchView).setOnQueryTextListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if(mDrawerLayout!!.isDrawerOpen(GravityCompat.START)){
                    mDrawerLayout!!.closeDrawer(GravityCompat.START)
                }else{
                    mDrawerLayout!!.openDrawer(GravityCompat.START)
                }
                true
            }
            R.id.helpButton -> {
                // DKH 4/1/2022 - Issue 635: WordLinks list help button does not work.
                // Display help from the HTML file when the help button is clicked at the activity
                // level.  Previously, a "NO Help Available" popup was displayed.
                val wv = WebView(this)  // grab a view that decodes html
                val iStream = assets.open(Phase.getHelpDocFile(PhaseType.WORD_LINKS)) // open help document
                val text = iStream.reader().use {  // Read the file
                    it.readText() }
                // convert the html to text
                wv.loadDataWithBaseURL(null,text,"text/html",null,null)
                // display the help text in a dialog box
                val dialog = android.app.AlertDialog.Builder(this)
                        .setTitle(baseContext.getString(R.string.phase_display_name_word_links) + " " + baseContext.getString(R.string.help))
                        .setView(wv)
                        .setNegativeButton(baseContext.getString(R.string.nav_close)) { dialog, _ ->
                            dialog!!.dismiss()
                        }
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onQueryTextSubmit(p0: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        val newList = ArrayList<String>()
        for (wl in termToWordLinkMap.keys) {
            if (wl.toLowerCase().contains(p0?.toLowerCase() ?: "")) {
                newList.add(wl)
            }
        }
        newList.sortWith(String.CASE_INSENSITIVE_ORDER)
        recyclerView.swapAdapter(WordLinkListAdapter(newList.toTypedArray(), this), true)
        recyclerView.scrollToPosition(0)
        return true
    }
}

class WordLinkListAdapter(private val wordLinkTerms: Array<String>, private val context: Context) : RecyclerView.Adapter<WordLinkListAdapter.WordLinkListViewHolder>() {

    class WordLinkListViewHolder(val item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordLinkListAdapter.WordLinkListViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return WordLinkListViewHolder(rootView)
    }

    override fun onBindViewHolder(wordLinkListViewHolder: WordLinkListViewHolder, position: Int) {
        val term = wordLinkTerms[position]
        wordLinkListViewHolder.item.findViewById<TextView>(android.R.id.text1).text = term
        wordLinkListViewHolder.item.setOnClickListener {
            val intent = Intent(context , WordLinksActivity::class.java)
            intent.putExtra(PHASE, Workspace.activePhase.phaseType)
            intent.putExtra(WORD_LINKS_CLICKED_TERM, term)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = wordLinkTerms.size
}


package org.tyndalebt.spadv.controller.wordlink

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import org.tyndalebt.spadv.R
import org.tyndalebt.spadv.controller.MainActivity
import org.tyndalebt.spadv.controller.RegistrationActivity
import org.tyndalebt.spadv.model.*
import org.tyndalebt.spadv.model.PHASE
import org.tyndalebt.spadv.model.WORD_LINKS_CLICKED_TERM
import org.tyndalebt.spadv.model.Workspace.termToWordLinkMap

/**
 * This activity shows all Word Links, clickable to go to the WordLinksActivity
 */
class WordLinksListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var recyclerView: RecyclerView
    private var mDrawerLayout: DrawerLayout? = null

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

        setupDrawer()

        supportActionBar?.setTitle(R.string.title_activity_wordlink_list)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_wordlink_list_view, menu)
        val searchItem = menu.findItem(R.id.search_button)
        (searchItem.actionView as SearchView).setOnQueryTextListener(this)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout!!.openDrawer(GravityCompat.START)
                true
            }
            R.id.helpButton -> {
                // DKH 3/7/2022 -
                // Display help from the HTML file when the help button is clicked at the activity
                // level.  Previously, a "NO Help Available" popup was displayed.
                val wv = WebView(this)
                val iStream = assets.open(Phase.getHelpDocFile(PhaseType.WORD_LINKS))
                val text = iStream.reader().use {
                    it.readText() }

                wv.loadDataWithBaseURL(null,text,"text/html",null,null)
                val dialog = android.app.AlertDialog.Builder(this)
                        .setTitle("Word Links Help")
                        .setView(wv)
                        .setNegativeButton("Close") { dialog, _ ->
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

    /**
     * initializes the items that the drawer needs
     */
    private fun setupDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout!!.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here
            val intent: Intent
            when (menuItem.itemId) {
                R.id.nav_stories -> {
                    intent = Intent(this, MainActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_word_link_list -> {
                    // Current fragment
                }
                R.id.nav_registration -> {
                    intent = Intent(this, RegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
            }
            true
        }
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

package org.sil.storyproducer.controller.wordlink

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
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.PHASE
import org.sil.storyproducer.model.WORD_LINKS_CLICKED_TERM
import org.sil.storyproducer.model.Workspace.termToWordLinkMap

/**
 * This activity shows all Word Links, clickable to go to the WordLinksActivity
 */
class WordLinksListActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private lateinit var recyclerView: RecyclerView
    private var mDrawerLayout: DrawerLayout? = null
    private var mSnackBar: Snackbar? = null

    private fun checkNoDatabaseMsg() {
        if (termToWordLinkMap.count() == 0) {

            dismissNoDatabaseMsg()

            mSnackBar = Snackbar.make(
                findViewById(R.id.drawer_layout),
                R.string.wordlinks_no_database_installed,
                60 * 1000   // display for 60 seconds
            )
            mSnackBar!!.show()
        }
    }

    private fun dismissNoDatabaseMsg() {
        if (mSnackBar != null)
            mSnackBar!!.dismiss()
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
//            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout!!.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here
            val intent: Intent
            when (menuItem.itemId) {
                R.id.nav_workspace -> {
                    showSelectTemplatesFolderDialog()
                    dismissNoDatabaseMsg()
                }
                R.id.nav_demo -> {
                    Workspace.addDemoToWorkspace(this)
                    controller.updateStories()  // refresh list of stories
                }
                R.id.nav_word_link_list -> {
                    // Current fragment
                    checkNoDatabaseMsg()
                }
//                R.id.nav_more_templates -> {
//                    // DKH - 01/15/2022 Issue #571: Add a menu item for accessing templates from Google Drive
//                    // A new menu item was added that opens a URL for the user to download templates.
//                    // If we get here, the user wants to browse for more templates, so,
//                    // open the URL in a new activity
//                    Workspace.startDownLoadMoreTemplatesActivity(this)
//                    this.finish()
//                }
                R.id.nav_stories -> {
                    finish()  // finish this activity to reveal main activity
                }
                R.id.nav_registration -> {
                    showRegistration(true)
                }
                R.id.nav_bloom_templates -> {
                    showBLDownloadDialog(BLOOM_DL_TEMPLATES_ACTIVITY);
                    this.finish()
                }
                R.id.nav_bloom_featured -> {
                    showBLDownloadDialog(BLOOM_DL_FEATURED_ACTIVITY);
                    this.finish()
                }
                R.id.nav_about -> {
                    showAboutDialog()
                    dismissNoDatabaseMsg()
                }
                R.id.nav_dev_settings -> {
                    showDevSettings()
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

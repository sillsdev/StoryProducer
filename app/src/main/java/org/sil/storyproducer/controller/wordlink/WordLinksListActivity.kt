
package org.sil.storyproducer.controller.wordlink

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.model.WORD_LINKS_CLICKED_TERM
import org.sil.storyproducer.model.PHASE
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.Workspace.termToWordLinkMap

class WordLinkListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

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
                val alert = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.help))
                        .setMessage(R.string.wordlink_list_help)
                        .create()
                alert.show()
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
                R.id.nav_workspace -> {
                    intent = Intent(this, RegistrationActivity.WorkspaceUpdateActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
//                R.id.nav_license -> {
//                    val dialog = AlertDialog.Builder(this)
//                            .setTitle(this.getString(R.string.license_title))
//                            .setMessage(this.getString(R.string.license_body))
//                            .setPositiveButton(this.getString(R.string.ok)) { _, _ -> }.create()
//                    dialog.show()
//                }
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
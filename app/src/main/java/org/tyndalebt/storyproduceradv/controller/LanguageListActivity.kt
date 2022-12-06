
package org.tyndalebt.storyproduceradv.controller

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
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.controller.MainActivity
import org.tyndalebt.storyproduceradv.controller.RegistrationActivity
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.model.LANGUAGE_CLICKED
import org.tyndalebt.storyproduceradv.model.Workspace.LanguageToTextMap
import org.tyndalebt.storyproduceradv.tools.file.goToURL

/**
 * This activity shows all Word Links, clickable to go to the WordLinksActivity
 */
class LanguageListActivity : BaseActivity(), SearchView.OnQueryTextListener {

    private lateinit var recyclerView: RecyclerView
    private var mDrawerLayout: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.language_list_container)

        val languageList = LanguageToTextMap.keys.toTypedArray()
        languageList.sortWith(String.CASE_INSENSITIVE_ORDER)

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = LanguageListAdapter(languageList, this)

        recyclerView = findViewById<RecyclerView>(R.id.language_list_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        setupDrawer()

        supportActionBar?.setTitle("")
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
        for (wl in LanguageToTextMap.keys) {
            if (wl.toLowerCase().contains(p0?.toLowerCase() ?: "")) {
                newList.add(wl)
            }
        }
        newList.sortWith(String.CASE_INSENSITIVE_ORDER)
        recyclerView.swapAdapter(LanguageListAdapter(newList.toTypedArray(), this), true)
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
            when (menuItem.itemId) {
                R.id.nav_workspace -> {
                    showSelectTemplatesFolderDialog()
                }
                R.id.nav_word_link_list -> {
                    // Current fragment
                }
                R.id.nav_more_templates -> {
                    Workspace.startDownLoadMoreTemplatesActivity(this)
                }
                R.id.nav_spadv_website -> {
                    goToURL(this, Workspace.URL_FOR_WEBSITE)
                }
                R.id.nav_stories -> {
                    intent = Intent(this, MainActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_registration -> {
                    intent = Intent(this, RegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_about -> {
                    showAboutDialog()
                }
            }
            true
        }
    }
}

class LanguageListAdapter(private val LanguageTerms: Array<String>, private val context: Context) : RecyclerView.Adapter<LanguageListAdapter.LanguageListViewHolder>() {

    class LanguageListViewHolder(val item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageListAdapter.LanguageListViewHolder {
        val rootView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return LanguageListViewHolder(rootView)
    }

    override fun onBindViewHolder(LanguageListViewHolder: LanguageListViewHolder, position: Int) {
        val term = LanguageTerms[position]
        LanguageListViewHolder.item.findViewById<TextView>(android.R.id.text1).text = term
        LanguageListViewHolder.item.setOnClickListener {
            val intent = Intent(context , LanguageListActivity::class.java)
            intent.putExtra(PHASE, Workspace.activePhase.phaseType)
            intent.putExtra(LANGUAGE_CLICKED, term)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = LanguageTerms.size
}
package org.sil.storyproducer.controller.keyterm

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.MainActivity
import org.sil.storyproducer.controller.RegistrationActivity
import org.sil.storyproducer.controller.WorkspaceAndRegistrationActivity
import org.sil.storyproducer.model.CLICKED_TERM
import org.sil.storyproducer.model.PHASE
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.Workspace.termToKeyterm

class KeyTermListActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private lateinit var recyclerView: RecyclerView

    private var mDrawerLayout: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term_list)

        val keytermList = termToKeyterm.keys.toTypedArray()
        keytermList.sortWith(String.CASE_INSENSITIVE_ORDER)

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = KeytermListAdapter(keytermList, this)

        recyclerView = findViewById<RecyclerView>(R.id.keyterm_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        setupDrawer()

        supportActionBar?.title = "Keyterm List"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_keyterm_list_view, menu)
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
                        .setMessage("Keyterm List Help")
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
        for (keyterm in termToKeyterm.keys) {
            if (keyterm.toLowerCase().contains(p0?.toLowerCase() ?: "")) {
                newList.add(keyterm)
            }
        }
        newList.sortWith(String.CASE_INSENSITIVE_ORDER)
        recyclerView.swapAdapter(KeytermListAdapter(newList.toTypedArray(), this), true)
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
                R.id.nav_workspace -> {
                    intent = Intent(this, WorkspaceAndRegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_stories -> {
                    intent = Intent(this, MainActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_keyterm_list -> {
                    // Current fragment
                }
                R.id.nav_registration -> {
                    intent = Intent(this, RegistrationActivity::class.java)
                    this.startActivity(intent)
                    this.finish()
                }
                R.id.nav_license -> {
                    val dialog = AlertDialog.Builder(this)
                            .setTitle(this.getString(R.string.license_title))
                            .setMessage(this.getString(R.string.license_body))
                            .setPositiveButton(this.getString(R.string.ok)) { _, _ -> }.create()
                    dialog.show()
                }
            }
            true
        }
    }
}

class KeytermListAdapter(private val keytermTerms: Array<String>, private val context: Context) : RecyclerView.Adapter<KeytermListAdapter.KeytermListViewHolder>() {

    class KeytermListViewHolder(val item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeytermListAdapter.KeytermListViewHolder {

        val rootView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)

        return KeytermListViewHolder(rootView)
    }

    override fun onBindViewHolder(keytermListViewHolder: KeytermListViewHolder, position: Int) {
        val term = keytermTerms[position]
        keytermListViewHolder.item.findViewById<TextView>(android.R.id.text1).text = term
        keytermListViewHolder.item.setOnClickListener {
            val intent = Intent(context , KeyTermActivity::class.java)
            intent.putExtra(PHASE, Workspace.activePhase.phaseType)
            intent.putExtra(CLICKED_TERM, term)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = keytermTerms.size
}



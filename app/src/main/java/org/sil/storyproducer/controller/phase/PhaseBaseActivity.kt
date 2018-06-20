package org.sil.storyproducer.controller.phase

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner

import org.sil.storyproducer.R
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.emptyStory
import org.sil.storyproducer.tools.DrawerItemClickListener
import org.sil.storyproducer.tools.PhaseGestureListener
import org.sil.storyproducer.tools.PhaseMenuItemListener

abstract class PhaseBaseActivity : AppCompatActivity() {
    private var mDetector: GestureDetectorCompat? = null
    private var mDrawerList: ListView? = null
    private var mAdapter: ArrayAdapter<String>? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mDrawerLayout: DrawerLayout? = null

    protected var phase: Phase = Workspace.activePhase
    protected var story: Story = Workspace.activeStory ?: emptyStory()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.phase_frame)

        //keeps the screen from going to sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mActionBarToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mActionBarToolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                phase.getColor(), null)))

        mDetector = GestureDetectorCompat(this, PhaseGestureListener(this))

        setupDrawer()
    }

    //Override setContentView to coerce into child view.
    override fun setContentView(id: Int) {
        val inflater = layoutInflater
        inflater.inflate(id, mDrawerLayout)
        //Bring menu to front again.
        mDrawerList!!.bringToFront()
    }

    /**
     * sets the Menu spinner_item object
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_phases, menu)

        val item = menu.findItem(R.id.spinner)
        val spinner = MenuItemCompat.getActionView(item) as Spinner
        spinner.onItemSelectedListener = PhaseMenuItemListener(this)
        val adapter: ArrayAdapter<CharSequence>
        if (Workspace.registration.getBoolean("isRemote",false)) {
            //remote
            adapter = ArrayAdapter.createFromResource(this,
                    R.array.remote_phases_menu_array, android.R.layout.simple_spinner_item)
        } else {
            //local
            adapter = ArrayAdapter.createFromResource(this,
                    R.array.local_phases_menu_array, android.R.layout.simple_spinner_item)
        }
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.setSelection(Workspace.getPhaseIndex())
        return true
    }

    /**
     * get the touch event so that it can be passed on to GestureDetector
     * @param event the MotionEvent
     * @return the super version of the function
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        mDetector!!.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()                                  //needed to make the drawer synced
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle!!.onConfigurationChanged(newConfig)            //needed to make the drawer synced
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return mDrawerToggle!!.onOptionsItemSelected(item)
    }

    /**
     * initializes the items that the drawer needs
     */
    private fun setupDrawer() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        mDrawerList = findViewById(R.id.navList)
        mDrawerList!!.bringToFront()
        mDrawerLayout = findViewById(R.id.drawer_layout)
        addDrawerItems()
        mDrawerList!!.onItemClickListener = DrawerItemClickListener(this)
        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.nav_open, R.string.nav_close) {

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View?) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
        mDrawerToggle!!.isDrawerIndicatorEnabled = true
        mDrawerLayout!!.addDrawerListener(mDrawerToggle!!)
    }

    /**
     * adds the items to the drawer from the array resources
     */
    private fun addDrawerItems() {
        val menuArray = resources.getStringArray(R.array.global_menu_array)
        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuArray)
        mDrawerList!!.adapter = mAdapter
    }

    fun jumpToPhase(newPhase: Phase) {
        if(newPhase == phase) return
        Workspace.activePhase = newPhase
        val intent = Intent(this.applicationContext, newPhase.getTheClass())
        intent.putExtra("storyname", Workspace.activeStory?.title ?: "no active story?")
        startActivity(intent)
        finish()
    }

    /**
     * starts the previous phase and starts that activity
     */
    fun startPrevActivity() {
        if (Workspace.goToNextPhase()) {
            jumpToPhase(Workspace.activePhase)
            overridePendingTransition(R.anim.enter_down, R.anim.exit_down)
        }
    }

    /**
     * starts the next phase and starts that activity
     */
    fun startNextActivity() {
        if (Workspace.goToNextPhase()) {
            jumpToPhase(Workspace.activePhase)
            overridePendingTransition(R.anim.enter_up, R.anim.exit_up)
        }
    }

    companion object {

        fun disableViewAndChildren(view: View) {
            view.isEnabled = false
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    disableViewAndChildren(child)
                }
            }
        }
    }

}

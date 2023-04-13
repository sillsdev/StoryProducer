package org.tyndalebt.storyproduceradv.controller.phase

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.GravityCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.BaseActivity
import org.tyndalebt.storyproduceradv.controller.adapter.PhaseList
import org.tyndalebt.storyproduceradv.controller.adapter.PhaseObject
import org.tyndalebt.storyproduceradv.controller.adapter.PhaseSpinnerAdapter
import org.tyndalebt.storyproduceradv.model.*
import org.tyndalebt.storyproduceradv.service.SlideService
import org.tyndalebt.storyproduceradv.tools.BitmapScaler
import org.tyndalebt.storyproduceradv.tools.DrawerItemClickListener
import org.tyndalebt.storyproduceradv.tools.PhaseGestureListener
import org.tyndalebt.storyproduceradv.tools.file.genDefaultImage
import org.tyndalebt.storyproduceradv.tools.file.getStoryImage
import org.tyndalebt.storyproduceradv.viewmodel.SlideViewModelBuilder
import kotlin.math.max

abstract class PhaseBaseActivity : BaseActivity(), AdapterView.OnItemSelectedListener {

    lateinit var slideService: SlideService

    private var mDetector: GestureDetectorCompat? = null
    private var mDrawerList: ListView? = null
    private var mAdapter: ArrayAdapter<String>? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mDrawerLayout: androidx.drawerlayout.widget.DrawerLayout? = null
    private var pView: ListView? = null
    protected var phase: Phase = Workspace.activePhase
    protected var story: Story = Workspace.activeStory
    lateinit var selectedPhase: PhaseObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.phase_frame)
        slideService = SlideService(this)

        val mActionBarToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mActionBarToolbar)
        supportActionBar?.title = ""
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                phase.getColor(), null)))
        pView = findViewById<View>(R.id.navList) as ListView

        mDetector = GestureDetectorCompat(this, PhaseGestureListener(this))

        setupDrawer()
        setupStatusBar()
    }

    //Do nothing if back button is pressed on the phone
    override fun onBackPressed() {
        updateView()
    }


    override fun onPause(){
        story.lastSlideNum = Workspace.activeSlideNum
        story.lastPhaseType = Workspace.activePhase.phaseType

        if (Workspace.LastActivityEvent == "Start" || Workspace.LastActivityEvent == "Resume") {
            // Issue #503, it is possible for the user to change workspaces causing a rogue story
            // to save. Instead, ensure that the story exists in the current workspace before saving.
            if (Workspace.Stories.contains(story)) {
                Thread(Runnable { story.toJson(this) }).start()
            }
        }
        Workspace.LastActivityEvent = "Pause"
        super.onPause()
    }

    override fun onStart() {
        Workspace.LastActivityEvent = "Start"
        super.onStart()
    }

    override fun onResume() {
        Workspace.LastActivityEvent = "Resume"
        super.onResume()
    }

    override fun onStop() {
        Workspace.LastActivityEvent = "Stop"
        super.onStop()
    }

    //Override setContentView to coerce into child view.
    override fun setContentView(id: Int) {
        val inflater = layoutInflater
        inflater.inflate(id, mDrawerLayout)
        //Bring menu to front again.
        mDrawerList!!.bringToFront()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.getItem(0)
        item.setIcon(phase.getIcon())
        return true
    }

    /**
     * sets the Menu spinner_item object
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_phases, menu)

        val item = menu.findItem(R.id.spinner)
        val spinner = item.actionView as Spinner
        val pList = PhaseList()
        spinner.adapter = PhaseSpinnerAdapter(this, pList.getPhaseList(spinner.context))
/*
        val adapter = if (Workspace.registration.getBoolean("isRemote",false)) {
            //remote
            ArrayAdapter.createFromResource(this,
                    R.array.remote_phases_menu_array, android.R.layout.simple_spinner_item)
        } else {
            //local
            ArrayAdapter.createFromResource(this,
                    R.array.local_phases_menu_array, android.R.layout.simple_spinner_item)
        }
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
 */
        spinner.setPopupBackgroundResource(R.color.darkGray)

        //Set the selection before setting the listener.  If flipped, the listener would be called
        //when initializing and cause bad things.
        spinner.setSelection(Workspace.activePhaseIndex)
        spinner.onItemSelectedListener = this
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View,
                                pos: Int, id: Long) {
        if(pos >= 0 && pos < Workspace.phases.size){
            jumpToPhase(Workspace.phases[pos])
        }else{
            FirebaseCrashlytics.getInstance().log("tyring to select phase index $pos that is out of bounds:${Workspace.phases.size}")
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
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
        return when (item.itemId) {
            android.R.id.home -> {
                if(mDrawerLayout!!.isDrawerOpen(GravityCompat.START)){
                    mDrawerLayout!!.closeDrawer(GravityCompat.START)
                }else{
                    mDrawerLayout!!.openDrawer(GravityCompat.START)
                }
                true
            }
            R.id.spinner -> {
                mDrawerToggle!!.onOptionsItemSelected(item)
            }
            R.id.helpButton -> {
                val alert = AlertDialog.Builder(this)
                //alert.setTitle("${Workspace.activePhase.getLangDisplayName(this)} ${resources.getString(R.string.help)}\n")

                val wv = WebView(this)
                val iStream = Phase.openHelpDocFile(Workspace.activePhase.phaseType, Workspace.activeStory.language,this)
                val text = iStream.reader().use {
                    it.readText() }

                wv.loadDataWithBaseURL(null,text,"text/html",null,null)
                alert.setView(wv)
                alert.setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
                }
                alert.show()
                true
            }
            else -> mDrawerToggle!!.onOptionsItemSelected(item)
        }
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
        //Lock from opening with left swipe
        mDrawerLayout!!.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        addDrawerItems()
        mDrawerList!!.onItemClickListener = DrawerItemClickListener(this)
        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.nav_open, R.string.nav_close) {

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
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
        menuArray[0] = getString(R.string.title_activity_story_templates)
        menuArray[1] = getString(R.string.update_registration)
        menuArray[2] = getString(R.string.more_templates)
        menuArray[3] = getString(R.string.title_activity_wordlink_list)
        menuArray[4] = getString(R.string.update_workspace)
        menuArray[5] = getString(R.string.change_language)
        menuArray[6] = getString(R.string.spadv_website)
        menuArray[7] = getString(R.string.about)

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuArray)
        mDrawerList!!.adapter = mAdapter
    }

    fun jumpToPhase(newPhase: Phase) {
        if(newPhase.phaseType == phase.phaseType) return
        Workspace.activePhase = newPhase
        val intent = Intent(this.applicationContext, newPhase.getTheClass())
        intent.putExtra("storyname", Workspace.activeStory.title)
        startActivity(intent)
        finish()
    }

    /**
     * starts the previous phase and starts that activity
     */
    fun startPrevActivity() {
        if (Workspace.goToPreviousPhase()) {
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

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv : FloatArray = floatArrayOf(0.0f,0.0f,0.0f)
            Color.colorToHSV(ContextCompat.getColor(this, Workspace.activePhase.getColor()), hsv)
            hsv[2] *= 0.8f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param slideImage    The ImageView that will contain the picture.
     * @param slideNum The slide number to grab the picture from the files.
     */
    fun setPic(slideImage: ImageView, slideNum: Int) {
        val downSample = 2
        var slidePicture: Bitmap = slideService.getImage(slideNum, downSample, story)
        //scale down image to not crash phone from memory error from displaying too large an image
        //Get the height of the phone.
        val phoneProperties = this.resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()
        val width = phoneProperties.widthPixels

        //scale bitmap
        slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)

        //draw the text overlay
        slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
        val canvas = Canvas(slidePicture)
        //only show the untranslated title in the Learn phase.
        val slideViewModel = SlideViewModelBuilder(Workspace.activeStory.slides[slideNum]).build()
        val tOverlay = slideViewModel.overlayText
        //if overlay is null, it will not write the text.
        tOverlay?.setPadding(max(20, 20 + (canvas.width - phoneProperties.widthPixels) / 2))
        tOverlay?.draw(canvas)

        //Set the height of the image view
        slideImage.requestLayout()

        slideImage.setImageBitmap(slidePicture)
    }

    companion object {

        /**
         * This function allows the picture to scale with the phone's screen size.
         *
         * @param slideImage    The ImageView that will contain the picture.
         * @param slideNumber The slide number to grab the picture from the files.
         */
        fun setPic(context: Context, slideImage: ImageView, slideNumber: Int) {
            val downSample = 2
            var slidePicture: Bitmap = getStoryImage(context, slideNumber, downSample)
                ?: genDefaultImage()

            if (slideNumber < Workspace.activeStory.slides.size) {
                //scale down image to not crash phone from memory error from displaying too large an image
                //Get the height of the phone.
                val scalingFactor = 0.4
                val height = (context.resources.displayMetrics.heightPixels * scalingFactor).toInt()
                val width = context.resources.displayMetrics.widthPixels

                slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)
                slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
                val canvas = Canvas(slidePicture)
                //only show the untranslated title in the Learn phase.
                val tOverlay = Workspace.activeStory.slides[slideNumber]
                    .getOverlayText(false, Workspace.activeStory.lastPhaseType == PhaseType.LEARN)
                //if overlay is null, it will not write the text.
                tOverlay?.setPadding(max(20, 20 + (canvas.width - width) / 2))
                tOverlay?.draw(canvas)
            }
            //Set the height of the image view
            slideImage.requestLayout()

            slideImage.setImageBitmap(slidePicture)
        }

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

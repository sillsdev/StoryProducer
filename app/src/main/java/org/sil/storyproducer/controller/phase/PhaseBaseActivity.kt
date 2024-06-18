package org.sil.storyproducer.controller.phase

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.GravityCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sil.storyproducer.R
import org.sil.storyproducer.activities.BaseActivity
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson
import org.sil.storyproducer.service.SlideService
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.PhaseGestureListener
import org.sil.storyproducer.viewmodel.SlideViewModelBuilder
import kotlin.math.max

abstract class PhaseBaseActivity : BaseActivity(), AdapterView.OnItemSelectedListener {

    lateinit var slideService: SlideService

    private var mDetector: GestureDetectorCompat? = null

    protected var phase: Phase = Workspace.activePhase
    protected var story: Story = Workspace.activeStory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.phase_frame)
        slideService = SlideService(this)

        val mActionBarToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mActionBarToolbar)
        supportActionBar?.title = ""
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                phase.getColor(), null)))

        mDetector = GestureDetectorCompat(this, PhaseGestureListener(this))

        if (mDrawerList == null) {
            setupDrawer()
            setupStatusBar()
        }
    }

    override fun onPause(){
        super.onPause()
        story.lastSlideNum = Workspace.activeSlideNum
        story.lastPhaseType = Workspace.activePhase.phaseType

        // Issue #503, it is possible for the user to change workspaces causing a rouge story
        // to save. Instead, ensure that the story exists in the current workspace before saving.
        if(Workspace.Stories.contains(story)) {
            Thread(Runnable { story.toJson(this) }).start()
        }
    }

    //Override setContentView to coerce into child view.
    override fun setContentView(id: Int) {
        val inflater = layoutInflater
        rootView = inflater.inflate(id, mDrawerLayout)
        //Bring menu to front again.
        mDrawerList!!.bringToFront()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
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
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_phases, menu)

        val item = menu.findItem(R.id.spinner)
        val spinner = item.actionView as Spinner
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


    override fun showDetailedHelp() {
        super.showDetailedHelp()

        val alert = AlertDialog.Builder(this)
        alert.setTitle("${Workspace.activePhase.getDisplayName()} Help")    // TODO: LOCALIZATION: Move this text to strings.xml resource

        val wv = WebView(this)
        val iStream = assets.open(Phase.getHelpDocFile(Workspace.activePhase.phaseType))
        val text = iStream.reader().use {
            it.readText() }

        wv.loadDataWithBaseURL(null,text,"text/html",null,null)
        alert.setView(wv)
        alert.setNegativeButton("Close") { dialog, _ -> // TODO: LOCALIZATION: Move this text to strings.xml resource
            dialog!!.dismiss()
        }
        alert.show()

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
                showHelpContextMenu()
                true
            }
            R.id.helpResumeButton -> {
                resumeShowingPopupHelp()
                checkDownloadStoriesMessage()
                true
            }
            else -> mDrawerToggle!!.onOptionsItemSelected(item)
        }
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
        if (slideNum >= Workspace.activeStory.slides.size)
            return  // workaround a crash if called when not fully initialized
        val downSample = 2
        var slidePicture: Bitmap = slideService.getImage(slideNum, downSample, story)
        //scale down image to not crash phone from memory error from displaying too large an image
        //Get the height of the phone.
        val phoneProperties = this.resources.displayMetrics
        val width = phoneProperties.widthPixels
        val desiredAspectRatio = slideService.getVideoScreenRatio(false)
        val height = (width / desiredAspectRatio).toInt()

        if (slideService.shouldScaleForAspectRatio(slidePicture.width, slidePicture.height, desiredAspectRatio)) {
            // Create a new bitmap with the desired aspect ratio
            val newBitmap = slideService.scaleImage(slidePicture, width, height, false)
            slidePicture = newBitmap.copy(Bitmap.Config.RGB_565, true)
        } else {
            // nearly the same ratio so use the current bitmap after centre crop
             slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)
             slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
        }

        //draw the text overlay
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

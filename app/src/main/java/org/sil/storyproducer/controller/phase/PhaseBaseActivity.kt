package org.sil.storyproducer.controller.phase

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.handleDrawerItemSelection
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.toJson
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.genDefaultImage
import org.sil.storyproducer.tools.file.getStoryImage
import kotlin.math.max

class PhaseBaseActivity : AppCompatActivity() {


    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mViewPager: ViewPager
    private var spinner: Spinner? = null

    private var story: Story = Workspace.activeStory
    private var isSettingSpinnerProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.phase_frame)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar: ActionBar = supportActionBar!!
        actionbar.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        supportActionBar?.title = ""
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                Workspace.activePhase.getColor(), null)))

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val drawerLayout = mDrawerLayout
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerItemSelection(this, menuItem, drawerLayout, null)
        }
        setupStatusBar()

        val pagerAdapter = PagerAdapter(supportFragmentManager!!)
        mViewPager = findViewById(R.id.phase_pager)
        mViewPager.adapter = pagerAdapter
        mViewPager.currentItem = Workspace.activePhaseIndex
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                Workspace.activePhaseIndex = position
                spinner?.setSelection(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }


    override fun onPause() {
        super.onPause()
        story.lastSlideNum = Workspace.activeSlideNum
        story.lastPhaseType = Workspace.activePhase
        Thread(Runnable { story.toJson(this) }).start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_phases, menu)

        val newSpinner = menu.findItem(R.id.spinner).actionView as Spinner
        val phaseArrayResource = if (Workspace.registration.consultantLocationType == "Remote") {
            R.array.remote_phases_menu_array
        } else {
            R.array.local_phases_menu_array
        }
        val adapter = ArrayAdapter.createFromResource(this, phaseArrayResource, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        newSpinner.adapter = adapter
        // Set selection before listener so that the listener is not triggered at first.
        newSpinner.setSelection(Workspace.activePhaseIndex)
        newSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.e("@pwhite", "switching to stage $position")
                if (!isSettingSpinnerProgrammatically) {
                    mViewPager.currentItem = position
                } else {
                    isSettingSpinnerProgrammatically = false
                }
                val item = menu.getItem(0)
                item.setIcon(Workspace.activePhase.getIcon())
                supportActionBar?.setBackgroundDrawable(ColorDrawable(ResourcesCompat.getColor(resources,
                        Workspace.activePhase.getColor(), null)))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner = newSpinner

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            R.id.helpButton -> {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("${Workspace.activePhase.getPrettyName()} Help")

                val wv = WebView(this)
                val iStream = assets.open(PhaseType.getHelpName(Workspace.activePhase))
                val text = iStream.reader().use {
                    it.readText()
                }

                wv.loadData(text, "text/html", null)
                alert.setView(wv)
                alert.setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
                }
                alert.show()
                true
            }
            else -> true
        }
    }

    private fun setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val hsv: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
            Color.colorToHSV(ContextCompat.getColor(this, Workspace.activePhase.getColor()), hsv)
            hsv[2] *= 0.8f
            window.statusBarColor = Color.HSVToColor(hsv)
        }
    }


    companion object {
        /**
         * This function allows the picture to scale with the phone's screen size.
         *
         * @param slideImage    The ImageView that will contain the picture.
         * @param slideNum The slide number to grab the picture from the files.
         */
        fun setPic(context: Context, slideImage: ImageView, slideNum: Int) {
            val downSample = 2
            var slidePicture: Bitmap = getStoryImage(context, slideNum, downSample)
                    ?: genDefaultImage()

            if (slideNum < Workspace.activeStory.slides.size) {
                //scale down image to not crash phone from memory error from displaying too large an image
                //Get the height of the phone.
                val scalingFactor = 0.4
                val height = (context.resources.displayMetrics.heightPixels * scalingFactor).toInt()
                val width = context.resources.displayMetrics.widthPixels

                slidePicture = BitmapScaler.centerCrop(slidePicture, height, width)
                slidePicture = slidePicture.copy(Bitmap.Config.RGB_565, true)
                val canvas = Canvas(slidePicture)
                //only show the untranslated title in the Learn phase.
                val tOverlay = Workspace.activeStory.slides[slideNum]
                        .getOverlayText(false, Workspace.activePhase == PhaseType.LEARN)
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

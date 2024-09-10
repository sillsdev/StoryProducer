package org.sil.storyproducer.controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import org.sil.storyproducer.R
import kotlin.concurrent.fixedRateTimer
import kotlin.math.min

public object SnackbarManager {

    private var currentSnackbar: Snackbar? = null

    fun show(view: View, message: String, duration: Int, maxLines: Int = 0) : Snackbar? {
        // Dismiss the current Snackbar if it is showing
        currentSnackbar?.dismiss()

        // Create and show the new Snackbar
        currentSnackbar = Snackbar.make(view, message, duration)
        if (currentSnackbar != null) {
            if (maxLines > 0) {
                val snackTextView =
                    currentSnackbar?.view?.findViewById(com.google.android.material.R.id.snackbar_text) as TextView?
                snackTextView?.maxLines = maxLines
            }
            currentSnackbar?.show()
        }
        return currentSnackbar
    }

    fun dismissCurrentSnackbar() {
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    fun saveNewSnackbar(snackbar: Snackbar) {
        dismissCurrentSnackbar()
        currentSnackbar = snackbar
    }
}


class PopupItem(val anchorViewId: Int,
                val percentX: Int,
                val percentY: Int,
                val titleResId: Int,
                val bodyResId: Int,
                val isTaskAccomplished: (() -> Boolean)? = null) {
}
class PopupHelpUtils(private val parent: Any,
                     private val helpSeriesIndex: Int = 0)  // used to allow two or more help sequences for this fragment/activity
{


    private var helpPopupWindow: PopupWindow? = null
    private var aboutToShowHelpPopup = false
    private var popupItems: MutableList<PopupItem> = mutableListOf()
    private var context: Context? = null
    private var activity: ComponentActivity? = null
    init {
        if (parent is Fragment) {
            activity = parent.requireActivity()
        } else if (parent is AppCompatActivity) {
            activity = parent
        }
        if (activity != null)
            context = activity?.applicationContext
    }

    fun dismissPopup() {
        aboutToShowHelpPopup = false
        if (helpPopupWindow != null) {
            helpPopupWindow?.dismiss()
            helpPopupWindow = null
        }
    }

    fun isShowingPopupWindow() : Boolean {
        return aboutToShowHelpPopup
    }

    private fun getDerivedClassName(obj: Any): String {
        var clazz: Class<*>? = obj::class.java
        while (clazz?.enclosingClass != null) {
            clazz = clazz.enclosingClass
        }
        return clazz?.simpleName ?: "Unknown"
    }

    private var currentHelpIndex = 0
        get () {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val activityClassName = getDerivedClassName(parent)
            val prefString = "PopupHelpGroup_${activityClassName}_${helpSeriesIndex}"
            field = prefs.getInt(prefString, 0)
            return field
        }
        set(value) {
            if (field != value) {
                val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                val activityClassName = getDerivedClassName(parent)
                val prefString = "PopupHelpGroup_${activityClassName}_${helpSeriesIndex}"
                preferencesEditor.putInt(prefString, value)
                preferencesEditor.commit()
                field = value
            }
        }

    private var globalCancelCount = 0
        get () {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val prefString = "PopupHelpGroup_cancelCount"
            field = prefs.getInt(prefString, 0)
            return field
        }
        set(value) {
            if (field != value) {
                val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                val prefString = "PopupHelpGroup_cancelCount"
                preferencesEditor.putInt(prefString, value)
                preferencesEditor.commit()
                field = value
            }
        }

        enum class CompassPoint {
        NO_COMPASS_POINT,
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    fun addPopupHelpItem(anchorViewId: Int,
                    percentX: Int,
                    percentY: Int,
                    titleResId: Int,
                    bodyResId: Int,
                    isTaskAccomplished: (() -> Boolean)? = null) {

        val newPopup = PopupItem(anchorViewId, percentX, percentY, titleResId, bodyResId, isTaskAccomplished)
        popupItems.add(newPopup)
    }

    fun stopShowingPopupHelp() {
        if (currentHelpIndex >= 0)
            currentHelpIndex = -currentHelpIndex -1   // Stop showing popup help for the current Phase/Activity (make it negative)
    }

    fun stopShowingAllHelp() {
        dismissPopup()
        globalCancelCount += 2 // increment global cancel count

        val rootView = activity?.findViewById<View>(R.id.drawer_layout)
        if (rootView != null) {
            SnackbarManager.show(
                rootView,
                context!!.getString(R.string.help_tutor_dismiss_message),
                4 * 1000,   // display for 4 seconds
                3
            )
        }
        stopShowingPopupHelp()
    }

    fun restartShowingPopupHelp() {
        val prefString = "PopupHelpGroup_"
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // Use the SharedPreferences editor to remove the preferences within the subgroup
        val editor = sharedPreferences.edit()
        val preferences = sharedPreferences.all
        for ((key, _) in preferences) {
            if (key.startsWith(prefString)) {
                editor.remove(key)
            }
        }
        // Apply the changes
        editor.apply()

        showNextPopupHelp()
    }

    fun resumeShowingPopupHelp() {
        if (currentHelpIndex < 0)
            currentHelpIndex = -(currentHelpIndex + 1)   // Resume showing where we last stopped

        if (currentHelpIndex >= popupItems.size && popupItems.size > 0)
            currentHelpIndex = popupItems.size - 1  // show the last popup help in this series

        globalCancelCount = 0
        if (!showNextPopupHelp()) {
            Toast.makeText(activity, activity?.getString(R.string.help_tutor_no_more_pages), Toast.LENGTH_LONG).show()
        }
    }

    fun showNextPopupHelp() :Boolean {

        if (globalCancelCount >= 2)
            return false  // help has been cancelled twice - don't show any more by default

        if (currentHelpIndex < 0)
            return false  // help has been turned off

        if (currentHelpIndex >= popupItems.size)
            return false  // we have shown all our help

        if (context == null || activity == null)
            return false

        val popupItem = popupItems[currentHelpIndex]
        var showNextGrayed = false
        if ((popupItem.isTaskAccomplished != null) && !popupItem.isTaskAccomplished.invoke())
             showNextGrayed = true
        aboutToShowHelpPopup = true
        val view = activity?.findViewById<View>(popupItem.anchorViewId)
        if (view != null) {
            view.post {
                if (!aboutToShowHelpPopup)
                    return@post // return immediately if popup was closed by a onPause() call to dismissPopup()
                // get the view again in case it has gone stale (fixes a crash/display bug)
                val view2 = activity?.findViewById<View>(popupItem.anchorViewId)
                if (view2 != null) {
                    if (helpPopupWindow != null)
                        helpPopupWindow?.dismiss()
                    var compassHint = CompassPoint.NO_COMPASS_POINT
                    var targetPercentX = popupItem.percentX
                    var targetPercentY = popupItem.percentY
                    if (popupItem.percentY < 0) {
                        targetPercentY = -popupItem.percentY
                        compassHint = CompassPoint.NORTH
                    } else if (targetPercentY in 0..50)
                        compassHint = CompassPoint.SOUTH
                    else if (targetPercentY > 50)
                        compassHint = CompassPoint.NORTH
                    else
                        compassHint = CompassPoint.NO_COMPASS_POINT   // give a hint for arrow direction based on position within the target view child item

                    helpPopupWindow = showHelpPopup2(
                        // using the class context seems to give a different style to
                        // using view2.rootView.context which seems to be the expected style
                        // we WERE using the context!! here as it matched the artwork in figma.com better
                        // but unfortunately that did not work on all devices
                        // we are now using the view2.rootView.context and a separate style in styles.xml
                        view2.rootView.context,//context!!,
                        view2,
                        popupItem,
                        if (popupItem.percentX == -1)
                            Point(-1, -1)
                        else
                            Point((view2.width.toFloat()*targetPercentX/100).toInt(),
                                    (view2.height.toFloat()*targetPercentY/100).toInt()),
                        popupItem.titleResId,
                        popupItem.bodyResId,
                        currentHelpIndex != popupItems.size-1,   // show next button for all but last message
                        showNextGrayed,
                        currentHelpIndex == 0,  // show close (x) button for first message only
                        currentHelpIndex == 0 && getDerivedClassName(parent) == "MainActivity",  // show logo for welcome help
                        compassHint
                    )
                    val buttonClose: ImageButton =
                        helpPopupWindow!!.getContentView().findViewById(R.id.btnClose)
                    buttonClose.setOnClickListener {

                        dismissPopup()

                        ++globalCancelCount // increment global cancel count
                        SnackbarManager.show(
                            view2,
                            context!!.getString(R.string.help_tutor_dismiss_message),
                            4 * 1000,   // display for 4 seconds
                            3
                        )

                        stopShowingPopupHelp()
                    }
                    val buttonNext: Button =
                        helpPopupWindow!!.getContentView().findViewById(R.id.btnNext)
                    buttonNext.setOnClickListener {

                        dismissPopup()

                        currentHelpIndex++  // show next help popup next time (backed by preferences)
                        globalCancelCount = 0 // reset cancelled count (backed by preferences)

                        if (currentHelpIndex >= 0 && currentHelpIndex < popupItems.size) {
                            showNextPopupHelp()
                        }
                    }
                    val buttonPrev: Button =
                        helpPopupWindow!!.getContentView().findViewById(R.id.btnPrev)
                    buttonPrev.visibility = if (currentHelpIndex > 0) View.VISIBLE else View.INVISIBLE
                    buttonPrev.setOnClickListener {

                        dismissPopup()

                        currentHelpIndex--  // show previous help popup next time (backed by preferences)
                        globalCancelCount = 0 // reset cancelled count (backed by preferences)

                        if (currentHelpIndex >= 0 && currentHelpIndex < popupItems.size) {
                            showNextPopupHelp()
                        }
                    }
                }
            }
            return true // something was shown
        }
        return false
    }

    // a PopupWindow that should enable resource deletion on destruction
    class CustomPopupWindow(
        private val popupItem: PopupItem,
        private val contentView: View,
        width: Int, height: Int) :
            PopupWindow(contentView, width, height) {

        private var doneIt = false
        private var allowedNext = false
        private lateinit var lifecycleOwner: LifecycleOwner
        private val handler = Handler(Looper.getMainLooper())
        private val timer = fixedRateTimer(initialDelay = 1000, period = 500) {
            handler.post {
                updateUI()
            }
        }

        fun setLifecycleOwner(owner: LifecycleOwner) {
            this.lifecycleOwner = owner

            // Listen to lifecycle events of the owner
            owner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    // Dismiss the PopupWindow when the associated LifecycleOwner is destroyed
                    dismissPopup()
                }
            })
        }

        private fun updateUI() {
            // UI update code here to be run on the UI thread
            if (!doneIt && popupItem.isTaskAccomplished != null && popupItem.isTaskAccomplished.invoke())
                doneIt = true
            if (doneIt && !allowedNext) {
                val textNextButton = contentView.findViewById<Button>(R.id.btnNext)
                textNextButton.alpha = 1.0f
                textNextButton.isEnabled = true
                allowedNext = true
            }
        }

        fun dismissPopup() {
            timer.cancel() // Stop the timer when the popup is dismissed
            dismiss()
        }
    }

    // Function to create and show a round corner polygon-shaped popup
    private fun showHelpPopup2(context: Context,
                               parentView: View,
                               popupItem: PopupItem,
                               arrowTarget: Point,
                               titleResId: Int,
                               bodyResId: Int,
                               showNext: Boolean = true,
                               showNextGrayed: Boolean = true,
                               showClose: Boolean = true,
                               showSPIcon: Boolean = false,
                               compassHint: CompassPoint = CompassPoint.NO_COMPASS_POINT)
            : PopupWindow {

        val prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // local hard-coded settings
        val boxWidthFraction = 0.65f    // the screen width fraction to use for the width of a popup help message
        val sideWidthFraction = 0.04    // If closer than this width fraction to the edge then draw arrow East or West
        var popupArrowLength: Int      // The length of the popup pointing arrow

        val rootView = parentView.rootView
        val rootDrawableBounds = Rect()
        rootView.getWindowVisibleDisplayFrame(rootDrawableBounds)

// Assuming childView is the view whose coordinates you want to convert
        val childCoordinates = IntArray(2)
        parentView.getLocationOnScreen(childCoordinates)

// Assuming rootView is the root view of your layout
        val rootViewCoordinates = IntArray(2)
        rootView.getLocationOnScreen(rootViewCoordinates)

// Calculate the offset
        var offsetX = childCoordinates[0] - rootViewCoordinates[0]
        while (offsetX >= parentView.rootView.width)
            offsetX -= parentView.rootView.width    // fix x offset if found on a different slide
        while (offsetX < 0)
            offsetX += parentView.rootView.width    // fix x offset if found on a different slide
        var offsetY = childCoordinates[1] - rootViewCoordinates[1]
//        if (BuildConfig.DEBUG)
//            Timber.d("COORD LOG EVENT: offsetX=$offsetX OffsetY=$offsetY") // log to console in debug mode

// Now you can use offsetX and offsetY to get the coordinates on the rootView
        val rootViewX = arrowTarget.x + offsetX
        val rootViewY = arrowTarget.y + offsetY

        val displayCentral = arrowTarget.x == -1 && arrowTarget.y == -1 // true if we have no arrow and popup is centred

        val rootArrowTarget = Point(rootViewX, rootViewY)   // location of arrow in root coordinates

        // keep the root target point within drawable bounds
        if (rootArrowTarget.x > rootDrawableBounds.right)
            rootArrowTarget.x = rootDrawableBounds.right
        if (rootArrowTarget.x < rootDrawableBounds.left)
            rootArrowTarget.x = rootDrawableBounds.left
        if (rootArrowTarget.y > rootDrawableBounds.bottom)
            rootArrowTarget.y = rootDrawableBounds.bottom
        if (rootArrowTarget.y < rootDrawableBounds.top)
            rootArrowTarget.y = rootDrawableBounds.top

        // Inflate the balloon message xml resource popup design
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.balloon_message_x, null)

        // Update the popup SP icon for visibility
        val helpImageIcon = popupView.findViewById<ImageView>(R.id.helpImageIcon)
        helpImageIcon.visibility = if (showSPIcon) View.VISIBLE else View.GONE
        // Update the popup title and body text with the given string IDs
        val textTitleView = popupView.findViewById<TextView>(R.id.textTitle)
        textTitleView?.text = context.getString(titleResId)
        val textBodyView = popupView.findViewById<TextView>(R.id.textBody)
        val bodyString = context.getString(bodyResId)
        textBodyView.text = HtmlCompat.fromHtml(bodyString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val textNextButton = popupView.findViewById<Button>(R.id.btnNext)
        if (showNext) {
            if (showNextGrayed) {
                textNextButton.alpha = 0.5f
                textNextButton.isEnabled = false
            }
        } else {
            val enableDismissEnd = prefs.getBoolean("help_dismissal_end", false)
            if (enableDismissEnd) {
                // the last popup in the series says "Got it" rather than "Next"
                textNextButton.text = context.getString(R.string.gotIt)
                textNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // hide the next arrow
            } else {
                textNextButton.visibility = View.GONE
            }
        }
        val enableDismissStart = prefs.getBoolean("help_dismissal_start", false)
        val textCloseButton = popupView.findViewById<ImageButton>(R.id.btnClose)
        textCloseButton.visibility = if (showClose && enableDismissStart) View.VISIBLE else View.GONE

        val usedPopupWidth = (rootDrawableBounds.width() * boxWidthFraction).toInt()
        // Set the width we are allowed to use as the width of the inflated view
        val layoutParams =
            ViewGroup.LayoutParams(usedPopupWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupView.layoutParams = layoutParams

        // Measure the height of the inflated view
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(usedPopupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        // Get the measured height of the inflated view for the text box
        var popupBoxWidth = usedPopupWidth
        var popupBoxHeight = popupView.measuredHeight
        var shiftBoxX = 0   // how far we need to shift X coord of the popup to make room for the arrow
        var shiftBoxY = 0   // ditto for Y

        // need to determine where the arrow goes (north, east, south, west) and length of arrow
        val popupArrowNSLength = (rootDrawableBounds.width() * sideWidthFraction * 2).toInt()
        val arrowDirection: CompassPoint
        if (displayCentral)
            arrowDirection = CompassPoint.NO_COMPASS_POINT
        else if (rootArrowTarget.x > rootDrawableBounds.width() * (1.0f - sideWidthFraction))
            arrowDirection = CompassPoint.EAST
        else if (rootArrowTarget.x < rootDrawableBounds.width() * sideWidthFraction)
            arrowDirection = CompassPoint.WEST
        else if (compassHint == CompassPoint.SOUTH && popupBoxHeight + popupArrowNSLength < rootArrowTarget.y)
            arrowDirection = CompassPoint.SOUTH
        else if (compassHint == CompassPoint.NORTH && popupBoxHeight + popupArrowNSLength < rootDrawableBounds.height() - rootArrowTarget.y)
            arrowDirection = CompassPoint.NORTH
        else if (rootArrowTarget.y > (rootDrawableBounds.top + rootDrawableBounds.bottom) / 2)
            arrowDirection = CompassPoint.SOUTH
        else
            arrowDirection = CompassPoint.NORTH

        var boxDrawX: Int
        var boxDrawY: Int
        val boxDrawPoint: Point
        val pointDrawOffset: Point

        // calculate how to draw the popup box and arrow
        when (arrowDirection) {

            CompassPoint.NO_COMPASS_POINT -> {
                popupArrowLength = 0
                boxDrawX = 0
                boxDrawY = 0
                boxDrawPoint = Point(boxDrawX, boxDrawY)
                popupBoxWidth += popupArrowLength
                pointDrawOffset = Point(boxDrawX, boxDrawY)
            }

            CompassPoint.EAST -> {
                popupArrowLength = (rootDrawableBounds.width() * sideWidthFraction).toInt()
                boxDrawX = rootArrowTarget.x - popupBoxWidth - popupArrowLength
                boxDrawY = rootArrowTarget.y - popupBoxHeight / 2
                if (boxDrawY < rootDrawableBounds.top)
                    boxDrawY = rootDrawableBounds.top
                if (boxDrawY > rootDrawableBounds.bottom - popupBoxHeight)
                    boxDrawY = rootDrawableBounds.bottom - popupBoxHeight
                boxDrawPoint = Point(boxDrawX, boxDrawY)
                popupBoxWidth += popupArrowLength
                pointDrawOffset =
                    Point(rootArrowTarget.x - boxDrawX, rootArrowTarget.y - boxDrawY)
            }

            CompassPoint.WEST -> {
                popupArrowLength = (rootDrawableBounds.width() * sideWidthFraction).toInt()
                boxDrawX = rootArrowTarget.x + popupArrowLength
                boxDrawY = rootArrowTarget.y - popupBoxHeight / 2
                if (boxDrawY < rootDrawableBounds.top)
                    boxDrawY = rootDrawableBounds.top
                if (boxDrawY > rootDrawableBounds.bottom - popupBoxHeight)
                    boxDrawY = rootDrawableBounds.bottom - popupBoxHeight
                boxDrawPoint = Point(boxDrawX, boxDrawY)
                popupBoxWidth += popupArrowLength
                shiftBoxX = popupArrowLength
                pointDrawOffset = Point(0, rootArrowTarget.y - boxDrawY)
            }

            CompassPoint.SOUTH -> {
                popupArrowLength = (rootDrawableBounds.width() * sideWidthFraction * 2).toInt()
                boxDrawY = rootArrowTarget.y - popupBoxHeight - popupArrowLength
                boxDrawX = rootArrowTarget.x - popupBoxWidth / 2
                if (boxDrawX < rootDrawableBounds.left + popupArrowLength / 2)
                    boxDrawX = rootDrawableBounds.left + popupArrowLength / 2
                if (boxDrawX + popupBoxWidth > rootDrawableBounds.right - popupArrowLength / 2)
                    boxDrawX = rootDrawableBounds.right - popupBoxWidth - popupArrowLength / 2
                if (boxDrawX > rootArrowTarget.x)
                    boxDrawX = rootArrowTarget.x
                if (boxDrawX + popupBoxWidth < rootArrowTarget.x)
                    boxDrawX = rootArrowTarget.x - popupBoxWidth
                boxDrawPoint = Point(boxDrawX, boxDrawY)
                popupBoxHeight += popupArrowLength
                pointDrawOffset =
                    Point(rootArrowTarget.x - boxDrawX, rootArrowTarget.y - boxDrawY)
            }

            CompassPoint.NORTH -> {
                popupArrowLength = (rootDrawableBounds.width() * sideWidthFraction * 2).toInt()
                boxDrawY = rootArrowTarget.y + popupArrowLength
                boxDrawX = rootArrowTarget.x - popupBoxWidth / 2
                if (boxDrawX < rootDrawableBounds.left + popupArrowLength / 2)
                    boxDrawX = rootDrawableBounds.left + popupArrowLength / 2
                if (boxDrawX + popupBoxWidth > rootDrawableBounds.right - popupArrowLength / 2)
                    boxDrawX = rootDrawableBounds.right - popupBoxWidth - popupArrowLength / 2
                if (boxDrawX > rootArrowTarget.x)
                    boxDrawX = rootArrowTarget.x
                if (boxDrawX + popupBoxWidth < rootArrowTarget.x)
                    boxDrawX = rootArrowTarget.x - popupBoxWidth
                boxDrawPoint = Point(boxDrawX, boxDrawY)
                popupBoxHeight += popupArrowLength
                shiftBoxY = popupArrowLength
                pointDrawOffset = Point(rootArrowTarget.x - boxDrawX, 0)
            }
        }

        // create a FrameLayout that we can draw the text and buttons shifted leaving
        // room for the arrow pointing north or west
        val shiftedView = ShiftedView(context, null, 0, popupView)
        shiftedView.setShift(shiftBoxX, shiftBoxY) // Shift the child view by given amount

        val popupWindow = CustomPopupWindow(popupItem, shiftedView, popupBoxWidth, popupBoxHeight)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupWindow.setIsClippedToScreen(true)  // allow dragging half off screen (Android 10+ only)
        }
        // use a background drawing class to draw the box and arrow
        val customBackgroundDrawable =
            CustomBackgroundDrawable(context, arrowDirection, popupArrowLength, pointDrawOffset)
        popupWindow.setBackgroundDrawable(customBackgroundDrawable)

        // Set animation style - to show a fade in and out when the help popup appears and disappears
        popupWindow.animationStyle = R.style.PopupAnimation

        var initialX = min(rootArrowTarget.x, boxDrawPoint.x)
        var initialY = min(rootArrowTarget.y, boxDrawPoint.y)
        if (arrowDirection == CompassPoint.NO_COMPASS_POINT) {
            // calculate the centred x and y initial positions
            initialX = rootDrawableBounds.width() / 2 - popupWindow.width / 2
            initialY = rootDrawableBounds.top + rootDrawableBounds.height() / 2 - popupWindow.height / 2
        }
//        Timber.i("first initialX = $initialX, first initialY = $initialY")
        // Show the popup window at the desired location using root coordinates
        popupWindow.showAtLocation(
            rootView,
            Gravity.NO_GRAVITY,
            initialX,
            initialY
        )
        // Handle touch events for dragging
        var lastX = 0
        var lastY = 0
        popupView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Remember initial touch position
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate the distance moved
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY

                    // Get current position
                    var currentX = initialX + dx
                    var currentY = initialY + dy
//                    Timber.i("currentX = $currentX, currentY = $currentY")

                    // don't allow position to go over half way off screen
                    if (currentX > rootDrawableBounds.right - popupWindow.width/2)
                        currentX = rootDrawableBounds.right - popupWindow.width/2
                    if (currentX < rootDrawableBounds.left - popupWindow.width/2)
                        currentX = rootDrawableBounds.left - popupWindow.width/2
                    if (currentY > rootDrawableBounds.bottom - popupWindow.height/2)
                        currentY = rootDrawableBounds.bottom - popupWindow.height/2
                    if (currentY < rootDrawableBounds.top - popupWindow.height/2)
                        currentY = rootDrawableBounds.top - popupWindow.height/2
                    initialX = currentX - dx    // update initial values if needed
                    initialY = currentY - dy    // ditto

                    // Update the position of the PopupWindow
                    popupWindow.update(currentX, currentY, -1, -1)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Ensure accessibility events are handled
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    initialX += dx
                    initialY += dy

                    v.performClick()
                    true
                }
                else -> false
            }
        }

// added code here to make sure there are no memory leaks
        popupWindow.setLifecycleOwner(activity!!)   // TODO: REMOVE? DOES THIS WORK?

        return popupWindow
    }

    class CustomBackgroundDrawable(private val context: Context,
                                   private val compassPoint: CompassPoint,
                                   private val popupArrowLength: Int,
                                   private val arrowPoint: Point
    ) : Drawable() {
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.popup_help_background)
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds // the drawing bounds of this item
            var xAdjustAdd = 0.0f
            var yAdjustAdd = 0.0f
            var xAdjustSub = 0.0f
            var yAdjustSub = 0.0f
            val cornerRadius = 30.0f
            when (compassPoint) {
                CompassPoint.NORTH -> yAdjustAdd = popupArrowLength.toFloat()
                CompassPoint.WEST -> xAdjustAdd = popupArrowLength.toFloat()
                CompassPoint.SOUTH -> yAdjustSub = popupArrowLength.toFloat()
                CompassPoint.EAST -> xAdjustSub = popupArrowLength.toFloat()
            }
            val rect = RectF(xAdjustAdd, yAdjustAdd, bounds.right.toFloat()-xAdjustSub, bounds.bottom.toFloat()-yAdjustSub)
            val arrowBaseXShift = ((bounds.right-xAdjustSub)/2-arrowPoint.x)/2
            val arrowBaseYShift = ((bounds.bottom-yAdjustSub)/2-arrowPoint.y)/1.25f

            // Define a path for the polygon shape
            val path = Path().apply {
                moveTo(rect.left, rect.top + cornerRadius) // top-left (before clockwise arc)
                arcTo(RectF(rect.left, rect.top, rect.left + 2 * cornerRadius, rect.top + 2 * cornerRadius), 180f, 90f)
                if (compassPoint == CompassPoint.NORTH) {
                    lineTo(bounds.right.toFloat()*9/16-arrowBaseXShift, yAdjustAdd) // arrow-base-right
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()*7/16-arrowBaseXShift, yAdjustAdd) // arrow-base-left
                }
                lineTo(rect.right - cornerRadius, rect.top) // top-right
                arcTo(RectF(rect.right - 2 * cornerRadius, rect.top, rect.right, rect.top + 2 * cornerRadius), 270f, 90f)
                if (compassPoint == CompassPoint.EAST) {
                    lineTo(bounds.right.toFloat()-xAdjustSub,bounds.bottom.toFloat()*9/16-arrowBaseYShift) // arrow-base-left-bottom
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()-xAdjustSub,bounds.bottom.toFloat()*7/16-arrowBaseYShift) // arrow-base-left-top
                }
                lineTo(rect.right, rect.bottom - cornerRadius) // bottom-right
                arcTo(RectF(rect.right - 2 * cornerRadius, rect.bottom - 2 * cornerRadius, rect.right, rect.bottom), 0f, 90f)
                if (compassPoint == CompassPoint.SOUTH) {
                    lineTo(bounds.right.toFloat()*9/16-arrowBaseXShift, bounds.bottom.toFloat()-yAdjustSub) // arrow-base-right
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()*7/16-arrowBaseXShift, bounds.bottom.toFloat()-yAdjustSub) // arrow-base-left
                }
                lineTo(rect.left + cornerRadius, rect.bottom) // Bottom-left
                arcTo(RectF(rect.left, rect.bottom - 2 * cornerRadius, rect.left + 2 * cornerRadius, rect.bottom), 90f, 90f)
                if (compassPoint == CompassPoint.WEST) {
                    lineTo(xAdjustAdd, bounds.bottom.toFloat()*9/16-arrowBaseYShift) // arrow-base-left-bottom
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(xAdjustAdd, bounds.bottom.toFloat()*7/16-arrowBaseYShift) // arrow-base-left-top
                }
                close() // Close the path back to top-left
            }
            // Draw the polygon shape as the background
            canvas.drawPath(path, paint)
        }
        override fun setAlpha(alpha: Int) {
            // Not implemented
        }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            // Not implemented
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            // Not implemented
            return PixelFormat.TRANSPARENT
        }
    }

    class ShiftedView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        childView: View // Add a parameter for the child view to be shifted
    ) : FrameLayout(context, attrs, defStyleAttr) {

        private val shiftedView: View = childView

        init {
            addView(shiftedView) // Add the child view to the layout
        }

        fun setShift(x: Int, y: Int) {
            // Adjust the position of the shifted view
            val layoutParams = shiftedView.layoutParams as LayoutParams
            layoutParams.leftMargin = x
            layoutParams.topMargin = y
            shiftedView.layoutParams = layoutParams
        }
    }
}

package org.sil.storyproducer.controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
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
import org.sil.storyproducer.R
import kotlin.math.min

class PopupItem(val anchorViewId: Int,
                val percentX: Int,
                val percentY: Int,
                val titleResId: Int,
                val bodyResId: Int,
                val waitForUi: Boolean = false) {

    var itemString = ""
}
class PopupHelpUtils(private val parent: Any,
                     private val slideNumber: Int = 0) {

    private var helpPopupWindow: PopupWindow? = null
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
        if (helpPopupWindow != null) {
            helpPopupWindow?.dismiss()
            helpPopupWindow = null
        }
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
            val a = parent.javaClass.simpleName
            val activityClassName = getDerivedClassName(parent)
            val prefString = "PopupHelpGroup_${activityClassName}_${slideNumber}"
            field = prefs.getInt(prefString, 0)
            return field
        }
        set(value) {
            if (field != value) {
                val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                val activityClassName = getDerivedClassName(parent)
                val prefString = "PopupHelpGroup_${activityClassName}_${slideNumber}"
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
                         waitForUi: Boolean = false) {
        val newPopup = PopupItem(anchorViewId, percentX, percentY, titleResId, bodyResId, waitForUi)
        popupItems.add(newPopup)
    }

    fun stopShowingPopupHelp() {
        currentHelpIndex = -1   // Stop showing popup help for the current Phase/Activity
    }

    fun reShowPopupHelp() {
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

    fun showNextPopupHelp() {

        if (globalCancelCount >= 2)
            return  // help has been cancelled twice

        if (currentHelpIndex < 0)
            return  // help has been turned off

        if (currentHelpIndex >= popupItems.size)
            return  // we have shown all our help

        if (context == null || activity == null)
            return

        var popupItem = popupItems[currentHelpIndex]

        val view = activity?.findViewById<View>(popupItem.anchorViewId)
        if (view != null) {
            view.post {
                // get the view again in case it has gone stale (fixes a crash/display bug)
                val view2 = activity?.findViewById<View>(popupItem.anchorViewId)
                if (view2 != null) {
                    if (helpPopupWindow != null)
                        helpPopupWindow?.dismiss()
                    helpPopupWindow = showHelpPopup2(
                        // using the class context seems to give a different style to
                        // using view2.rootView.context which seems to be the expected style
                        // we are now using the context!! here as it matches the artwork in figma.com better
                        // we WERE using the view2.rootView.context to be consistent
                        context!!,//view2.rootView.context,
                        view2,
                        if (popupItem.percentX == -1)
                            Point(-1, -1)
                        else
                            Point((view2.width.toFloat()*popupItem.percentX/100).toInt(),
                            (view2.height.toFloat()*popupItem.percentY/100).toInt()),
                        popupItem.titleResId,
                        popupItem.bodyResId,
                        currentHelpIndex == popupItems.size-1   // show ok button for last message
                    )
                    if (helpPopupWindow != null) {

                        val buttonClose: ImageButton =
                            helpPopupWindow!!.getContentView().findViewById(R.id.btnClose)
                        buttonClose.setOnClickListener {

                            dismissPopup()

                            if (++globalCancelCount >= 2) // no more popups if cancelled twice (backed by preferences)
                                Toast.makeText(
                                    activity,
                                    activity?.getString(R.string.help_dismiss_message),
                                    Toast.LENGTH_LONG
                                ).show()

                            stopShowingPopupHelp()
                        }
                        val buttonNext: Button =
                            helpPopupWindow!!.getContentView().findViewById(R.id.btnNext)
                        buttonNext.setOnClickListener {

                            //                        Toast.makeText(context, "Showing Next...", Toast.LENGTH_SHORT).show()
                            dismissPopup()

                            currentHelpIndex++  // show next help popup next time (backed by preferences)
                            globalCancelCount = 0 // reset cancelled count (backed by preferences) TODO: TO CONFIRM WITH BRYAN AND GREG

                            if (currentHelpIndex >= 0 && currentHelpIndex < popupItems.size) {

                                var nextPopupItem = popupItems[currentHelpIndex]

                                if (!nextPopupItem.waitForUi) {
                                    showNextPopupHelp()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // a PopupWindow that should enable resource deletion on destruction
    class CustomPopupWindow(contentView: View, width: Int, height: Int) :
            PopupWindow(contentView, width, height) {

        private lateinit var lifecycleOwner: LifecycleOwner

        fun setLifecycleOwner(owner: LifecycleOwner) {
            this.lifecycleOwner = owner

            // Listen to lifecycle events of the owner
            owner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    // Dismiss the PopupWindow when the associated LifecycleOwner is destroyed
                    dismiss()
                }
            })
        }
    }

    // Function to create and show a round corner polygon-shaped popup
    private fun showHelpPopup2(context: Context, parentView: View, arrowTarget: Point, titleResId: Int, bodyResId: Int, showOk: Boolean = false)
            : PopupWindow {

        // local hard-coded settings
        val boxWidthFraction = 0.65f    // the screen width fraction to use for the width of a popup help message
        val sideWidthFraction = 0.04    // If closer than this width fraction to the edge then draw arrow East or West
        var popupArrowLength = 200      // The length of the popup pointing arrow

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

        // Update the popup title and body text with the given string IDs
        val textTitleView = popupView.findViewById<TextView>(R.id.textTitle)
        textTitleView?.text = context.getString(titleResId)
        val textBodyView = popupView.findViewById<TextView>(R.id.textBody)
        val bodyString = context.getString(bodyResId)
        textBodyView.text = HtmlCompat.fromHtml(bodyString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (showOk) {
            // the last popup in the series says "OK" rather than "Next"
            val textOkButton = popupView.findViewById<Button>(R.id.btnNext)
            textOkButton.text = context.getString(R.string.ok)
        }

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
        val arrowDirection: CompassPoint
        if (displayCentral)
            arrowDirection = CompassPoint.NO_COMPASS_POINT
        else if (rootArrowTarget.x > rootDrawableBounds.width() * (1.0f - sideWidthFraction))
            arrowDirection = CompassPoint.EAST
        else if (rootArrowTarget.x < rootDrawableBounds.width() * sideWidthFraction)
            arrowDirection = CompassPoint.WEST
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

        val popupWindow = CustomPopupWindow(shiftedView, popupBoxWidth, popupBoxHeight)
//        popupWindow.setTouchModal(true)   // not working? (needs API 29 anyway)

        // use a background drawing class to draw the box and arrow
        val customBackgroundDrawable =
            CustomBackgroundDrawable(context, arrowDirection, popupArrowLength, pointDrawOffset)
        popupWindow.setBackgroundDrawable(customBackgroundDrawable)

        // Set animation style - to show a fade in and out when the help popup appears and disappears
        popupWindow.animationStyle = R.style.PopupAnimation

        // Show the popup window at the desired location using root coordinates
        popupWindow.showAtLocation(
            rootView,
            if (arrowDirection == CompassPoint.NO_COMPASS_POINT)
                Gravity.CENTER
            else
                Gravity.NO_GRAVITY,
            min(rootArrowTarget.x, boxDrawPoint.x),
            min(rootArrowTarget.y, boxDrawPoint.y)
        )

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
            when (compassPoint) {
                CompassPoint.NORTH -> yAdjustAdd = popupArrowLength.toFloat()
                CompassPoint.WEST -> xAdjustAdd = popupArrowLength.toFloat()
                CompassPoint.SOUTH -> yAdjustSub = popupArrowLength.toFloat()
                CompassPoint.EAST -> xAdjustSub = popupArrowLength.toFloat()
            }
            val arrowBaseXShift = ((bounds.right-xAdjustSub)/2-arrowPoint.x)/2
            val arrowBaseYShift = ((bounds.bottom-yAdjustSub)/2-arrowPoint.y)/1.25f

            // Define a path for the polygon shape
            val path = Path().apply {
                moveTo(xAdjustAdd, yAdjustAdd) // top-left
                if (compassPoint == CompassPoint.NORTH) {
                    lineTo(bounds.right.toFloat()*9/16-arrowBaseXShift, yAdjustAdd) // arrow-base-right
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()*7/16-arrowBaseXShift, yAdjustAdd) // arrow-base-left
                }
                lineTo(bounds.right.toFloat()-xAdjustSub, yAdjustAdd) // top-right
                if (compassPoint == CompassPoint.EAST) {
                    lineTo(bounds.right.toFloat()-xAdjustSub,bounds.bottom.toFloat()*9/16-arrowBaseYShift) // arrow-base-left-bottom
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()-xAdjustSub,bounds.bottom.toFloat()*7/16-arrowBaseYShift) // arrow-base-left-top
                }
                lineTo(bounds.right.toFloat()-xAdjustSub, bounds.bottom.toFloat()-yAdjustSub) // bottom-right
                if (compassPoint == CompassPoint.SOUTH) {
                    lineTo(bounds.right.toFloat()*9/16-arrowBaseXShift, bounds.bottom.toFloat()-yAdjustSub) // arrow-base-right
                    lineTo(arrowPoint.x.toFloat(), arrowPoint.y.toFloat()) // arrow-tip
                    lineTo(bounds.right.toFloat()*7/16-arrowBaseXShift, bounds.bottom.toFloat()-yAdjustSub) // arrow-base-left
                }
                lineTo(xAdjustAdd,bounds.bottom.toFloat()-yAdjustSub) // Bottom-left
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

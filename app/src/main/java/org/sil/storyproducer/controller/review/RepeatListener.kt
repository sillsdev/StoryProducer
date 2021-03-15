import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next one after the initialInterval, and subsequent
 * ones after the initialRepeatDelay.
 *
 * @param initialInterval The interval after first click event
 * @param initialRepeatDelay The interval after second and subsequent click events
 *
 * @param clickListener The OnClickListener, that will be called
 * periodically
 *
 * Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
 * achieve this.
 *
 * Usage:
 *
 * someView.setOnTouchListener(new RepeatListener(400, 100, new OnClickListener() {
 *  @Override
 *  public void onClick(View view) {
 *      // the code to execute repeatedly
 *  }
 * }));
 *
 * Kotlin example:
 *  someView.setOnTouchListener(RepeatListener(defaultInitialTouchTime, defaultRepeatDelayTime, OnClickListener {
 *      // the code to execute repeatedly
 *  }))
 *
 */
class RepeatListener(initialInterval: Int,
                     initialRepeatDelay: Int,
                     clickListener: View.OnClickListener) : OnTouchListener {

    private val handler = Handler()

    private var initialInterval: Int
    private var initialRepeatDelay: Int

    private var clickListener: View.OnClickListener
    private var touchedView: View? = null

    init {
        require(!(initialInterval < 0 || initialRepeatDelay < 0)) { "negative intervals not allowed" }

        this.initialInterval = initialRepeatDelay
        this.initialRepeatDelay = initialInterval

        this.clickListener = clickListener
    }

    private val handlerRunnable: Runnable = run {
        Runnable {
            if (touchedView!!.isEnabled) {
                handler.postDelayed(handlerRunnable, initialRepeatDelay.toLong())
                clickListener.onClick(touchedView)
            } else {
                // if the view was disabled by the clickListener, remove the callback
                handler.removeCallbacks(handlerRunnable)
                touchedView!!.isPressed = false
                touchedView = null
            }
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(handlerRunnable)
                handler.postDelayed(handlerRunnable, initialRepeatDelay.toLong())
                touchedView = view
                touchedView!!.isPressed = true
                clickListener.onClick(view)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(handlerRunnable)
                touchedView!!.isPressed = false
                touchedView = null
                return true
            }
        }
        return false
    }
}
package org.tyndalebt.spadv.tools.toolbar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Handler

private const val ANIMATION_DURATION = 1500

/**
 * Handles the animation between two colors.
 *
 * The initial color transitions to the target color and back and repeats until stopped.
 *
 * The transitionDrawable property can be set to a color property of some element of the UI.
 */
class AnimationHandler(initialColor: Int = Color.rgb(0, 0, 255),
                       targetColor: Int = Color.rgb(255, 0, 0)) {
    val transitionDrawable: TransitionDrawable = TransitionDrawable(arrayOf(ColorDrawable(initialColor), ColorDrawable(targetColor)))
    private var colorHandler: Handler = Handler()
    private var colorHandlerRunnable: Runnable
    private var isTargetColor = false

    init {
        colorHandlerRunnable = Runnable {
            isTargetColor = if (isTargetColor) {
                transitionDrawable.reverseTransition(ANIMATION_DURATION)
                false
            } else {
                transitionDrawable.startTransition(ANIMATION_DURATION)
                true
            }
            startAnimation(true, ANIMATION_DURATION)
        }
    }

    /**
     * This function is used to start the handler to run the runnable. <br></br>
     * [.setupRecordingAnimationHandler] should be called first before calling this function
     * to initialize the colorHandler and colorHandlerRunnable().
     *
     * @param isDelayed Used to signify that the runnable will be delayed in running.
     * @param delay     The time that will be delayed in ms if isDelayed is true.
     */
    fun startAnimation(isDelayed: Boolean = false, delay: Int = 0) {
        if (isDelayed) {
            colorHandler.postDelayed(colorHandlerRunnable, delay.toLong())
        } else {
            colorHandler.post(colorHandlerRunnable)
        }
    }

    /**
     * Stops the animation from continuing. The removeCallbacks function removes all
     * colorHandlerRunnable from the MessageQueue and also resets the toolbar to its original color.
     * (transitionDrawable.resetTransition();)
     */
    fun stopAnimation() {
        colorHandler.removeCallbacks(colorHandlerRunnable)
        transitionDrawable.resetTransition()
    }
}
package org.sil.storyproducer.androidtest.happypath.finalize

import androidx.test.espresso.IdlingResource
import org.sil.storyproducer.androidtest.utilities.Constants

class VideoCreationIdlingResource() : IdlingResource {
    private var callback : IdlingResource.ResourceCallback? = null
    private var startTime = System.currentTimeMillis()

    /** Returns the name of the resources (used for logging and idempotency of registration).  */
    override fun getName(): String {
        return "VideoCreation Idling"
    }

    /**
     * Returns `true` if resource is currently idle. Espresso will **always** call this
     * method from the main thread, therefore it should be non-blocking and return immediately.
     */
    override fun isIdleNow(): Boolean {
        return System.currentTimeMillis() - startTime < Constants.durationToWaitForVideoExport
    }

    /**
     * Registers the given [ResourceCallback] with the resource. Espresso will call this method:
     *
     *
     *  * with its implementation of [ResourceCallback] so it can be notified asynchronously
     * that your resource is idle
     *  * from the main thread, but you are free to execute the callback's onTransitionToIdle from
     * any thread
     *  * once (when it is initially given a reference to your IdlingResource)
     *
     *
     *
     * You only need to call this upon transition from busy to idle - if the resource is already
     * idle when the method is called invoking the call back is optional and has no significant
     * impact.
     */
    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }
}
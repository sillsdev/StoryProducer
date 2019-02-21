package org.sil.storyproducer.androidtest.utilities

import android.R
import android.app.Activity
import android.content.Context
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

object ActivityAccessor {
    // See https://stackoverflow.com/questions/24517291/get-current-activity-in-espresso-android
    fun getCurrentActivity(): Activity? {
        val currentActivity = arrayOfNulls<Activity>(1)
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.content), ViewMatchers.isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "Gets the currently displayed activity so that Espresso tests can reach 'under the hood' and reference actual Views."
            }

            override fun perform(uiController: UiController, view: View) {
                if (view.context is Activity) {
                    val activity1 = view.context as Activity
                    currentActivity[0] = activity1
                }
            }
        })
        return currentActivity[0]
    }
}
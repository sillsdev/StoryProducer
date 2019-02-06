package org.sil.storyproducer.controller

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.support.v7.widget.AppCompatSeekBar
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.sil.storyproducer.R

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LearnPhaseTest {

    @Rule
    @JvmField
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(RegistrationActivity::class.java, false, false)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE")

    @Before
    fun setUp() {
        navigateToLearnPhase()
    }

    @Test
    fun A_should_BeAbleToUsePlayButton() {
        val learnPhaseVideoSeekBar = getActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        // check progress of seek bar
        val firstProgress = learnPhaseVideoSeekBar!!.progress
        // click play button
        onView(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button)).perform(click())
        // wait a few seconds for narration to play and story to move to next slide
        Thread.sleep(7000)
        // click pause button
        onView(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button)).perform(click())
        // check progress of seek bar
        val secondProgress = learnPhaseVideoSeekBar.progress
        assert(secondProgress > firstProgress)
    }

    @Test
    fun B_should_BeAbleToRecordAudioClip() {
        // click 'mic' button to start recording
        //onView(allOf(isDisplayed(), withId(org.sil.storyproducer.R.id.start_recording_button))).perform(click())

        //onView(withId(org.sil.storyproducer.R.id.fragment_image_view)).perform(click())
        // wait a few seconds
        //Thread.sleep(9000)
        // click 'mic' button to start recording
        //onView(allOf(isDisplayed(), withId(org.sil.storyproducer.R.id.start_recording_button))).perform(click())
    }

    private fun getActivity(): Activity? {
        val currentActivity = arrayOfNulls<Activity>(1)
        onView(allOf(withId(android.R.id.content), isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "getting progress from a videoSeekBar"
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

    private fun navigateToLearnPhase() {
        setUpDummyWorkspacePickerIntent()
        mActivityTestRule.launchActivity(null)

        onView(withText("Skip Registration")).perform(click())
        onView(withId(android.R.id.button1)).perform(scrollTo(), click())
        onView(withText(containsString("Lost Coin"))).perform(scrollTo(), click())
        onView(withId(org.sil.storyproducer.R.id.toolbar)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Learn"))).perform(click())

        Intents.release()
    }

    private fun setUpDummyWorkspacePickerIntent() {
        Intents.init()
        val expectedIntent = hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val returnedIntent = Intent()
        // An empty Uri will cause Workspace.setupWorkspacePass to silently fail.
        // This allows the test to proceed with a manually set workspace directory.
        returnedIntent.setData(Uri.EMPTY)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, returnedIntent))
    }

}

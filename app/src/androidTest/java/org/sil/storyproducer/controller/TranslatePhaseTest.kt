package org.sil.storyproducer.controller

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.AppCompatTextView
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.*
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.runners.MethodSorters
import java.lang.Integer.parseInt
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.UiController
import org.hamcrest.Matcher
import org.junit.*
import org.sil.storyproducer.R


@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TranslatePhaseTest {

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
        navigateToTranslatePhase()
    }

    @Test
    fun A_should_BeAbleToSwipeBetweenSlides() {
        val originalSlideNumber = findCurrentSlideNumber()
        var nextSlideNumber = originalSlideNumber + 1
        expectToBeOnSlide(originalSlideNumber)
        swipeLeftOnSlide()
        giveUiTimeToChangeSlides()
        expectToBeOnSlide(nextSlideNumber)
        swipeRightOnSlide()
        giveUiTimeToChangeSlides()
        expectToBeOnSlide(originalSlideNumber)
    }

    @Test
    fun B_should_BeAbleToPlayNarrationOfASlide() {
        val originalProgress = getCurrentSlideAudioProgress()
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertNotEquals(endingProgress, originalProgress)
    }

    @Test
    fun C_should_BeAbleToRecordTranslationForASlide() {
        // The "pulsing" animation on the recording toolbar causes the
        // Espresso click to hang, so we disable it for the test.
        disableCustomAnimations()
        pressMicButton()
        giveAppTimeToRecordAudio()
        pressMicButton()
        enableCustomAnimations()
    }

    private fun enableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
        preferencesEditor.remove(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation))
    }

    private fun disableCustomAnimations() {
        val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
        preferencesEditor.putBoolean(mActivityTestRule.activity.resources.getString(org.sil.storyproducer.R.string.recording_toolbar_disable_animation), true)
        preferencesEditor.commit()
    }

    private fun pressMicButton() {
        onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToRecordAudio() {
        Thread.sleep(200)
    }

    private fun expectToBeOnSlide(originalSlideNumber: Int) {
        onView(allOf(withId(R.id.slide_number_text), withText(originalSlideNumber.toString()))).check(matches(isDisplayed()))
    }

    private fun findCurrentSlideNumber(): Int {
        val slideNumberTextView = getActivity()?.findViewById<AppCompatTextView>(org.sil.storyproducer.R.id.slide_number_text)
        return parseInt(slideNumberTextView!!.text.toString())
    }

    private fun swipeRightOnSlide() {
        onView(allOf(withId(R.id.phase_frame))).perform(swipeRight())
    }

    private fun swipeLeftOnSlide() {
        onView(allOf(withId(R.id.phase_frame))).perform(swipeLeft())
    }

    private fun giveUiTimeToChangeSlides() {
        Thread.sleep(50)
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = getActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressPlayPauseButton() {
        onView(allOf(withId(org.sil.storyproducer.R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(250)
    }

    private fun navigateToTranslatePhase() {
        setUpDummyWorkspacePickerIntent()
        mActivityTestRule.launchActivity(null)
        tearDownDummyWorkspacePickerIntent()

        onView(withText("Skip Registration")).perform(click())
        onView(withId(android.R.id.button1)).perform(scrollTo(), click())
        onView(withText(containsString("Lost Coin"))).perform(scrollTo(), click())
        onView(withId(org.sil.storyproducer.R.id.toolbar)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Translate"))).perform(click())
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

    private fun tearDownDummyWorkspacePickerIntent() {
        Intents.release()
    }

    // See https://stackoverflow.com/questions/24517291/get-current-activity-in-espresso-android
    private fun getActivity(): Activity? {
        val currentActivity = arrayOfNulls<Activity>(1)
        onView(allOf(withId(android.R.id.content), isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "getting text from a TextView"
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

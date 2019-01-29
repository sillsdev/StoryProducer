package org.sil.storyproducer.controller

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onData



@LargeTest
@RunWith(AndroidJUnit4::class)
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

    @Test
    fun should_DoStuffInTheTranslatePhase() {
        navigateToTranslatePhase()

        // TODO: Actually test the translate phase, duh!
        Thread.sleep(10000)
    }

    private fun navigateToTranslatePhase() {
        setUpDummyWorkspacePickerIntent()
        mActivityTestRule.launchActivity(null)

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
}

package org.sil.storyproducer.androidtest.utilities

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers

object IntentMocker {
    fun setUpDummyWorkspacePickerIntent() {
        Intents.init()
        val expectedIntent = IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val returnedIntent = Intent()
        // An empty Uri will cause Workspace.setupWorkspacePass to silently fail.
        // This allows the test to proceed with a manually set workspace directory.
        returnedIntent.setData(Uri.EMPTY)
        Intents.intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, returnedIntent))
    }

    fun tearDownDummyWorkspacePickerIntent() {
        Intents.release()
    }
}
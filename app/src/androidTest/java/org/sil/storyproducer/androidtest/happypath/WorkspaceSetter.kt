package org.sil.storyproducer.androidtest.happypath

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertNotNull
import org.junit.Rule
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator.isNotDisplayed
import org.sil.storyproducer.R

private const val APP_PACKAGE_NAME = "org.sil.storyproducer"
private const val TIMEOUT_DURATION = 5000L
private const val FILE_PICKER_PACKAGE = "com.android.documentsui"

// Android Version: 7.1.1 (API 25)
//private const val INTERNAL_STORAGE_BUTTON_TEXT = "Android SDK built for x86"

// Android Version: 9.0 (API 28)
private const val INTERNAL_STORAGE_BUTTON_TEXT = "AOSP on IA Emulator"

private const val REGISTRATION_SCREEN_CONTAINER = "org.sil.storyproducer:id/registration_scroll_view"

/**
 * This "test" uses UIAutomator to set the Story Producer workspace directory to the value
 * specified in org.sil.storyproducer.androidtest.utilities.Constants. Be sure that the
 * workspace directory actually exists on the device prior to running this test.
 *
 * This test is intended to be run once prior to all the Espresso tests, since those tests
 * assume that the workspace directory has already been selected by the user. Every time
 * the app's storage gets cleared, the workspace information gets lost. When this happens,
 * this test then needs to be run again, to select the workspace.
 *
 * This test was developed assuming a Nexus 5X emulator running Android API 28 for x86.
 * Depending on your device/emulator, you may need to modify the INTERNAL_STORAGE_BUTTON_TEXT
 * string.
 */
class WorkspaceSetter {

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

    companion object {
        @SdkSuppress(minSdkVersion = 18)
        fun setWorkspaceSoOtherTestsRunCorrectly() {
            val device = UiDevice.getInstance(getInstrumentation())

            launchStoryProducerApp(device)

            selectStoryProducerWorkspace(device)
        }

        private fun launchStoryProducerApp(device: UiDevice) {
            device.pressHome()
            val launcherPackage = device.launcherPackageName
            assertNotNull(launcherPackage)
            device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIMEOUT_DURATION)

            val context = getApplicationContext<Application>()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(APP_PACKAGE_NAME)
            launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launchIntent)
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE_NAME).depth(0)), TIMEOUT_DURATION)
        }

        private fun selectStoryProducerWorkspace(device: UiDevice) {

            // If the welcome dialog does not display, then the workspace has already been initialized.
            // This means, that we need to ensure that the workspace is set to the testing directory
            val welcomeScreenButton = onView(withId(R.id.select_templates_button))
            if(welcomeScreenButton.isNotDisplayed()) {
                return
            }

            // Select the "Select Location" button
            onView(withId(R.id.select_templates_button))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                    .perform(click())

            device.findObject(By.res("android:id/button1").text("OK")).click()
            device.wait(Until.hasObject(By.desc("More options")), TIMEOUT_DURATION)
            device.findObject(By.desc("More options")).click()
            val showInternalStorage = device.findObject(By.text("Show internal storage"))
            if (showInternalStorage != null) {
                showInternalStorage.click()
            } else {
                device.pressBack()
            }
            device.findObject(By.desc("Show roots")).click()

            device.wait(Until.hasObject(By.text(INTERNAL_STORAGE_BUTTON_TEXT)), TIMEOUT_DURATION)
            device.findObject(By.text(INTERNAL_STORAGE_BUTTON_TEXT)).click()

            val workspaceDirectoryName = getDirectoryNameFromFullPath(Constants.workspaceDirectory)
            device.wait(Until.hasObject(By.text(workspaceDirectoryName)), TIMEOUT_DURATION)
            device.findObject(By.text(workspaceDirectoryName)).click()

            device.findObject(By.text("SELECT")).click()
            device.wait(Until.hasObject(By.res(REGISTRATION_SCREEN_CONTAINER)), TIMEOUT_DURATION)
        }

        private fun getDirectoryNameFromFullPath(fullPath: String): String {
            return fullPath.split('/').last()
        }

    }
}
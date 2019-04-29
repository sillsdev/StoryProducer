package org.sil.storyproducer.androidtest.runfirst

import android.app.Application

import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter

private const val APP_PACKAGE_NAME = "org.sil.storyproducer"
private const val TIMEOUT_DURATION = 5000L
private const val FILE_PICKER_PACKAGE = "com.android.documentsui"
private const val INTERNAL_STORAGE_BUTTON_TEXT = "Android SDK built for x86"
private const val REGISTRATION_SCREEN_CONTAINER = "org.sil.storyproducer:id/registration_scroll_view"

@RunWith(AndroidJUnit4::class)
class WorkspaceSetter {

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = PermissionsGranter.grantStoryProducerPermissions()

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
    @Test
    @SdkSuppress(minSdkVersion = 18)
    fun setWorkspaceSoOtherTestsRunCorrectly() {
        val device = UiDevice.getInstance(getInstrumentation())

        launchStoryProducerApp(device)

        if (isWorkspacePickerDisplayed(device)) {
            selectStoryProducerWorkspace(device)
        }
    }

    private fun launchStoryProducerApp(device: UiDevice) {
        device.pressHome()
        val launcherPackage = device.launcherPackageName
        assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIMEOUT_DURATION)

        val context = getApplicationContext<Application>()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(APP_PACKAGE_NAME)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
        device.wait(Until.hasObject(By.pkg(APP_PACKAGE_NAME).depth(0)), TIMEOUT_DURATION)
    }

    private fun isWorkspacePickerDisplayed(device: UiDevice): Boolean {
        val selectTemplateAlert = device.findObject(By.res("android:id/alertTitle").text("Select 'SP Templates' folder"))
        return selectTemplateAlert != null
    }

    private fun selectStoryProducerWorkspace(device: UiDevice) {
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
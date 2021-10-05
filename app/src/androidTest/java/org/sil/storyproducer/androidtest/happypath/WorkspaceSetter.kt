package org.sil.storyproducer.androidtest.happypath

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.Until.textMatches
import junit.framework.TestCase.assertNotNull
import org.junit.Rule
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PermissionsGranter
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator.isNotDisplayed
import org.sil.storyproducer.R
import java.util.regex.Pattern

// 09/02/2021 - DKH: Update for Testing Abstraction #566,
// APP_PACKAGE_NAMES were changed to detect between debug, continuous or released Story Producer
// version, so as to correctly record crashes for Firebase crashlytics
private const val APP_PACKAGE_NAME = "org.sil.storyproducer.debug"
private const val TIMEOUT_DURATION = 5000L
private const val FILE_PICKER_PACKAGE = "com.android.documentsui"

// 09/21/2021 - DKH: Update for Testing Abstraction #566
// Use scopedStorageUseFolderClick for Android 10 or greater for scoped file access acknowledgement
// This is a Regex expression that searches for multiple possible matches.
// Parenthesis are needed to define regex match term, but the parenthesis are
// not actually in the string we are looking for.
// These are the strings for emulators and phones and may change depending
// on the type of SDK version, emulator or phone hardware.  If they do,
// just add another string to the regex search pattern
private val A10_AccessOkString_Pixel4_ASEmulator = "(ALLOW ACCESS TO.*)"
private val A11_AccessOkString_Pixel4_ASEmulator = "(USE THIS FOLDER)"
private val A11_AccessOkString_Pixel5_Phone = "(Use this folder)"
// Use "|" in pattern to logical OR regex match terms
private val scopedStorageUseFolderClick = Pattern.compile(
        A10_AccessOkString_Pixel4_ASEmulator +
                "|" + A11_AccessOkString_Pixel4_ASEmulator +
                "|" + A11_AccessOkString_Pixel5_Phone
)

// 09/17/2021 - DKH: Update for Testing Abstraction #566
// The INTERNAL_STORAGE_BUTTON_TEXT differs between SDK versions.  It is used to locate the
// root file storage on the target device. These are the strings
//  when running on Android Studio with Debug version of Story Producer for
//  ANDROID 8,9,10,11 (SDK 27,28,29,30) on a Pixel emulator or phone
// This is a Regex expression that searches for multiple possible matches
// Parenthesis are needed to define regex match term, but the parenthesis are
// not actually in the string we are looking for.
// These are the strings for emulators and phones.  These may change depending
// on the type of SDK version, emulator or phone hardware.  If they do,
// just add another string to the regex search pattern
private val A8_InternalStorageButton_Pixel3_ASEmulator = "(Android SDK built for x86)"
private val A9_InternalStorageButton_Pixel2_ASEmulator = "(AOSP on IA Emulator)"
private val A10_InternalStorageButton_Pixel4_ASEmulator = "(Android SDK built for x86)"
private val A11_InternalStorageButton_Pixel4_ASEmulator = "(sdk_gphone_x86)"
private val A11_InternalStorageButton_Pixel5_Phone = "(Internal storage)"

private val INTERNAL_STORAGE_BUTTON_TEXT = Pattern.compile(
        A8_InternalStorageButton_Pixel3_ASEmulator +
                "|" + A9_InternalStorageButton_Pixel2_ASEmulator +
                "|" + A10_InternalStorageButton_Pixel4_ASEmulator +
                "|" + A11_InternalStorageButton_Pixel5_Phone +
                "|" + A11_InternalStorageButton_Pixel4_ASEmulator
)

private val allowPattern = Pattern.compile ("Allow|ALLOW")

private const val REGISTRATION_SCREEN_CONTAINER = "org.sil.storyproducer:id/registration_scroll_view"

/**
 * This "test" uses UIAutomator to set the Story Producer workspace directory.
 *
 *  09/21/2021 - DKH: Update for Testing Abstraction #566
 * >>>> Location of the workspace directory is now in
 * "StoryProducer/app/src/androidTest/java/org/sil/storyproducer/androidtest/happypath/base/ImageBase.kt"
 *
 * Be sure that the workspace directory actually exists on the device prior to running this test.
 *
 * This test is intended to be run once prior to all the Espresso tests, since those tests
 * assume that the workspace directory has already been selected by the user. Every time
 * the app's storage gets cleared, the workspace information gets lost. When this happens,
 * this test then needs to be run again, to select the workspace.
 *
 * This test was developed assuming a Nexus 5X emulator running Android API 28 for x86.
 * Depending on your device/emulator, you may need to modify the INTERNAL_STORAGE_BUTTON_TEXT
 * string.
 *
 * 09/21/2021 - DKH: Update for Testing Abstraction #566
 * Updated to run on the following:
 * Android 8, Android Studio Emulator, Pixel 3
 * Android 9, Android Studio Emulator, Pixel 2
 * Android 10, Android Studio Emulator, Pixel 4
 * Android 11, Android Studio Emulator, Pixel 4
 * Android 11, Phone Hardware, Pixel 5
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
        // 10/04/2021 - DKH: Espresso test fail for Android 10 and 11 #594
        // Expose context for use in DocumentFile copy operations.  Context keeps
        // track of file descriptors and streams.
        fun getContext():Context {
            return(getApplicationContext<Application>())
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

            // 09/02/2021 - DKH: Update for Testing Abstraction #566
            // Added "device.wait" which allows menu to popup before searching for a menu pick.
            // Encountered intermittent failures due to searching for the object before the object appeared
            device.wait(Until.hasObject(By.res("android:id/button1").text("OK")), TIMEOUT_DURATION)
            device.findObject(By.res("android:id/button1").text("OK")).click()
            device.wait(Until.hasObject(By.desc("More options")), TIMEOUT_DURATION)
            device.findObject(By.desc("More options")).click()

            // 09/02/2021 - DKH: Update for Testing Abstraction #566
            // Added "device.wait" which allows menu to popup before searching for a menu pick.
            // Encountered intermittent failures due to searching for the object before the object appeared
            device.wait(Until.hasObject(By.text("Show internal storage")), TIMEOUT_DURATION)

            val showInternalStorage = device.findObject(By.text("Show internal storage"))
            if (showInternalStorage != null) {
                showInternalStorage.click()
            } else {
                // since our option is not available in the menu, get rid of the menu
                device.pressBack()
            }
            // 09/02/2021 - DKH: Update for Testing Abstraction #566
            // Added "device.wait" which allows window to get setup before picking an item
            device.wait(Until.hasObject(By.desc("Show roots")), TIMEOUT_DURATION)

            // 09/21/2021 - DKH: Update for Testing Abstraction #566
            // Depending on the device, this may or may not show
            val showRoots = device.findObject(By.desc("Show roots"))
            if (showRoots != null) {
                showRoots.click()
            }


            device.wait(Until.hasObject(By.text(INTERNAL_STORAGE_BUTTON_TEXT)), TIMEOUT_DURATION)

            // 09/21/2021 - DKH: Update for Testing Abstraction #566
            // Depending on the device, this may or may not show
            val isButton = device.findObject(By.text(INTERNAL_STORAGE_BUTTON_TEXT))
            if (isButton != null) {
                isButton.click()
            }

            val workspaceDirectoryName = getDirectoryNameFromFullPath(Constants.workspaceDirectory)
            device.wait(Until.hasObject(By.text(workspaceDirectoryName)), TIMEOUT_DURATION)
            device.findObject(By.text(workspaceDirectoryName)).click()

            // 09/17/2021 - DKH: Update for Testing Abstraction #566
            // The advent of Android 10 introduced scoped storage where the user has to
            //  okay the use of files and directories.  Add extra clicks to okay the use of
            //  external storage
            if(Build.VERSION.SDK_INT > 28){
                // 09/17/2021 - DKH: Update for Testing Abstraction #566
                // Android 10 and greater must get user to okay the use of directories & files
                // These next lines of code allow the picker to acknowledge the use of the directories and files
                //  Use a Regex expression to search for Android 10 or 11 key words

                // File picker waits and then  acknowledges the use of selected folder
                device.wait(Until.hasObject(By.text(scopedStorageUseFolderClick)), TIMEOUT_DURATION)
                device.findObject(By.text(scopedStorageUseFolderClick)).click()

                // File picker waits and then Allows Story Producer to access files in selected folder
                device.wait(Until.hasObject(By.text(allowPattern)), TIMEOUT_DURATION)
                device.findObject(By.text(allowPattern)).click()
            }else {
                device.findObject(By.text("SELECT")).click()
            }
            device.wait(Until.hasObject(By.res(REGISTRATION_SCREEN_CONTAINER)), TIMEOUT_DURATION)
        }

        private fun getDirectoryNameFromFullPath(fullPath: String): String {
            return fullPath.split('/').last()
        }

    }
}
#!/usr/bin/env sh
# Pass in the following parameters:
# $1 is the device type number
# $2 is the API revision
# $3 is the type of image, either google_apis or google_apis_playstore

# Create the AVD for the emulator:
echo Creating AVD
avdmanager create avd -f -n dev_$1_$2_$3 -k "system-images;android-$2;$3;x86" -d $1

# Start the emulator with the AVD:
echo Starting emulator
$ANDROID_SDK_ROOT/emulator/emulator -no-snapshot-save -avd dev_$1_$2_$3 &

# Wait for the emulated device to boot up:
echo Waiting for device to boot up
while [ "`adb shell getprop sys.boot_completed | tr -d '\r' `" != "1" ] ; do sleep 1; done
echo Device has booted.

# Clean the app since we are changing devices and just building doesn't always work right
echo Cleaning the app
./gradlew :clean
./gradlew :app:clean

# Build the app and tests:
echo Building the app
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:installDebug :app:installDebugAndroidTest

# While we were doing the above Gradle processing, the emulated device might have gone to sleep
# so wake it up before trying to access it again.
echo Waking device
adb shell input keyevent KEYCODE_WAKEUP

# Clear out a previously created workspace, ignoring any errors.
echo Deleting workspace
adb shell rm -rf sdcard/SPWorkspace

# Create a workspace.
echo Creating workspace
adb shell mkdir sdcard/SPWorkspace

# Copy the Bloom file to the device's workspace.
echo Copying Bloom file
adb push "app/EspressoTestData/002 Lost Coin.bloom" sdcard/SPWorkspace

# Sometimes the app would not find the .bloom file, so give it a chance to really get there?
sleep 5

# Run the test:
echo Run the test
adb shell am instrument -w -e package org.sil.storyproducer.androidtest.happypath -e debug false org.sil.storyproducer.debug.test/androidx.test.runner.AndroidJUnitRunner > results_$1_$2.txt

# Now that the test is complete, shut down the emulator
echo Shutting down emulator
adb emu kill

# Give the emulator time to shut down before we delete its AVD
sleep 20

# Delete the AVD
echo Deleting AVD
avdmanager delete avd -n dev_$1_$2_$3
echo Done!


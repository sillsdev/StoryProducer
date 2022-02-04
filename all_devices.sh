
#export ANDROID_SDK_ROOT=~/Android/Sdk
#export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools
#export JAVA_HOME=~/AndroidStudio/jre

# TEMPORARY: Use SDK Tim installed?
export TEMP_ANDROID_SDK_ROOT=/home/bob/AndroidSDK

# Only test one device until we get the scripts working right.

echo Test a Pixel 2 at Android 8.0:
./one_device.sh 16 26 google_apis_playstore

# Test a Pixel 2 at Android 8.1:
#./one_device.sh 16 27 google_apis_playstore

# Test a Pixel 2 at Android 9.0:
#./one_device.sh 16 28 google_apis_playstore

# Test a Pixel 2 at Android 10.0:
#./one_device.sh 16 29 google_apis_playstore

# Test a Pixel 2 at Android 11.0:
#./one_device.sh 16 30 google_apis_playstore


# Test a Pixel 4 XL at Android 8.0:
#./one_device.sh 23 26 google_apis

# Test a Pixel 4 XL at Android 8.1:
#./one_device.sh 23 27 google_apis

# Test a Pixel 4 XL at Android 9.0:
#./one_device.sh 23 28 google_apis

# Test a Pixel 4 XL at Android 10.0:
#./one_device.sh 23 29 google_apis

# Test a Pixel 4 XL at Android 11.0:
#./one_device.sh 23 30 google_apis


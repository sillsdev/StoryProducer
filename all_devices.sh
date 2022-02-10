
export ANDROID_SDK_ROOT=~/AndroidSDK
export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator
export JAVA_HOME=~/AndroidStudio/jre
echo $PATH

echo Discover where things are and what they are called
ls -alF $ANDROID_SDK_ROOT
ls -alF $ANDROID_SDK_ROOT/cmdline-tools/latest/bin
ls -alF $ANDROID_SDK_ROOT/platform-tools
ls -alF $ANDROID_SDK_ROOT/emulator

# Get keystore for signing the Story Producer app
echo Getting keystore
curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/dev-keystore.jks" -o "dev-keystore.jks"
curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/keystore.properties" -o "keystore.properties"

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


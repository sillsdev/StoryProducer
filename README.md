# Story Producer (SP app)

Purpose: to enable end users to translate still-picture audio-video stories with community help, then dramatize, publish and digitally share new local language videos. 

End users and target audience: speakers of (minority) languages, some of whom are bilingual in a major language

End recipients: members of local language communities

Developers:  
   * Vision and design by Robin Rempel, SIL
   * Logo by Jordan Skomer and Mackenzie Sanders
   * Software engineers for Prototype app: LeTourneau University students (Jordan Skomer, Hannah Brown, Caleb Phillips)
   * Software engineers for v1.2: Cedarville University students (Noah Bragg, Grant Dennison, Robert Jacubec, Andrew Lockridge, Michael Baxter and Abigail Riffle guided by Dr. Seth Hamman)
   * Software engineers for Remote Oral Consultant Checker website and app v1.4 and v2.0: Cedarville University students (Blake Lasky, Daniel Weaver, Ann Costantino and Brendon Burns guided by Dr. Seth Hamman)
   * Software engineer for v2.3 and later: John Lambert
   * Software engineers for Remote Oral Consultant Checker website and app v2.4 and testing frameworks: Cedarville Univesity students (Aaron Cannon, Justin Stallard, Jonny Taylor, Ben Ratkey, Lindsey Carroll, Nathan Herr)

Translate and produce stories (starting with templates in a major language made up of images, audio and text) by working through these phases:  
* [REGISTER project and team information (one time)]
* LEARN or internalize the story
* TRANSLATE/DRAFT an audio translation
* COMMUNITY CHECK the draft for naturalness and clarity
* CONSULTANT CHECK for accuracy and approval
* DRAMATIZE/VOICE STUDIO the final audio
* FINALIZE/CREATE new videos
* SHARE the videos

## Creating a signed APK

To install onto a device, an APK needs to be signed. When uploading to Google Play, it has to be signed by the same
keystore that was used on the initial upload. For testing on a device using side loading, it can be signed with any
keystore. We do not want to check in a keystore to the repo, so developers can either download a pre-made set of files or
create their own.

To download pre-made set of files:
* run these commands in the root directory
    * `curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/dev-keystore.jks" -o "dev-keystore.jks"`
    * `curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/keystore.properties" -o "keystore.properties"`

To create the set of files:
* [create a keystore](https://stackoverflow.com/questions/3997748/how-can-i-create-a-keystore) (a .jks file) and
save in the root directory.
* create keystore.properties with the values:
    * storeFile=<keystore-filename>.jks
    * storePassword=<store-password>
    * keyAlias=<key-alias>
    * keyPassword=<key-password>
* run `gradle clean assembleRelease`

## Installing the application
* Minimum Requirements:  
    * Android 5.x.x; Android 8 is required to create 3GP output videos
    * 2+GB RAM is needed to create the longer/larger videos
    * an average of 500 MB storage must be available per story template one wishes to create; it is suggested to put templates on an SD card
    * an (Android) file manager
* Prepare your Android device: 
    * If you have installed Story Producer before, delete the previous version. If the version of the app that you delete is pre-2.3, deleting the app will delete all the template folders and any work you did on the translations.  After 2.3, the data is stored in a different location and nothing is lost.
    * Enable a manual install of apk file: 
        * Settings(gear icon) -> Security -> (scroll down) Enable "Unknown sources"; disable "Verify apps"
    * Insert SD card if desired
         * You will need approximately 500 MB of storage for each story you wish to produce.
    * Connect to your device via USB (it will show as a drive on your computer)
        * If it does not show up as a drive, swipe down from the top.  You should see a notification: USB for charging.  Touch it.  Select USB for file transfer.
    * Download StoryProducer_v[most recent version].apk and copy the file onto your phone or tablet. Use your file manager on your Android device to open the apk and install it onto your device.
    * Download the [template folders](https://drive.google.com/drive/folders/0Bw7whhMtjqJTVkljWlY0akZXeDg?usp=sharing) to your computer
    * Copy the unzipped templates folder to your SD card.  A good directory would be **\[thumb drive\]/SP Templates**
    * Open Story Producer and select the "SP Templates" which is the folder that all the templates are in.  (Not the folder of a specific template, but the folder that contains the templates.)
    * Continue with registration and use the app!

## Unit Tests
#### Organization
* All unit tests are located in the `app\src\test\java\org\sil\storyproducer\test` directory.
* Test files are named for the class that they test, plus the word "Test" prepended.
    * >**Example:** `TestRegistrationActivity` contains tests for the `RegistrationActivity` class.
* Folders in the unit test directory correspond to folders in the source directory.
    * >**Example:** `org.sil.storyproducer.test/controller/` contains tests for code that lives in `org.sil.storyproducer/controller/`
* Individual tests are named according to the following format: `FunctionName_When_[Condition]_Should_[DoExpectedBehavior]`
    * >**Example:** `OnCreate_When_WorkspaceDirectoryIsAlreadySet_Should_NotStartFileTreeActivity`

#### Running the Unit Tests
##### From the command line:
1. Navigate to the root directory of the repository.
2. Run `./gradlew test` (on Linux) or `gradlew.bat test`(on Windows).
*Note:* You may need to run the gradle wrapper with sudo or make the gradle wrapper executable with `sudo chmod +x ./gradlew`
##### From Android Studio:
1. Open the Story Producer project (StoryProducer.iml) in Android Studio.
2. Set the "Project" tool window to show "Local Unit Tests" (or just navigate a the folder containing unit tests).
3. Right click on one of the files or directories that contains some unit tests (this can be the "app" directory, a specific folder, or a single test file.).
4. Click "Run 'All Tests'" (or a more specific option if you chose a folder or file).
5. The "Run" tool window shows the results of the tests.
*Note:* If no tests appear in the "Run" window, you may need to toggle the visibility of passing tests. Currently, the toggle button looks like green checkmark inside of a circle.

## Espresso Tests (UI Tests)
#### Organization
* All Espresso tests are located in the `app\src\androidTest\java\org\sil\storyproducer` directory.
* Folders in the Espresso test directory should roughly correspond to the screen and/or features they exercise.

#### Before You Run the Espresso Tests:
The Espresso tests make a couple of assumptions about the state of the emulator/device that they run on. In order for the tests to pass, you must do the following:
1. Create a directory on the phone to act as the Story Producer workspace. The path needs to match the "pathToWorkspaceDirectory" constant defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
2. Create a directory on the phone to store resource files that the Espresso tests use. The path needs to match the "pathToEspressoResourceDirectory" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
3. Copy the "Lost Coin" story template into the espresso resource directory you created in step #2. The name of the directory needs to match the "nameOfTestStoryDirectory" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
4. Copy an .mp4 video file (the particular length or content doesn't matter) into the espresso resource directory. The name of the .mp4 file needs to match the "nameOfSampleExportVideo" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
5. Launch your emulator device (or connect a physical one via adb).
6. Run `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:installDebug :app:installDebugAndroidTest` from the root directory of the repository.
7. Run `adb shell am instrument -w -e debug false -e package 'org.sil.storyproducer.androidtest.runfirst' org.sil.storyproducer.test/androidx.test.runner.AndroidJUnitRunner` from the root directory of the repository. (Note that the folder containing ADB must be in your path for this command to work.)

> **Why is this necessary?** The Espresso tests rely on the presence of the "Lost Coin" template as a sample with which to exercise the features of the app. The Espresso Tests also require the workspace to have been set up, but Espresso is not capable interacting with the operating system's file picker, so the WorkspaceSetter class uses UIAutomator to select the workspace.

#### Running the Espresso Tests
##### From the command line:
1. **Ensure that you have set up your Android device according to the previous section, "Before You Run the Tests."** (The device/emulator should be running.)
2. Navigate to the root directory of the repository.
3. Run `adb shell am instrument -w -e debug false org.sil.storyproducer.test/androidx.test.runner.AndroidJUnitRunner`.
##### From Android Studio:
1. **Ensure that you have set up your Android device according to the previous section, "Before You Run the Tests."**
2. Open the Story Producer project (StoryProducer.iml) in Android Studio.
3. Set the "Project" tool window to show "Android Instrumented Tests" (or just navigate to a directory containing Espresso tests).
4. Right click on one of the directories that contains some Espresso tests (this can be the "app" directory, a specific folder, or a single test file.).
5. Click "Run 'Tests in org.sil.storyproducer...'".
6. The "Run" tool window shows the results of the tests.
*Note:* If no tests appear in the "Run" window, you may need to toggle the visibility of passing tests. Currently, the toggle button looks like green checkmark inside of a circle.


# Story Publisher Adv (SPadv)

Purpose: to enable end users to translate still-picture audio-video stories with community help, then dramatize, publish and digitally share new local language videos. 

End users and target audience: speakers of (minority) languages, some of whom are bilingual in a major language

End recipients: members of local language communities

Developers:  
   * Vision and design by Robin Rempel, SIL
   * Logo by Jordan Skomer and Mackenzie Sanders
   * Software engineers for Prototype app: LeTourneau University students (Jordan Skomer, Hannah Brown, Caleb Phillips)
   * Software engineers for v1.2: Cedarville University students (Noah Bragg, Grant Dennison, Robert Jacubec, Andrew Lockridge, Michael Baxter and Abigail Riffle guided by Dr. Seth Hamman)
   * Software engineers for Remote Oral Consultant Checker website and app v1.4 and v2.0: Cedarville University students (Blake Lasky, Daniel Weaver, Ann Costantino and Brendon Burns guided by Dr. Seth Hamman)
   * Software engineer for v2.3-2.5: John Lambert
   * Software engineers for Remote Oral Consultant Checker website and app v2.3 and testing frameworks: Cedarville Univesity students (Aaron Cannon, Justin Stallard, Jonny Taylor, Ben Ratkey, Lindsey Carroll, Nathan Herr)
   * Software engineers for Film Producer: Cedarville Univesity students (Donald Shade,...)
   * Software engineer for v3.0.2: Chad Dalton
   * Software engineers for filtered story list, Film Producer, extensive refactoring based on v3.02: Cedarville Univesity students (Jake Allinson, Nathan ONeel, Clare)
   * Software engineer for v3.0.4 - current : Dale Hensley

Translate and produce stories (starting with templates in a major language made up of images, audio and text) by working through these phases:  
* [REGISTER project and team information (one time)]
* LEARN or internalize the story
* TRANSLATE+REVISE an audio translation
* COMMUNITY CHECK the draft for naturalness and clarity
* ACCURACY CHECK for Biblical accuracy and approval
* VOICE STUDIO to dramatize the final audio
* FINALIZE create new videos
* SHARE the videos


## RK 2/2/23 with contributions from Dale Hensley

######
###### Usful info for getting started on SPadv
######

   [ Note:  I am using Ubuntu as my development platform, so my comments will
     favor Ubuntu.  Prior to me, Dale Hensley was using Windows and followed
     a very similar path.  ]

###
### GitHub
###     
   For me, the first step in getting Story Publisher up and running on my PC was 
   to install the techteam25 Github in my Github account and then clone a local copy 
   on my PC.
   
   github-desktop  is a useful application.  Many of the git operations can
   be accessed through the UI rather than the command line.  I used the following
   to install it on Ubuntu
   
        sudo wget https://github.com/shiftkey/desktop/releases/download/release-3.1.1-linux1/GitHubDesktop-linux-3.1.1-linux1.deb
        sudo apt-get install gdebi-core 
        sudo gdebi GitHubDesktop-linux-3.1.1-linux1.deb
   
   [Adapted from Chris Hubbard [SIL] document and updated for Story Publisher and techteam25
   1. Clone Locally (this used SSH Keys to authenticate with git which is more convenient than using https and username/password)

       git clone https://github.com/techteam25/SPadv.git   
       cd SPadv
       git config --global user.email "you@example.com"
       git config --global user.name "Your Name"   
       git remote add upstream git@github.com:techteam25/SPadv    

   2. Do Your work
       Make a branch <branch_name> using the github UI
       git checkout <branch_name>
       git add                       // add the changed/new files to the branch transaction
       git status                    // review your changes
       git commit                    // commit your changes to the branch

   3. Update your local develop and rebase your branch
       git checkout main            // goes back to main branch
       git pull --rebase            // update your main to the latest
       git checkout <branch_name>   // back to the bramch
       git merge main               // merge branch with the updated main, fix any conflicts

   4. Build and test your changes with the lates2 updates, then commit again
       git commit                    // commit your changes to the branch

   5. Push the changes to the repository
      git push -u origin


    

###
### Android Studio
###
   The next step is to install Android Studio.  [From Dale for Windows: Android    
   Studio comes with Java 11 so you don't need to install Java.  It is suggested 
   to use the version that comes with Android Studio.  I installed a version but 
   I later uninstalled it.]

   For Windows/Mac, refer to the following website:
      https://developer.android.com/studio/install
      
   For Ubuntu, I used the following steps.
      Step 1: Install Snap  <-- hopefully this is already installed by default
         sudo apt update
         sudo apt install snapd

      Step 2: Install JDK
         sudo apt update
         sudo apt install openjdk-11-jdk

      Step 3: Install Android Studio
         sudo snap install android-studio --classic

      If you wish to uninstall Android Studio using Snap then follow the command given below:
         sudo snap remove android-studio
         
   The first time I invoked Android Studio, it needed to do more installation/configuration
   and this took a while.

   Once Android Studio is installed/configured, point Android Studio at your local GIT 
   repository.  Dale used the menu "File->New->Import", while I just used the menu
   "File->Open" and pointed    it at the installation directory: git-storage/SPadv
      
   This will sync Android Studio to GIT and Android Studio will track your 
   current GIT branch.  If you change branches in GIT, it will appear in Android 
   Studio.  As the project was getting configured, it had to update gradle and this
   took a while, so wait until all that is finished.
      
   Story Publisher has its own gradle files so you should be able to build Story 
   Publisher, using Build->Make Project.  My build initially failed and needed the
   following change to successfully complete, in the following element, make sure that 
   the package name ends with ".debug" (line 12):
      SPadf/app/google-services.json
        "android_client_info": {
          "package_name": "org.tyndalebt.storyproduceradv.debug"
        }

   Once built, use the Device Manager (accessible from the menu "Tools->Device Manager") to
   point to either a virtual device (emulator) or a Physical Device (an Android 
   phone or tablet) 

   If you are using a phone connected via USB, you will need to 
   enable USB debugging on the phone.  There is a description of that here:
      https://developer.android.com/studio/debug/dev-options   
      
   You will probably want to create a couple of emulators on useful android
   versions, so you can test in various configurations.  To create an emulator,
   in the Device Manager, select the Virtual tab and click on Create device.  Pick 
   a phone in the first screen and pick an android version in the second screen.  
   In the second screen, you will need to download the android version that you
   select.  The download may take a while.  Once the download has finished, then
   you can finish creating the emulator.
   
   To run SPadv, you will need to select a device in the device manager where
   you want to run the product.  If you select an emulator, click the arrow to 
   start the emulator and give the emulator a chance to start up.  Then you can
   run the app on your selected device using the menu "Run->Run 'app'"  Similarly,
   you can debug the product using the menu "Run->Debug 'app'"
  
###
### More Useful Git Help
###    
   Next, you will find github page for SPadv useful.  This is accessed from the following URL
   https://github.com/techteam25/SPadv.  Look first at the Issues tab and the Projects 
   tab.  Issues mostly has the items that Robin has entered and Projects has 
   the currently active items that TechTeam is working on.   
   
   On the right hand side of the main page (below the Code dropdown) there is
   a link to all the commits that have been made in the codebase and this could 
   be interesting.

   gitk is a graphical tool that lets you explore the git repository.  It shows
   you all the branches in your repository and all the change that were made
   and who made them.  Also useful will be a chance to review your own changes
   that you are currently making before you commit them.
   
   In ubuntu, I used the following commands to install gitk:
      sudo apt update
      sudo apt -y install gitk
      
   adb stands for Android Debug Bridge and is a command line tool that allows you  
   to communicate with your device (either the emulator of android phone).  You
   can push/pull files to and from the device (useful for tests) and you can also 
   run a shell on    the device to investigate what is going on there, looking at 
   the files that    have been created and the processes that are running.  To 
   run the shell on your device, simply type "adb shell"
      
   In ubuntu, I used the following commands to install:
      sudo apt update
      sudo apt install android-tools-adb 
      
      adb version   <-- checks that it is running
      adb devices   <-- tells you what devices are available
      adb shell     <-- runs shell on the device
      adb -s devicename shell  <-- if more than one device running, connects a shell.

   From Dale for Windows users:
      I usually do a lot of stuff from the 
      bash shell and the power shell (sorry I can't help you out on the Mac but I 
      am sure it is similar):
      
      > Here is the location of gitk on my PC: C:\Program Files\Git\mingw64\bin\gitk
      > Here is the location of bash on my PC: C:\Program Files\Git\usr\bin\bash.exe
      > Here is the location of adb on my PC: /c/Users/bdhensley/AppData/Local/Android/SDK/platform-tools/adb.exe

###
### Unit Tests
###

   * All unit tests are located in the following directory:
      SPadv/app/src/test/java/org/tyndalebt/storyproduceradv/test
   * Test files are named for the class that they test, plus the word "Test" 
      in the beginning of end of the file name.
         **Example:** `TestRegistrationActivity` contains tests for the 
            `RegistrationActivity` class.
   * Folders in the unit test directory correspond to folders in the source directory.
         **Example:** `storyproduceradv/test/controller/` contains tests for 
            code that lives in `storyproduceradv/controller/`
   * Individual tests are named according to the following format: 
      `FunctionName_When_[Condition]_Should_[DoExpectedBehavior]`
         **Example:** `OnCreate_When_WorkspaceDirectoryIsAlreadySet_Should_NotStartFileTreeActivity`

   To run the tests from Android Studio:
   1. Open the Story Publisher project (SPadv) in Android Studio.
   2. Set the "Project" tool window to show "Tests" from the project dropdown
      (or just navigate a the folder containing unit tests).
   3. To run an indiviual test, right cight click on one of the test files 
      and select the menu "Run 'TestName'" 
   4. To run all the tests in a directory, right click on the directory that 
      contains some unit tests and select the menu "Run 'Tests in DirName'"
   5. To run all the tests, right click on the app directory and select 
      the menu "Run 'All Tests'"
   5. The "Run" tool window shows the results of the tests.
      *Note:* If no tests appear in the "Run" window, you may need to toggle the 
      visibility of passing tests. Currently, the toggle button looks like green 
      checkmark inside of a circle.

   To run the Unit Tests from the command line:  (Note: I have not done this yet)
   1. Navigate to the root directory of the repository.
   2. Run `./gradlew test` (on Linux) or `gradlew.bat test`(on Windows).
      *Note:* You may need to run the gradle wrapper with sudo or make the gradle 
      wrapper executable with `sudo chmod +x ./gradlew`

   Issues (2/2/23):
     - TestParsePhotoStory was written with XML file formats in mind.  The product 
       code has been updated (to us json?) and the test needs to be updated.
       Currently all the tests have been disabled.
       
     - Running the tests gives an error that the targetSdkVersion 31 is greater
       than the maxSdkVersion 30.  Temporarily I modified the targetSdkVersion
       to 30 to be able to proceed.
    
      - There are errors in TestRegistrationActivitybecause the state was
        content.pm.action.REQUEST_PERMISSIONS rather than the expected states
        
      - There are errors in TestSplashScreenActivity.  One was another state
        issue RegistrationActivity vs WelcomeActvity.  There was also a 
        NullPointerException     


###
### Espresso Tests
###
   The Expresso Tests are still a work in progress.  Stay tuned for more info......
   From Dale:
   > The test data is in the "Espresso test data for SHA ID" commit and is 
      actually located in SHA ID: 40244005cf69ce (ignore the SHA ID listed in 
      the commit, it was correct but got changed in the pull and we haven't fixed 
      the commit title yet).  There is a README file (enclosed) and the test 
      data in a windows zip file (the SHA ID is incorrect in the Test File).  
      Let me know if you can't crack the zip file and I will send you a link.  
      For now, all the test will run successfully under Android 8/9 (API 27/28).  
      Android 10 changed the file access mechanism and now an app must ask the 
      user to use a file.  I am currently rewriting the Espresso test to use 
      the new file access mechanism for Android 10/11.  Someone else had 
      refactored Story Publisher to use the new file access mechanism.
      
     ----------------------
     More info:
     ## Espresso Tests (UI Tests)
      #### Organization
      * All Espresso tests are located in the `app\src\androidTest\java\org\sil\storyproducer` directory.
      * Folders in the Espresso test directory should roughly correspond to the screen and/or features they exercise.

      #### Before You Runhs_err_pid46149.log the Espresso Tests:
      The Espresso tests make a couple of assumptions about the state of the emulator/device that they run on. In order for the tests to pass, you must do the following:
      1. Create a directory on the phone to act as the Story Publisher workspace. The path needs to match the "pathToWorkspaceDirectory" constant defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
      2. Create a directory on the phone to store resource files that the Espresso tests use. The path needs to match the "pathToEspressoResourceDirectory" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
      3. Copy the "Lost Coin" story template into the espresso resource directory you created in step #2. The name of the directory needs to match the "nameOfTestStoryDirectory" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
      4. Copy an .mp4 video file (the particular length or content doesn't matter) into the espresso resource directory. The name of the .mp4 file needs to match the "nameOfSampleExportVideo" value defined in `app\src\androidTest\java\org\sil\storyproducer\androidtest\utilities\Constants.kt`
      5. Launch your emulator device (or connect a physical one via adb).
      6. Run `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:installDebug :app:installDebugAndroidTest` from the root directory of the repository.
      7. Run `adb shell am instrument -w -e debug false -e package 'storyproduceradv.androidtest.runfirst' storyproduceradv.test/androidx.test.runner.AndroidJUnitRunner` from the root directory of the repository. (Note that the folder containing ADB must be in your path for this command to work.)

      > **Why is this necessary?** The Espresso tests rely on the presence of the "Lost Coin" template as a sample with which to exercise the features of the app. The Espresso Tests also require the workspace to have been set up, but Espresso is not capable interacting with the operating system's file picker, so the WorkspaceSetter class uses UIAutomator to select the workspace.

      #### Running the Espresso Tests
      ##### From the command line:
      1. **Ensure that you have set up your Android device according to the previous section, "Before You Run the Tests."** (The device/emulator should be running.)
      2. Navigate to the root directory of the repository.
      3. Run `adb shell am instrument -w -e debug false storyproduceradv.test/androidx.test.runner.AndroidJUnitRunner`.
      ##### From Android Studio:
      1. **Ensure that you have set up your Android device according to the previous section, "Before You Run the Tests."**
      2. Open the Story Publisher project (StoryPublisher.iml) in Android Studio.
      3. Set the "Project" tool window to show "Android Instrumented Tests" (or just navigate to a directory containing Espresso tests).
      4. Right click on one of the directories that contains some Espresso tests (this can be the "app" directory, a specific folder, or a single test file.).
      5. Click "Run 'Tests in storyproduceradv...'".
      6. The "Run" tool window shows the results of the tests.
      *Note:* If no tests appear in the "Run" window, you may need to toggle the visibility of passing tests. Currently, the toggle button looks like green checkmark inside of a circle.

## END - RK 2/2/23 





## Creating a signed APK

To install onto a device, an APK needs to be signed. When uploading to Google Play, it has to be signed by the same
keystore that was used on the initial upload. For testing on a device using side loading, it can be signed with any
keystore. We do not want to check in a keystore to the repo, so developers can either download a pre-made set of files or
create their own.

To download pre-made set of files:
* run these commands in the root directory
    * `curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/dev-keystore.jks" -o "dev-keystore.jks"`
    * `curl "https://sil-storyproducer-resources.s3.amazonaws.com/dev/keystore.properties" -o "keystore.properties"`
-------------
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
    * Android 5.x.x
    * 2+GB RAM is needed to create the longer/larger videos
    * an average of 500 MB storage must be available per story template one wishes to create; it is suggested to put templates on an SD card
    * an (Android) file manager
* Prepare your Android device: 
    * If you have installed Story Publisher before, delete the previous version. If the version of the app that you delete is pre-2.3, deleting the app will delete all the template folders and any work you did on the translations.  After 2.3, the data is stored in a different location and nothing is lost.
    * Enable a manual install of apk file: 
        * Settings(gear icon) -> Security -> (scroll down) Enable "Unknown sources"; disable "Verify apps"
    * Insert SD card if desired
         * You will need approximately 500 MB of storage for each story you wish to produce.
    * Connect to your device via USB (it will show as a drive on your computer)
        * If it does not show up as a drive, swipe down from the top.  You should see a notification: USB for charging.  Touch it.  Select USB for file transfer.
    * Download StoryPublisher_v[most recent version].apk and copy the file onto your phone or tablet. Use your file manager on your Android device to open the apk and install it onto your device.
    * Download the .bloom template files (https://drive.google.com/drive/folders/1CxpggJUJ6QPnNgb3Veh9r7SWiLfPKCDj?usp=sharing) to your computer.
    * Co-------------py the the .bloom files to your SD card.  The standard folder there is **\[SDCARD]/SP Templates**
    * Open Story Publisher and select the folder "SP Templates" which is the folder that all the templates are in.
    * After the app scans the templates, continue with registration and use the app!



## Word Links (WL)
* WLs are imported in the workspace, wordlinks/wordlinks.csv is read from the workspace path

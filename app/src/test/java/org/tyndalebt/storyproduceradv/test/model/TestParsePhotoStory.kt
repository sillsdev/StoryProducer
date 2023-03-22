package org.tyndalebt.storyproduceradv.test.model

import android.os.Build
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.tyndalebt.storyproduceradv.controller.SplashScreenActivity
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.Workspace.registration
import java.io.File


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])    // our robolectric version (4.5.1) is not updated to 31 yet
class TestParsePhotoStory {
    @Test
    fun parsePhotoStoryTest() {
        setupWorkspace()
       val myStory = loadStory()
       Assert.assertNotNull("Unable to open story", myStory)
       checkStoryContents(myStory)

       // Future: may want to edit the data, resave story.json, reload, and check
       // the modified contents.
       //
       // If so, may want to make a copy of the data directory and operate
       // on the copy in order to preserve the test data to be used for
       // other tests.
    }

    private fun setupWorkspace() {

       //Workspace.initializeWorkspace(ctx)  // will this work?

       var df = androidx.documentfile.provider.DocumentFile.fromFile(File("app/sampledata"))
       if (!df.isDirectory) {
          df = androidx.documentfile.provider.DocumentFile.fromFile(File("sampledata"))
       }

       Workspace.workdocfile = df
       Workspace.isUnitTest = true
    }

   private fun loadStory() : Story? {
      var df2 = androidx.documentfile.provider.DocumentFile.fromFile(File("app/sampledata/002 Lost Coin"))
      if (!df2.isDirectory) {
         df2 = androidx.documentfile.provider.DocumentFile.fromFile(File("002 Lost Coin"))
      }

      val splashScreenActivity = startSplashScreenActivity()
      val myStory = Workspace.buildStory(splashScreenActivity, df2)
      return myStory
   }

    private fun checkStoryContents(myStory: Story?) {
       // Testing that the story contents have been properly loaded
        checkStoryContentsGeneral(myStory);
        checkStoryContentsBeginningSlide(myStory);
        checkStoryContentsNextSlide(myStory);
    }
    
    private fun checkStoryContentsGeneral(myStory: Story?) {
        Assert.assertFalse("Story should not be approved.", myStory!!.isApproved)
        Assert.assertTrue("Story lastSlideNum should be 0.",  myStory.lastSlideNum == 0)
        Assert.assertTrue("Story lastPhase should be LEARN.", myStory!!.lastPhaseType == PhaseType.LEARN)
        Assert.assertTrue("Expected number of slides should be 8.", myStory!!.slides.size.toInt() == 8)
    }
            
 
     private fun checkStoryContentsBeginningSlide(myStory: Story?) {
        val slide = myStory!!.slides[0]
        val pageNo = 0;
         
        Assert.assertEquals("Image file should be blank.  Slide: " + pageNo, slide.imageFile, "")
        Assert.assertEquals("Text file should be blank.  Slide: " + pageNo, slide.textFile, "")
        Assert.assertEquals("Title should be blank.  Slide: " + pageNo, slide.title, "")
        Assert.assertEquals("SubTitle should be blank.  Slide: " + pageNo, slide.subtitle, "Luke 15")

        Assert.assertTrue("Width value incorrect.  Slide: " + pageNo, slide.width.toInt() == -1)
        Assert.assertTrue("Height value incorrect.  Slide: " + pageNo, slide.height.toInt() == -1)

        val reference = "Luke 15:1-2, 15:8-10";
        val content = "Title Ideas:\nGod is happy when a person repents.\nThe Lost Coin.\nWhen God celebrates!"
        val narrationFile = "audio/5484e7a9-65ab-4108-8850-d6d240318254.mp3"
        Assert.assertEquals("Reference is not correct.  Slide: " + pageNo, slide.reference, reference)
        Assert.assertEquals("Content is not correct.  Slide: " + pageNo, slide.content, content)
        Assert.assertEquals("Narration file is not correct.  Slide: " + pageNo, slide.narrationFile, narrationFile)

        Assert.assertTrue("Volume value incorrect.  Slide: " + pageNo, 
                (slide.volume.toDouble() >= .3) && (slide.volume.toDouble() < .4))       
   
        Assert.assertNull("Crop should be null.  Slide: " + pageNo, slide.crop)                  
        // Assert.assertEquals(20, slide.crop!!.left.toLong())   // crop == null
        // Assert.assertEquals(40, slide.crop!!.top.toLong())
        // Assert.assertEquals(720, slide.crop!!.right.toLong())
        // Assert.assertEquals(540, slide.crop!!.bottom.toLong())

        Assert.assertEquals("Start motion left value incorrect.  Slide: " + pageNo, 0, slide.startMotion!!.left.toLong())  // 0
        Assert.assertEquals("Start motion top value incorrect.  Slide: " + pageNo, 0, slide.startMotion!!.top.toLong())   // 0
        Assert.assertEquals("Start motion right value incorrect.  Slide: " + pageNo, -1, slide.startMotion!!.right.toLong())  // -1
        Assert.assertEquals("Start motion bottom value incorrect.  Slide: " + pageNo, -1, slide.startMotion!!.bottom.toLong())  // -1
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 0, slide.endMotion!!.left.toLong())  // 0
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 0, slide.endMotion!!.top.toLong()) // 0
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, -1, slide.endMotion!!.right.toLong())  // -1
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, -1, slide.endMotion!!.bottom.toLong())  // -1
    }


     private fun checkStoryContentsNextSlide(myStory: Story?) {
        val slide = myStory!!.slides[1]
        val pageNo = 1;
        val content2 = slide.content  // "One day, Jesus was teaching the people. Some of those people were critical that Jesus ate and spoke with others [other people] who did bad things. So Jesus told this story:"
        val musicFile = myStory!!.slides[1].musicFile  // "continueSoundtrack"
 
         
        Assert.assertEquals("Image file value is incorrect.  Slide: " + pageNo, slide.imageFile, "1.jpg")  
        Assert.assertEquals("Text file should be blank.  Slide: " + pageNo, slide.textFile, "")
        Assert.assertEquals("Title is incorrect.  Slide: " + pageNo, slide.title,  "Title Slide 1")
        Assert.assertEquals("SubTitle is incorrect.  Slide: " + pageNo, slide.subtitle, "Subtitle Slide 1")

        Assert.assertTrue("Width value incorrect.  Slide: " + pageNo, slide.width.toInt() == 1185)
        Assert.assertTrue("Height value incorrect.  Slide: " + pageNo, slide.height.toInt() == 1005)

        val reference = "Luke 15:1-2";
        val content = "One day, Jesus was teaching the people. Some of those people were critical that Jesus ate and spoke with others [other people] who did bad things. So Jesus told this story:"
        val narrationFile = "audio/narration1.mp3"
        Assert.assertEquals("Reference is not correct.  Slide: " + pageNo, slide.reference, reference)
        Assert.assertEquals("Content is not correct.  Slide: " + pageNo, slide.content, content)
        Assert.assertEquals("Narration file is not correct.  Slide: " + pageNo, slide.narrationFile, narrationFile)

        Assert.assertTrue("Volume value incorrect.  Slide: " + pageNo, (slide.volume.toInt() == 0))
   
        Assert.assertNull("Crop should be null.  Slide: " + pageNo, slide.crop)                  

        Assert.assertEquals("Start motion left value incorrect.  Slide: " + pageNo, 244, slide.startMotion!!.left.toLong())  
        Assert.assertEquals("Start motion top value incorrect.  Slide: " + pageNo, 0, slide.startMotion!!.top.toLong())   
        Assert.assertEquals("Start motion right value incorrect.  Slide: " + pageNo, 1020, slide.startMotion!!.right.toLong())  
        Assert.assertEquals("Start motion bottom value incorrect.  Slide: " + pageNo, 657, slide.startMotion!!.bottom.toLong())  
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 70, slide.endMotion!!.left.toLong())  // 0
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 0, slide.endMotion!!.top.toLong()) // 0
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 1112, slide.endMotion!!.right.toLong())  // -1
        Assert.assertEquals("End motion left value incorrect.  Slide: " + pageNo, 882, slide.endMotion!!.bottom.toLong())  // -1
    }

    fun startSplashScreenActivity() : SplashScreenActivity {
        registration.complete = true
        val splashScreenActivity = Robolectric.buildActivity(SplashScreenActivity::class.java).create().get()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        return splashScreenActivity
    }
    
    /*

    @Test
    fun parsePhotoStoryXML_When_StoryFolderDoesNotExist_Should_ReturnNull() {
        setupWorkspace()
        val storyPath = Mockito.mock(androidx.documentfile.provider.DocumentFile::class.java)
        Mockito.`when`(storyPath.name).thenReturn("IDoNotExist")

        val result = parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), storyPath)

        Assert.assertNull(result)
    }

    @Test
    fun parsePhotoStoryXML_When_StoryHasNoSlides_Should_ReturnNull() {
        setupWorkspace()
        val storyPath = Mockito.mock(androidx.documentfile.provider.DocumentFile::class.java)
        Mockito.`when`(storyPath.name).thenReturn("StoryWithNoSlides")

        val result = parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), storyPath)

        Assert.assertNull(result)
    }
*/


}

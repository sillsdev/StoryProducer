package org.sil.storyproducer.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
// import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

import androidx.test.core.app.ApplicationProvider
import org.sil.storyproducer.tools.file.getChildDocuments

import java.io.File

// See below for some background to Roboelectric
// https://www.vogella.com/tutorials/Robolectric/article.html

@RunWith(RobolectricTestRunner::class)
class ParseBloomTBTest {

    var result: Story? = null   // saved 'one-time' parsed story for validation with each test

    init {
        // WARNING: parsing Bloom HTML Templates is not just read-only - in for some stories extra concatenated audio files could be produced
        result = parse_BTB_Story() // parse the SP authored template once, on class initialization
    }

    @Test
    fun parsed_BTB_Should_get_getChildDocuments() {
        Assert.assertTrue(getChildDocuments(ApplicationProvider.getApplicationContext(), "Chicken+and+Millipede").size > 0)
    }

    @Test
    fun parsed_BTB_Should_ReturnAStory() {
        Assert.assertNotNull(result)
        Assert.assertEquals(Story::class.java, result!!.javaClass)
    }

//    @Test
    fun parsed_BTB_Should_get_isSPAuthored_True() {
        // This currently cannot be tested as isSPAuthored has not been added as a member of the Story class
        // Assert.assertTrue(result!!.???)
    }

    @Test
    fun parsed_BTB_Should_get_StoryTitleFromBookFrontCover() {
        Assert.assertEquals("Chicken+and+Millipede", result!!.title)
    }

    @Test
    fun parsed_BTB_Should_get_ProvidedPagesPlusSong() {
        Assert.assertEquals(18, result!!.slides.size.toLong())
    }

    // this test initialization code should only need to be called one
    private fun parse_BTB_Story(): Story? {
        setupWorkspace()
        // NB: This story is currently not under git to save space.  Please download from: https://bloomlibrary.org/book/XV844n7KiK
        // and unzip to the test_data folder (also do not check into git in for now).
        val storyPath = Workspace.workdocfile.findFile("Chicken+and+Millipede")
        return parseBloomHTML(ApplicationProvider.getApplicationContext(), storyPath!!)
    }

    private fun setupWorkspace() {
//        println(System.getProperty("user.dir"))
        var df = androidx.documentfile.provider.DocumentFile.fromFile(File("app/src/test/test_data"))
        if(!df.isDirectory){
            df = androidx.documentfile.provider.DocumentFile.fromFile(File("src/test/test_data"))
        }
        Workspace.workdocfile = df
    }

}

package org.sil.storyproducer.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
// import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

import androidx.test.core.app.ApplicationProvider

import java.io.File

// See below for some background to Roboelectric
// https://www.vogella.com/tutorials/Robolectric/article.html

@RunWith(RobolectricTestRunner::class)
class ParseBloomTest {

    var result: Story? = null   // saved 'one-time' parsed story for validation with each test

    init {
        // WARNING: parsing Bloom HTML Templates is not just read-only - in for some stories extra concatenated audio files could be produced
        result = parse_SPAT_Story() // parse the SP authored template once, on class initialization
    }

    @Test
    fun parsed_SPAT_Should_ReturnAStory() {
        Assert.assertNotNull(result)
        Assert.assertEquals(Story::class.java, result!!.javaClass)
    }

//    @Test
    fun parsed_SPAT_Should_get_isSPAuthored_True() {
        // This currently cannot be tested as isSPAuthored has not been added as a member of the Story class
        // Assert.assertTrue(result!!.???)
    }

    @Test
    fun parsed_SPAT_Should_get_StoryTitleFromBookFrontCover() {
        Assert.assertEquals("000 Unlocked demo", result!!.title)
    }

    @Test
    fun parsed_SPAT_Should_get_ProvidedPagesPlusSong() {
        // should be 9 slides 7 numbered 1 front cover and 1 copyright
        Assert.assertEquals(9, result!!.slides.size.toLong())
    }

    // this test initialization code should only need to be called one
    private fun parse_SPAT_Story(): Story? {
        setupWorkspace()
        val storyPath = Workspace.workdocfile.findFile("000 Unlocked demo")
        return parseBloomHTML(ApplicationProvider.getApplicationContext(), storyPath!!)
    }

    private fun setupWorkspace() {
        println(System.getProperty("user.dir"))
        // NB: Using the unlocked demo story for now but in the future we should probably have dedicated test templates under git
        var df = androidx.documentfile.provider.DocumentFile.fromFile(File("app${File.separator}src${File.separator}main${File.separator}assets"))
        if(!df.isDirectory){
            df = androidx.documentfile.provider.DocumentFile.fromFile(File("src${File.separator}main${File.separator}assets"))
        }
        Workspace.workdocfile = df
    }

}

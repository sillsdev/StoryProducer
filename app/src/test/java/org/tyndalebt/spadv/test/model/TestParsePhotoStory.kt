package org.tyndalebt.spadv.test.model

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.tyndalebt.spadv.model.Story
import org.tyndalebt.spadv.model.Workspace

import androidx.test.core.app.ApplicationProvider

import org.tyndalebt.spadv.model.parsePhotoStoryXML
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TestParsePhotoStory {
    @Test
    fun parsePhotoStoryXML_Should_ReturnAStory() {
        println(System.getProperty("user.dir"))
        val result = parseValidStory()
        Assert.assertEquals(Story::class.java, result!!.javaClass)
    }

    @Test
    fun parsePhotoStoryXML_Should_ReturnAStoryWithProvidedSlidesPlusSongAndCreditSlides() {
        val result = parseValidStory()

        Assert.assertEquals(5, result!!.slides.size.toLong())
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseThePathOfTheImage() {
        val result = parseValidStory()

        Assert.assertEquals("0.jpg", result!!.slides[0].imageFile)
    }


    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseTheWidthOfTheImage() {
        val result = parseValidStory()

        Assert.assertEquals(720, result!!.slides[0].width.toLong())
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseTheHeightOfTheImage() {
        val result = parseValidStory()

        Assert.assertEquals(540, result!!.slides[0].height.toLong())
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_DeriveTextFileNameFromImageName() {
        val result = parseValidStory()

        Assert.assertEquals("0.txt", result!!.slides[0].textFile)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_GetSlideTitleFromCorrespondingTextFile() {
        val result = parseValidStory()

        Assert.assertEquals("The Valid Story", result!!.slides[0].title)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_GetSlideSubtitleFromCorrespondingTextFile() {
        val result = parseValidStory()

        Assert.assertEquals("When Testing Works", result!!.slides[0].subtitle)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImage_Should_GetSlideReferenceFromCorrespondingTextFile() {
        val result = parseValidStory()

        Assert.assertEquals("Exodus 20:20", result!!.slides[0].reference)
    }

    @Test
    fun parsePhotoStoryXML_When_AnySlideAfterFirstSlideHasAnImage_Should_GetSlideContentFromCorrespondingTextFile() {
        val result = parseValidStory()

        Assert.assertEquals("This is test content.", result!!.slides[1].content)
    }

    @Test
    fun parsePhotoStoryXML_When_HasFirstSlide_Should_GetSlideContentFromCorrespondingTextFile() {
        val result = parseValidStory()

        Assert.assertEquals("Once there was a sample story template that was formatted correctly.", result!!.slides[0].content)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasANarrationTag_Should_ParseTheNameOfTheNarrationFile() {
        val result = parseValidStory()

        Assert.assertEquals("firstSlideNarration.wav", result!!.slides[0].narrationFile)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAMusicTrack_Should_ParseTheNameOfTheSoundFile() {
        val result = parseValidStory()

        Assert.assertEquals("firstSlideMusicTrack.mp3", result!!.slides[0].musicFile)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAMusicTrack_Should_ParseTheVolume() {
        val result = parseValidStory()

        Assert.assertEquals(0.09, result!!.slides[0].volume.toDouble(), 0.01)
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAnImageWithRotateAndCrop_Should_ParseCropRectangle() {
        val result = parseValidStory()

        Assert.assertEquals(20, result!!.slides[0].crop!!.left.toLong())
        Assert.assertEquals(40, result.slides[0].crop!!.top.toLong())
        Assert.assertEquals(720, result.slides[0].crop!!.right.toLong())
        Assert.assertEquals(540, result.slides[0].crop!!.bottom.toLong())
    }

    @Test
    fun parsePhotoStoryXML_When_ASlideHasAMotion_Should_ParseStartAndEndRectangles() {
        val result = parseValidStory()

        val slide = result!!.slides[0]
        Assert.assertEquals(0, slide.startMotion!!.left.toLong())
        Assert.assertEquals(0, slide.startMotion!!.top.toLong())
        Assert.assertEquals(540, slide.startMotion!!.right.toLong())
        Assert.assertEquals(405, slide.startMotion!!.bottom.toLong())
        Assert.assertEquals(100, slide.endMotion!!.left.toLong())
        Assert.assertEquals(50, slide.endMotion!!.top.toLong())
        Assert.assertEquals(720, slide.endMotion!!.right.toLong())
        Assert.assertEquals(540, slide.endMotion!!.bottom.toLong())
    }

    @Test
    fun parsePhotoStoryXML_When_StoryHasExtraTagsInsideRoot_Should_IgnoreThem() {
        val result = parseValidStory()

        Assert.assertEquals(5, result!!.slides.size.toLong())
    }

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

    private fun parseValidStory(): Story? {
        setupWorkspace()
        val storyPath = Mockito.mock(androidx.documentfile.provider.DocumentFile::class.java)
        Mockito.`when`(storyPath.name).thenReturn("ValidStory")

        return parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), storyPath)
    }

    private fun setupWorkspace() {
        var df = androidx.documentfile.provider.DocumentFile.fromFile(File("app/sampledata"))
        if(!df.isDirectory){
            df = androidx.documentfile.provider.DocumentFile.fromFile(File("sampledata"))
        }

        Workspace.workdocfile = df
    }

}

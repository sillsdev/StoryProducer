package org.sil.storyproducer.test.model;

import android.support.v4.provider.DocumentFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.sil.storyproducer.model.Slide;
import org.sil.storyproducer.model.Story;

import androidx.test.core.app.ApplicationProvider;

import static org.sil.storyproducer.model.ParsePhotoStoryKt.parsePhotoStoryXML;

@RunWith(RobolectricTestRunner.class)
public class TestParsePhotoStory {
    @Test
    public void parsePhotoStoryXML_Should_ReturnAStory() {
        Story result = parseValidStory();

        Assert.assertEquals(Story.class, result.getClass());
    }

    @Test
    public void parsePhotoStoryXML_When_ThereAreTwoSlides_Should_ReturnAStoryWithTwoSlides() {
        Story result = parseValidStory();

        Assert.assertEquals(2, result.getSlides().size());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseThePathOfTheImage() {
        Story result = parseValidStory();

        Assert.assertEquals("0.jpg", result.getSlides().get(0).getImageFile());
    }


    @Test
    public void parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseTheWidthOfTheImage() {
        Story result = parseValidStory();

        Assert.assertEquals(720, result.getSlides().get(0).getWidth());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAnImage_Should_ParseTheHeightOfTheImage() {
        Story result = parseValidStory();

        Assert.assertEquals(540, result.getSlides().get(0).getHeight());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAnImage_Should_DeriveTextFileNameFromImageName() {
        Story result = parseValidStory();

        Assert.assertEquals("0.txt", result.getSlides().get(0).getTextFile());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasANarrationTag_Should_ParseTheNameOfTheNarrationFile() {
        Story result = parseValidStory();

        Assert.assertEquals("firstSlideNarration.wav", result.getSlides().get(0).getNarrationFile());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAMusicTrack_Should_ParseTheNameOfTheSoundFile() {
        Story result = parseValidStory();

        Assert.assertEquals("firstSlideMusicTrack.mp3", result.getSlides().get(0).getMusicFile());
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAMusicTrack_Should_ParseTheVolume() {
        Story result = parseValidStory();

        Assert.assertEquals(9, result.getSlides().get(0).getVolume(), 0.1);
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAnImageWithRotateAndCrop_Should_ParseCropRectangle() {
        Story result = parseValidStory();

        Assert.assertEquals(20, result.getSlides().get(0).getCrop().left);
        Assert.assertEquals(40, result.getSlides().get(0).getCrop().top);
        Assert.assertEquals(720, result.getSlides().get(0).getCrop().right);
        Assert.assertEquals(540, result.getSlides().get(0).getCrop().bottom);
    }

    @Test
    public void parsePhotoStoryXML_When_ASlideHasAMotion_Should_ParseStartAndEndRectangles() {
        Story result = parseValidStory();

        Slide slide = result.getSlides().get(0);
        Assert.assertEquals(0, slide.getStartMotion().left);
        Assert.assertEquals(0, slide.getStartMotion().top);
        Assert.assertEquals(540, slide.getStartMotion().right);
        Assert.assertEquals(405, slide.getStartMotion().bottom);
        Assert.assertEquals(100, slide.getEndMotion().left);
        Assert.assertEquals(50, slide.getEndMotion().top);
        Assert.assertEquals(720, slide.getEndMotion().right);
        Assert.assertEquals(540, slide.getEndMotion().bottom);
    }

    @Test
    public void parsePhotoStoryXML_When_StoryHasExtraTagsInsideRoot_Should_IgnoreThem() {
        Story result = parseValidStory();

        Assert.assertEquals(2, result.getSlides().size());
    }

    @Test
    public void parsePhotoStoryXML_When_StoryFolderDoesNotExist_Should_ReturnNull() {
        DocumentFile inputFile = Mockito.mock(DocumentFile.class);
        Mockito.when(inputFile.getName()).thenReturn("I do not exist");

        Story result = parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), inputFile);

        Assert.assertNull(result);
    }

    @Test
    public void parsePhotoStoryXML_When_StoryHasNoSlides_Should_ReturnNull() {
        DocumentFile inputFile = Mockito.mock(DocumentFile.class);
        Mockito.when(inputFile.getName()).thenReturn("Story with no slides");

        Story result = parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), inputFile);

        Assert.assertNull(result);
    }

    private Story parseValidStory() {
        DocumentFile inputFile = Mockito.mock(DocumentFile.class);
        Mockito.when(inputFile.getName()).thenReturn("Valid Story");

        return parsePhotoStoryXML(ApplicationProvider.getApplicationContext(), inputFile);
    }

}

package org.sil.storyproducer.tools.file;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

/**
 * ImageFiles represents an abstraction of the image resources for story templates.
 */
public class ImageFiles {
    private static final String TAG = "ImageFiles";

    private static final String FILE_EXTENSION = ".jpg";

    public static final int TITLE_BACKGROUND = -1;
    public static final int TITLE_TEMP = -3;
    public static final int COPYRIGHT = -2;

    private static final String TITLE_BACK_IMAGE_NAME = "title" + FILE_EXTENSION;
    private static final String TITLE_TEMP_IMAGE_NAME = "titleTemp" + FILE_EXTENSION;
    private static final String COPYRIGHT_IMAGE_NAME = "end" + FILE_EXTENSION;

    /**
     * Get an image file from the story template.
     * @param story name of the story template to query.
     * @param number 0-based slide number or special slide (e.g. {@link #TITLE_BACKGROUND}.
     * @return image file.
     */
    public static File getFile(String story, int number) {
        switch(number) {
            case TITLE_BACKGROUND:
                return new File(FileSystem.getProjectDirectory(story), TITLE_BACK_IMAGE_NAME);
            case TITLE_TEMP:
                return new File(FileSystem.getHiddenTempDirectory(story), TITLE_TEMP_IMAGE_NAME);
            case COPYRIGHT:
                return getCopyrightImageFile(story);
            default:
                return new File(FileSystem.getTemplatePath(story), number + FILE_EXTENSION);
        }
    }

    public static Bitmap getBitmap(String story, int number) {
        return getBitmap(story, number, 1);
    }
    public static Bitmap getBitmap(String story, int number, int sampleSize) {
        File f = getFile(story, number);

        if(f.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            return BitmapFactory.decodeFile(f.getPath(), options);
        }

        return null;
    }

    /**
     * Get the amount of number-based image files (e.g. "1.jpg") in the template of the story.
     * @param story name of the story template to query.
     * @return amount of number-based image files in the template of the story.
     */
    static int getNumberedAmount(String story) {
        String templateDirPath = FileSystem.getTemplatePath(story);
        if(templateDirPath == null) {
            return 0;
        }
        return FileSystem.getNumberedFilesAmount(new File(templateDirPath), null, FILE_EXTENSION);
    }

    private static File getCopyrightImageFile(String story) {
        File copyrightFile = new File(FileSystem.getTemplatePath(story), COPYRIGHT_IMAGE_NAME);
        if(!copyrightFile.exists()) {
            copyrightFile = getFile(story, getNumberedAmount(story) - 1);
        }
        return copyrightFile;
    }
}

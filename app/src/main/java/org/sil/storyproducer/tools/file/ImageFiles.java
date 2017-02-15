package org.sil.storyproducer.tools.file;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class ImageFiles {
    private static final String FILE_EXTENSION = ".jpg";

    public static final int TITLE_BACKGROUND = -1;
    public static final int TITLE_TEMP = -3;
    public static final int COPYRIGHT = -2;

    private static final String TITLE_BACK_IMAGE_NAME = "title" + FILE_EXTENSION;
    private static final String TITLE_TEMP_IMAGE_NAME = "titleTemp" + FILE_EXTENSION;
    private static final String COPYRIGHT_IMAGE_NAME = "end" + FILE_EXTENSION;

    public static File getFile(String story, int number) {
        switch(number) {
            case TITLE_BACKGROUND:
                return new File(FileSystem.getProjectDirectory(story), TITLE_BACK_IMAGE_NAME);
            case TITLE_TEMP:
                return new File(FileSystem.getHiddenTempDirectory(story), TITLE_TEMP_IMAGE_NAME);
            case COPYRIGHT:
                return new File(FileSystem.getTemplatePath(story), COPYRIGHT_IMAGE_NAME);
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

    public static int getAmount(String storyName) {
        String templateDirectory = FileSystem.getTemplatePath(storyName);
        if(templateDirectory == null) {
            return 0;
        }

        File[] files = new File(templateDirectory).listFiles();
        int totalPics = 0;
        int highestNumber = -1;

        for (File aFile : files) {
            String tempNumber;
            String fileName = aFile.toString();
            if (fileName.endsWith(FILE_EXTENSION)) {
                tempNumber = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf(FILE_EXTENSION));
                if (tempNumber.matches("^([0-9]+)$")) {
                    int checkingNumber = Integer.valueOf(tempNumber);
                    totalPics++;
                    if(checkingNumber > highestNumber) {
                        highestNumber = checkingNumber;
                    }
                }
            }
        }

        if(highestNumber + 1 > totalPics) {
            //TODO: handle missing pictures error case
        }

        return totalPics;
    }
}

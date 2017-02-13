package org.sil.storyproducer.tools.file;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class ImageFiles {
    public static final int SLIDE_NUMBER_COPYRIGHT = -2;

    private static final String COPYRIGHT_IMAGE_NAME = "end.jpg";

    private static final String FILE_EXTENSION = ".jpg";

    public static File getFile(String story, int number) {
        String name = number + FILE_EXTENSION;

        if(number == SLIDE_NUMBER_COPYRIGHT) {
            name = COPYRIGHT_IMAGE_NAME;
        }

        return new File(FileSystem.getTemplatePath(story), name);
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

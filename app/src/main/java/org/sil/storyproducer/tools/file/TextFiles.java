package org.sil.storyproducer.tools.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class TextFiles {
    private static String[] content;

    public static void loadSlideContent(String storyName, int slideNum) {
        File file = new File(FileSystem.getTemplatePath(storyName), (slideNum + ".txt"));
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }


        String text1 = text.toString();
        byte[] temp = text1.getBytes();
        for (int i = 0; i < temp.length - 2; i++) {
            //Swap out curly apostrophe with ASCII single quote
            if (temp[i] == -17 && temp[i + 1] == -65 && temp[i + 2] == -67) {
                text = text.replace(i, i + 1, "'");
                text1 = text.toString();
                temp = text1.getBytes();
            }
        }
        content = text.toString().split(Pattern.quote("~"));
    }

    public static String getTitle() {
        return content[0];
    }

    public static String getSubTitle() {
        return content[1];
    }

    public static String getSlideVerse() {
        return content[2];
    }

    public static String getSlideContent() {
        return content[3];
    }
}

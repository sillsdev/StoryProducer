package org.sil.storyproducer.tools.file;

import android.util.Log;

import org.sil.storyproducer.model.SlideText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * TextFiles represents an abstraction of the text resources for story templates.
 */
public class TextFiles {
    private static final String TAG = "TextFilesClass";

    private static final String FILE_TEXT_EXTENSION = ".txt";
    private static final String DRAMATIZATION_FILE_PREFIX = "dramatizationText";
    private static final String CHARACTER_ENCODING = "UTF-8";

    /**
     * Get the amount of number-based text files (e.g. "1.txt") in the template of the story.
     * @param story name of the story template to query.
     * @return amount of number-based text files in the template of the story.
     */
    static int getNumberedAmount(String story) {
        String templateDirPath = FileSystem.getTemplatePath(story);
        if(templateDirPath == null) {
            return 0;
        }
        return FileSystem.getNumberedFilesAmount(new File(templateDirPath), null, FILE_TEXT_EXTENSION);
    }

    public static SlideText getSlideText(String storyName, int slideNum) {
        String[] content;
        File file = new File(FileSystem.getTemplatePath(storyName), (slideNum + FILE_TEXT_EXTENSION));
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
            //TODO: add proper error handling here
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

        if (content.length == 4) {
            return new SlideText(content[0].trim(), content[1].trim(), content[2].trim(), content[3].trim());
        } else {
            Log.e(TAG, "Text file not found for " + storyName + " slide " + slideNum);
            return new SlideText();
        }
    }

     /** The purpose of this function is to get the EditText field in the dramatization phase.
     * @param storyName The story that the dramatization text is associated with.
     * @param slideNum The particular slide number that the text is associated with.
     * @return The text for the EditText field in dramatization phase.
     */
    public static String getDramatizationText(String storyName, int slideNum){
        StringBuilder dramTextBuilder = null;
        File dramFile = new File(FileSystem.getProjectDirectory(storyName), DRAMATIZATION_FILE_PREFIX + slideNum + FILE_TEXT_EXTENSION);
        if(dramFile.exists()){
            Scanner scanner;
            try{
                scanner = new Scanner(dramFile);
                dramTextBuilder = new StringBuilder();
                while(scanner.hasNextLine()){
                    dramTextBuilder.append(scanner.nextLine() + "\n");
                }
                scanner.close();
            }catch(IOException ex){
                Log.e(TAG, "Could not find dramatization text file");
            }
            return dramTextBuilder.toString();
        }else{
            return "";
        }
    }

    /**
     * The purpose of this function is to set the text for the EditText field in dramatization phase.
     * @param storyName The story that the dramatization text is associated with.
     * @param slideNum The particular slide number that the text is associated with.
     * @param text The text from the EditText field in dramatization phase.
     */
    public static void setDramatizationText(String storyName, int slideNum, String text){
        File dramFile = new File(FileSystem.getProjectDirectory(storyName), DRAMATIZATION_FILE_PREFIX + slideNum + FILE_TEXT_EXTENSION);

        try{
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                            new FileOutputStream(dramFile, false), Charset.forName(CHARACTER_ENCODING)), false);
            pw.write(text);
            pw.close();
        }catch(FileNotFoundException ex){
            Log.e(TAG, "Could not write to dramatization text file");
        }
    }
}

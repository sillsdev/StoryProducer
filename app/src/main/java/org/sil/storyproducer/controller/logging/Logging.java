package org.sil.storyproducer.controller.logging;

import org.sil.storyproducer.tools.FileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;

/**
 * Created by user on 1/22/2017.
 */

public class Logging {

    private static TreeSet<LogEntry> currentLog = null;
    private static String currentLang = null; //ethnologue code
    private static String currentStory = null;

    public static void makeEntry(LogEntry le, String ethnoCode, String storyTitle){
        if(currentLang != null && currentLang.equals(ethnoCode)
                && currentStory != null && currentStory.equals(storyTitle)
                && currentLog!=null){
            currentLog.add(le);
        } else {
            currentLang=ethnoCode;
            currentStory=storyTitle;
            currentLog = null;
            //TODO: find a place to put logs for each story, based on ethnode and title
            File dir = new File("PlaceWhereTheLogFileShouldBe");

            if(dir.exists() && dir.isDirectory()) {
                File f = new File(dir, "log.ser");
                if(f.exists()) {
                    try {
                        currentLog = (TreeSet<LogEntry>) loadObject(dir.getAbsolutePath() + "/log.ser");
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(currentLog == null){ //no pre-existing log file, or old file failed to load
                //TODO: handle log file deserialization failures better (right now we automatically overwrite)
                currentLog = new TreeSet<LogEntry>();
                currentLog.add(le);
                try {
                    saveObject(currentLog, dir.getAbsolutePath()+"/log.ser");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void saveObject(Object obj, String fileName) throws IOException {

        FileOutputStream fOut = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oOut = new ObjectOutputStream(fOut);
            try {
                oOut.writeObject(obj);
                System.out.println("Object saved.");
            } finally {
                oOut.close();
            }
        } finally {
            fOut.close();
        }
            /*if we can set minAPI level to 19, then we can use try-with-resources instead of this
            crazy stuff with three try blocks
            */
    }

    private static Object loadObject(String fileName) throws IOException, ClassNotFoundException {
        Object ret=null;

        FileInputStream fIn = new FileInputStream(fileName);
        try {
            ObjectInputStream oIn = new ObjectInputStream(fIn);
            try {
                ret = oIn.readObject();
            } finally {
                oIn.close();
            }
        } finally {
            fIn.close();
        }
        return ret;
    }

}

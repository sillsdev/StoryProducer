package org.sil.storyproducer.controller.logging;

import org.sil.storyproducer.model.StoryState;
import org.sil.storyproducer.tools.file.FileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by Michael D. Baxter on 1/22/2017.
 */

public class Logging {

    private static final String LOGS_ROOT_DIR = "/storage/emulated/0/splogs/";
    private static Log currentLog = null;
    private static String currentLang = null; //ethnologue code
    private static String currentStory = null;

    public static void saveLogEntry(LogEntry le, String ethnoCode, String storyTitle){
        saveLogEntries(Collections.singleton(le), ethnoCode, storyTitle);
    }

    public static void saveLog(Log log){
        saveLogEntries(log, log.getLang(), log.getStory());
    }

    public static boolean deleteLog(String ethnoCode, String storyTitle){
        File file = new File(LOGS_ROOT_DIR+ethnoCode+"/"+storyTitle+"/log.ser");
        return file.delete();
    }

    /**
     * Returns the log for the story with this language and title, or null if there isn't one.
     * @param ethnoCode
     * @param storyTitle
     * @return the log for the story with this language and title, or null if there isn't one
     */
    public static Log getLog(String ethnoCode, String storyTitle){
        File file = new File(LOGS_ROOT_DIR+ethnoCode+"/"+storyTitle+"/log.ser");
        Log ret = null;
        if(file.exists()) {
            try {
                ret = (Log) loadObject(file.getAbsolutePath());
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                ret = null;
            }
        }
        return ret;
    }

    public static void saveLogEntries(Collection <LogEntry> le){
        saveLogEntries(le, FileSystem.getLanguage(), StoryState.getStoryName());
    }

    public static void saveLogEntry(LogEntry le){
        saveLogEntry(le, FileSystem.getLanguage(), StoryState.getStoryName());
    }

    public static void saveLogEntries(Collection<LogEntry> le, String ethnoCode, String storyTitle){
        /*we need to make sure that currentLog contains a reference to valid log object, whether
        that means loading a log object by deserializing it, or creating a new log object
         */

        //TODO: find a place to put logs for each story, based on ethnocode and title
        File file = new File(LOGS_ROOT_DIR+ethnoCode+"/"+storyTitle+"/log.ser");
        File dir = file.getParentFile(); //yes, getParentFile gives you a directory. Not a file.

        if(! dir.exists()){
            dir.mkdirs();
        }

        if(currentLog == null
                || (! currentLog.getLang().equals(ethnoCode) )
                || (! currentLog.getStory().equals(storyTitle) )){
            currentLog = null;

            currentLog = getLog(ethnoCode, storyTitle);

            if(currentLog == null) { //no pre-existing log file, or old file failed to load
                //TODO: handle log file deserialization failures better (right now we automatically overwrite)
                currentLog = new Log(ethnoCode, storyTitle);
            }

        }

        currentLog.addAll(le);

        try {
            saveObject(currentLog, file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void saveObject(Object obj, String fileName) throws IOException {

        FileOutputStream fOut = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oOut = new ObjectOutputStream(fOut);
            try {
                oOut.writeObject(obj);
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

    public static void createFakeLogEntries(String lang, String story, int n){
        HashSet<LogEntry> james = new HashSet<>();
        Random rand = new Random();
        double start=0;
        ComChkEntry.Type[] cchkVals = ComChkEntry.Type.values();
        DraftEntry.Type[] dVals = DraftEntry.Type.values();
        for(int i=0; i<n; i++){


            LogEntry jim = new DraftEntry((long) (Math.random()*System.currentTimeMillis()),
                    dVals[rand.nextInt(dVals.length)], rand.nextInt(15));
            james.add(jim);
            jim = new LearnEntry((long) (Math.random()*System.currentTimeMillis()),
                    start = Math.random()*100 , start + Math.random()*100);
            james.add(jim);
            jim = new ComChkEntry((long) (Math.random()*System.currentTimeMillis()),
                    cchkVals[rand.nextInt(cchkVals.length)], rand.nextInt(15));
            james.add(jim);
        }
        System.out.println("cardinality of james: "+james.size());
        saveLogEntries(james, lang, story);
    }

}

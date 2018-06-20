package org.sil.storyproducer.tools.file;

import android.content.Context;

import org.sil.storyproducer.model.Workspace;
import org.sil.storyproducer.model.logging.ComChkEntry;
import org.sil.storyproducer.model.logging.DraftEntry;
import org.sil.storyproducer.model.logging.LearnEntry;
import org.sil.storyproducer.model.logging.Log;
import org.sil.storyproducer.model.logging.LogEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;

public class LogFiles {

    private static final String TAG = "LogFiles";

    private static String mLogsRootDir = null; //should be constant. initialized in init.
    private static Log currentLog = null;
    private static String SLASH = File.pathSeparator;

    public static void init(Context context) {
        mLogsRootDir = new File(context.getFilesDir(), "logs").getAbsolutePath()+SLASH;
        DraftEntry.init(context);
        ComChkEntry.init(context);
        LearnEntry.init(context);
    }

    public static void saveLogEntry(LogEntry le, String ethnoCode, String storyTitle){
        saveLogEntries(Collections.singleton(le), ethnoCode, storyTitle);
    }

    public static void saveLog(Log log){
        saveLogEntries(log, log.getLang(), log.getStory());
    }

    public static boolean deleteLog(String ethnoCode, String storyTitle){
        File file = new File(mLogsRootDir +ethnoCode+SLASH+storyTitle+SLASH+"log.ser");
        return file.delete();
    }

    /**
     * Returns the log for the story with this language and title, or null if there isn't one.
     * @param ethnoCode
     * @param storyTitle
     * @return the log for the story with this language and title, or null if there isn't one
     */
    public static Log getLog(String ethnoCode, String storyTitle){
        File file = new File(mLogsRootDir +ethnoCode+SLASH+storyTitle+SLASH+"log.ser");
        Log ret = null;
        if(file.exists()) {
            try {
                ret = loadLog(file.getAbsolutePath());
            } catch (ClassNotFoundException | IOException e) {
                android.util.Log.e(TAG, "Failed to load log!", e);
                ret = null;
            }
        }
        return ret;
    }

    public static void saveLogEntries(Collection <LogEntry> le){
        saveLogEntries(le, FileSystem.getLanguage(), Workspace.INSTANCE.getActiveStory().getTitle());
    }

    public static void saveLogEntry(LogEntry le){
        saveLogEntry(le, FileSystem.getLanguage(), Workspace.INSTANCE.getActiveStory().getTitle());
    }

    public static void saveLogEntries(Collection<LogEntry> le, String ethnoCode, String storyTitle){
        /*we need to make sure that currentLog contains a reference to valid log object, whether
        that means loading a log object by deserializing it, or creating a new log object
         */

        File file = new File(mLogsRootDir +ethnoCode+SLASH+storyTitle+SLASH+"log.ser");
        File dir = file.getParentFile();

        if(! dir.exists()){
            dir.mkdirs();
        }

        if(currentLog == null
                || (! currentLog.getLang().equals(ethnoCode) )
                || (! currentLog.getStory().equals(storyTitle) )){
            currentLog = getLog(ethnoCode, storyTitle);

            if(currentLog == null) { //no pre-existing log file, or old file failed to load
                //TODO: handle log file deserialization failures better (right now we automatically overwrite)
                currentLog = new Log(ethnoCode, storyTitle);
            }

        }

        currentLog.addAll(le);

        try {
            saveLog(currentLog, file.getAbsolutePath());
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to save log object!", e);
        }


    }

    private static void saveLog(Log log, String fileName) throws IOException {
        //TODO: consider using some kind of custom serialization

        FileOutputStream fOut = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oOut = new ObjectOutputStream(fOut);
            try {
                oOut.writeObject(log);
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

    private static Log loadLog(String fileName) throws IOException, ClassNotFoundException {
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
        return (Log) ret;
    }
}

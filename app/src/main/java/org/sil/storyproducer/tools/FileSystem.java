package org.sil.storyproducer.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class FileSystem {
    private static String language = "ENG"; //ethnologue code for english

    private static Context context;
    private static final String TEMPLATES_DIR = "templates",
                                NARRATION_PREFIX = "narration",
                                PROJECT_DIR = "projects",
                                SOUNDTRACK_PREFIX = "SoundTrack";


    //Paths to template directories from language and story name
    private static Map<String, Map<String, String>> storyPaths;
    private static Map<String, String> projectPaths;

    private static final FilenameFilter directoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };

    public static void init(Context con) {
        context = con;
        loadStories();
    }

    //Populate storyPaths from files in system
    public static void loadStories() {
        //Reset storyPaths
        storyPaths = new HashMap<>();
        projectPaths=new HashMap<>();

        File[] storeDirs = getStorageDirs();
        for(int storeIndex = 0; storeIndex < storeDirs.length; storeIndex++) {
            File sDir = storeDirs[storeIndex];

            if (sDir != null) {
                File templateDir = new File(sDir, TEMPLATES_DIR);

                if (templateDir.exists() && templateDir.isDirectory()) {
                    File[] langDirs = getLanguageDirs(templateDir);
                    for (int langIndex = 0; langIndex < langDirs.length; langIndex++) {
                        File lDir = langDirs[langIndex];
                        String lang = lDir.getName();

                        if (!storyPaths.containsKey(lang)) {
                            storyPaths.put(lang, new HashMap<String, String>());
                        }
                        Map<String, String> storyMap = storyPaths.get(lang);

                        File[] storyDirs = getStoryDirs(lDir);
                        for (int storyIndex = 0; storyIndex < storyDirs.length; storyIndex++) {
                            File storyDir = storyDirs[storyIndex];
                            String storyName = storyDir.getName();
                            String storyPath = storyDir.getPath();
                            storyMap.put(storyName, storyPath);
                        }
                    }
                }

                File projectDir  = new File(sDir, PROJECT_DIR);

                if (projectDir.exists() && projectDir.isDirectory()) {
                    File[] storyDirs = getStoryDirs(projectDir);
                    for (int storyIndex = 0; storyIndex < storyDirs.length; storyIndex++) {
                        File storyDir = storyDirs[storyIndex];
                        String storyName = storyDir.getName();
                        String storyPath = storyDir.getPath();
                        projectPaths.put(storyName, storyPath);
                    }
                }

            }
        }
    }

    public static void changeLanguage(String lang) {
        language = lang;
    }

    private static File[] getStorageDirs() {
        return ContextCompat.getExternalFilesDirs(context, null);
    }
    private static File[] getLanguageDirs(File storageDir) {
        return storageDir.listFiles(directoryFilter);
    }
    private static File[] getStoryDirs(File dir) {
        return dir.listFiles(directoryFilter);
    }

    private static String getStoryPath(String story){
        Map<String, String> storyMap = storyPaths.get(language);
        if(storyMap != null) {
            return storyMap.get(story);
        }
        return null;
    }

    public static File getNarrationAudio(String story, int i){
        return new File(getStoryPath(story)+"/"+NARRATION_PREFIX+i+".wav");
    }

    public static File getSoundtrack(String story){
        return new File(getStoryPath(story)+"/"+SOUNDTRACK_PREFIX+0+".mp3");
    }

    /**
     * Gets the directory of a particular story in the <b>projects</b> directory.
     * @param story
     * @return
     */
    public static File getProjectDirectory(String story) {
        String path = projectPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

    public static String[] getStoryNames() {
        Map<String, String> storyMap = storyPaths.get(language);
        if (storyMap != null) {
            Set<String> keys = storyMap.keySet();
            return keys.toArray(new String[keys.size()]);
        }
        return new String[0];
    }

    public static Bitmap getImage(String story, int number) {
        String path = getStoryPath(story);
        File f = new File(path);
        File file[] = f.listFiles();

        for (int i=0; i < file.length; i++) {
                if (file[i].getName().equals(number + ".jpg")) {
                    return BitmapFactory.decodeFile(path + "/" + file[i].getName());
                }
        }
        return null;
    }

    public static int getImageAmount(String storyName){
        String path = getStoryPath(storyName);
        File f = new File(path);
        File file[] = f.listFiles();
        int count = 0;
        for(int i=0; i<file.length; i++) {
            if (!file[i].isHidden() && file[i].getName().contains(".jpg")) {
                count++;
            }
        }
        return count;
    }

    private static String[] content;
    public static void loadSlideContent(String storyName, int slideNum){
        File file = new File(getStoryPath(storyName), (slideNum + ".txt"));
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }


        String text1 = text.toString();
        byte[] temp = text1.getBytes();
        for(int i = 0; i < temp.length - 2; i++) {
            //Swap out curly apostrophe with ASCII single quote
            if(temp[i] == -17 && temp[i+1] == -65 && temp[i+2] == -67) {
                text = text.replace(i, i+1, "'");
                text1 = text.toString();
                temp = text1.getBytes();
            }
        }
        content = text.toString().split(Pattern.quote("~"));
    }

    public static String getTitle(){
        return content[0];
    }
    public static String getSubTitle(){
        return content[1];
    }
    public static String getSlideVerse(){
        return content[2];
    }
    public static String getSlideContent(){
        return content[3];
    }

    public static String[] getLanguages() {
        return storyPaths.keySet().toArray(new String[storyPaths.size()]);
    }
}

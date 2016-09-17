package org.sil.storyproducer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by hannahbrown on 9/27/15.
 */
class FileSystem {
    private static String language = "English";

    private static Map<String, Map<String, String>> storyPaths = new HashMap<>();

    public static void init() {
        loadStories();
    }

    public static void loadStories() {
        File[] storeDirs = getStorageDirs();
        for(int storeIndex = 0; storeIndex < storeDirs.length; storeIndex++) {
            File sDir = storeDirs[storeIndex];
            File[] langDirs = getLanguageDirs(sDir);
            for(int langIndex = 0; langIndex < langDirs.length; langIndex++) {
                File lDir = langDirs[langIndex];
                String lang = lDir.getName();
                if(!storyPaths.containsKey(lang)) {
                    storyPaths.put(lang, new HashMap<String, String>());
                }
                Map<String, String> storyMap = storyPaths.get(lang);
                File[] storyDirs = getStoryDirs(lDir);
                for(int storyIndex = 0; storyIndex < storyDirs.length; storyIndex++) {
                    File storyDir = storyDirs[storyIndex];
                    String storyName = storyDir.getName();
                    String storyPath = storyDir.getPath();
                    storyMap.put(storyName, storyPath);
                }
            }
        }
    }

    public static void changeLanguage(String lang) {
        language = lang;
    }

//    public String[] getVideos() {
//        String path = getPath();
//        File f = new File(path);
//        File file[] = f.listFiles();
//        ArrayList<String> list = new ArrayList<>();
//        if(file != null) {
//            for (int i = 0; i < file.length; i++) {
//                if (!file[i].isHidden()) {
//                    list.add(file[i].getName());
//                }
//            }
//        }
//        String[] temp = new String[list.size()];
//        return list.toArray(temp);
//    }

    private static File[] getStorageDirs() {
        return Main.getAppContext().getExternalFilesDirs(null);
    }

    private static FilenameFilter directoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };

    private static File[] getLanguageDirs(File storageDir) {
        return storageDir.listFiles(directoryFilter);
    }

    private static File[] getStoryDirs(File langDir) {
        return langDir.listFiles(directoryFilter);
    }

    public static String getStoryPath(String story) {
        Map<String, String> storyMap = storyPaths.get(language);
        if(storyMap != null) {
            return storyMap.get(story);
        }
        return null;
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
    public static Bitmap getAudio(String story, int number) {
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
        for(int i = 0; i < temp.length; i++) {
            if(temp[i] == -17) {
                if(temp[i+1] == -65 && temp[i+2] == -67) {
                    text = text.replace(i, i+1, "'");
                    text1 = text.toString();
                    temp = text1.getBytes();
                }
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

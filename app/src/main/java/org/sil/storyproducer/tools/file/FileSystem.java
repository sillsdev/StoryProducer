package org.sil.storyproducer.tools.file;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.*;
import java.util.*;

/**
 * FileSystem serves as a base for the file package. The purpose of the package is to provide an
 * abstraction of file resources used by the application. Almost all construction of file paths
 * should be contained within this package. The API will consist primarily of static methods.
 *
 * This class in particular handles the details of the main file directories and also provides some
 * high-level information about stories. This class should primarily be used by classes at a higher
 * level than a project (for setup or macro-story information) or by other classes in this package.
 */
public class FileSystem {
    private static final String TAG = "FileSystem";

    private static String language = "ENG"; //ethnologue code for english

    private static final String TEMPLATES_DIR = "templates";
    private static final String PROJECT_DIR = "projects";

    //Paths to template directories from language and story name
    private static Map<String, Map<String, String>> templatePaths;
    private static Map<String, String> projectPaths;

    static final FilenameFilter directoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };

    //Populate templatePaths from files in system
    public static void init(Context con) {
        //Reset templatePaths
        templatePaths = new HashMap<>();
        projectPaths = new HashMap<>();

        //Iterate external files directories.
        File[] storeDirs = ContextCompat.getExternalFilesDirs(con, null);
        for (File currentStoreDir : storeDirs) {
            if (currentStoreDir != null) {
                //Get templates directory of current external storage directory.
                File templateDir = new File(currentStoreDir, TEMPLATES_DIR);

                //If there is no template directory (i.e. there are no templates on this storage
                // device), move on from this storage device.
                if (!templateDir.exists() || !templateDir.isDirectory()) {
                    continue;
                }

                File projectDir = new File(currentStoreDir, PROJECT_DIR);
                //Make the project directory if it does not exist.
                //The template creator shouldn't have to remember this step.
                if (!projectDir.isDirectory()) {
                    projectDir.mkdir();
                }

                File[] langDirs = templateDir.listFiles(directoryFilter);
                for (File currentLangDir : langDirs) {
                    String lang = currentLangDir.getName();

                    if (!templatePaths.containsKey(lang)) {
                        templatePaths.put(lang, new HashMap<String, String>());
                    }
                    Map<String, String> storyMap = templatePaths.get(lang);

                    File[] storyDirs = currentLangDir.listFiles(directoryFilter);
                    for (File currentStoryDir : storyDirs) {
                        String storyName = currentStoryDir.getName();
                        String storyPath = currentStoryDir.getPath();
                        storyMap.put(storyName, storyPath);

                        //Make sure the corresponding projects directory exists.
                        File storyWriteDir = new File(new File(currentStoreDir, PROJECT_DIR), storyName);
                        if (!storyWriteDir.isDirectory()) {
                            storyWriteDir.mkdir();
                        }
                    }
                }

                //Iterate story directories and populate projectPaths.
                File[] storyDirs = projectDir.listFiles(directoryFilter);
                for (File storyDir : storyDirs) {
                    String storyName = storyDir.getName();
                    String storyPath = storyDir.getPath();
                    projectPaths.put(storyName, storyPath);
                }
            }
        }
    }

    /**
     * Change the language templates are drawn from.
     * @param lang ethnologue code for new language
     */
    public static void changeLanguage(String lang) {
        if(templatePaths.containsKey(lang)) {
            language = lang;
        }
        else {
            Log.w(TAG, "No templates available for language " + lang + ". Retaining language " + language + ".");
        }
    }

    /**
     * Gets the directory of a particular story in the <b>templates</b> directory (i.e. LWC-specific
     * read-only files).
     * Note: package-local access intended
     * @param story
     * @return
     */
    static String getTemplatePath(String story){
        Map<String, String> storyMap = templatePaths.get(language);
        if (storyMap != null) {
            return storyMap.get(story);
        }
        return null;
    }

    /**
     * Gets the directory of a particular story in the <b>projects</b> directory (i.e. writable files).
     * Note: package-local access intended
     * @param story
     * @return
     */
    static File getProjectDirectory(String story) {
        String path = projectPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

    /**
     * Get a list of available stories in the current language.
     * @return
     */
    public static String[] getStoryNames() {
        Map<String, String> storyMap = templatePaths.get(language);
        if (storyMap != null) {
            Set<String> keys = storyMap.keySet();
            return keys.toArray(new String[keys.size()]);
        }
        return new String[0];
    }

    /**
     * Get a list of available languages (in ethnologue code format) with templates installed.
     * @return
     */
    public static String[] getLanguages() {
        return templatePaths.keySet().toArray(new String[templatePaths.size()]);
    }

    /**
     * This function searches the directory of the story and finds the total number of
     * slides associated with the story. The total number of slides will be determined by
     * the number of .jpg and .txt extensions. The smaller number of .jpg or .txt will be returned.
     *
     * @param storyName The story name that needs to find total number of slides.
     * @return The number of slides total for the story. The smaller number of .txt or .jpg files
     * found in the directory.
     */
    public static int getTotalSlideNum(String storyName) {
        String rootDirectory = getTemplatePath(storyName);
        File[] files = new File(rootDirectory).listFiles();
        int totalPics = 0;
        int totalTexts = 0;

        for (File aFile : files) {
            String tempNumber;
            String fileName = aFile.toString();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".txt")) {
                String extension = (fileName.endsWith(".jpg")) ? ".jpg" : ".txt";
                tempNumber = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf(extension));
                if (tempNumber.matches("^([0-9]+)$")) {
                    int checkingNumber = Integer.valueOf(tempNumber);
                    if (extension.equals(".txt")) {
                        totalTexts = (checkingNumber > totalTexts) ? checkingNumber : totalTexts;
                    } else {
                        totalPics = (checkingNumber > totalPics) ? checkingNumber : totalPics;
                    }
                }
            }
        }

        return (totalPics < totalTexts) ? totalPics : totalTexts;
    }
}

package org.sil.storyproducer.tools.file;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
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

    private static String language;

    private static final String HIDDEN_TEMP_DIR = ".temp";
    private static final String TEMPLATES_DIR = "templates";
    private static final String PROJECT_DIR = "projects";
    private static final String LANGUAGE_PREFS = "languages";
    private static final String LWC_LANGUAGE = "lwc language";

    //Paths to template directories from language and story name
    private static Map<String, Map<String, String>> templatePaths;
    private static Map<String, String> projectPaths;
    private static Map<String, String> moviesPaths;

    private static File cacheDir;

    static final FilenameFilter directoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };

    //Populate templatePaths from files in system
    public static void init(Context con) {
        cacheDir = con.getCacheDir();

        //Reset templatePaths
        templatePaths = new HashMap<>();
        projectPaths = new HashMap<>();
        moviesPaths = new HashMap<>();

        // Get the LWC language from preferences (defaults to ENG if none set)
        SharedPreferences prefs = con.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE);
        language = prefs.getString(LWC_LANGUAGE, "ENG");

        //Iterate external files directories.
        File[] storeDirs = ContextCompat.getExternalFilesDirs(con, null);
        File[] moviesDirs = ContextCompat.getExternalFilesDirs(con, Environment.DIRECTORY_MOVIES);
        for (int i = 0; i < storeDirs.length; i++) {
            File currentStoreDir = storeDirs[i];
            File currentMoviesDir = moviesDirs[i];
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
                    Map<String, String> storyTemplateMap = templatePaths.get(lang);

                    File[] storyDirs = currentLangDir.listFiles(directoryFilter);
                    for (File currentStoryDir : storyDirs) {
                        String storyName = currentStoryDir.getName();
                        String storyTemplatePath = currentStoryDir.getAbsolutePath();
                        storyTemplateMap.put(storyName, storyTemplatePath);

                        //Make sure the corresponding projects directory exists.
                        File storyWriteDir = new File(new File(currentStoreDir, PROJECT_DIR), storyName);
                        if (!storyWriteDir.isDirectory()) {
                            storyWriteDir.mkdir();
                        }
                        projectPaths.put(storyName, storyWriteDir.getAbsolutePath());
                        moviesPaths.put(storyName, currentMoviesDir.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static String getLanguage(){
        return language;
    }

    /**
     * Gets the <b>templates</b> directory (i.e. LWC-specific read-only files) of a particular story.
     * Note: This method should be used sparingly, primarily by classes within this package.
     * @param story
     * @return
     */
    public static String getTemplatePath(String story){
        Map<String, String> storyMap = templatePaths.get(language);
        if (storyMap != null) {
            return storyMap.get(story);
        }
        return null;
    }

    /**
     * Gets the <b>projects</b> directory (i.e. writable files) of a particular story.
     * Note: package-local access intended
     * @param story
     * @return
     */
    static File getProjectDirectory(String story) {
        String path = projectPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

}
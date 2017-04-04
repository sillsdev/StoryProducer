package org.sil.storyproducer.tools.file;

import android.content.Context;
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

    private static String language = "ENG"; //ethnologue code for english

    private static final String HIDDEN_TEMP_DIR = ".temp";
    private static final String TEMPLATES_DIR = "templates";
    private static final String PROJECT_DIR = "projects";

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

    /**
     * Change the language templates are drawn from.
     * @param lang ethnologue code for new language
     * @return true if change applied, false if error
     */
    public static boolean changeLanguage(String lang) {
        if(templatePaths.containsKey(lang)) {
            language = lang;
            return true;
        }
        else {
            Log.w(TAG, "No templates available for language " + lang + ". Retaining language " + language + ".");
            return false;
        }
    }

    /**
     * Gets the <b>templates</b> directory (i.e. LWC-specific read-only files) of a particular story.
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
     * Gets the <b>projects</b> directory (i.e. writable files) of a particular story.
     * Note: package-local access intended
     * @param story
     * @return
     */
    static File getProjectDirectory(String story) {
        String path = projectPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

    /**
     * Gets the <b>movies</b> directory (i.e. output videos) of a particular story.
     * Note: package-local access intended
     * @param story
     * @return
     */
    static File getMoviesDirectory(String story) {
        String path = moviesPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

    /**
     * Gets a special hidden directory for temporary files within the <b>projects</b> directory
     * of a particular story.
     * Note: package-local access intended
     * @param story
     * @return
     */
    static File getHiddenTempDirectory(String story) {
        String projectPath = projectPaths.get(story);
        File hiddenTempDir = new File(projectPath, HIDDEN_TEMP_DIR);
        if(!hiddenTempDir.exists()) {
            hiddenTempDir.mkdir();
        }
        return hiddenTempDir;
    }

    /**
     * Gets the directory of temporary files. Use this for small temporary files.
     * Note: package-local access intended
     * @return
     */
    static File getCacheDirectory() {
        return cacheDir;
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
     * the number of slide pictures and text files. The number of slides is assumed to be the smaller of the two.
     *
     * @param story The story name that needs to find total number of slides.
     * @return The number of slides total for the story. The smaller number of image or text files
     * found in the directory.
     */
    public static int getContentSlideAmount(String story) {
        int totalPics = ImageFiles.getNumberedAmount(story);
        int totalTexts = TextFiles.getNumberedAmount(story);

        return (totalPics < totalTexts) ? totalPics : totalTexts;
    }

    /**
     * Search a directory for files of the matching (prefix)[0-9]+(extension) and count them.
     * In practice, the number returned may not actually be the count of files but how many files
     * are suggested by an unbroken sequence of file names (with sequentially numbered names). If
     * there seem to be files missing from the sequence, an exception will be logged.
     *
     * @param directory to search.
     * @param prefix common start of file names.
     * @param extension file extension (including '.'), though could be any postfix to number.
     * @return count of numbered files matching format as suggested by highest number in sequence.
     */
    static int getNumberedFilesAmount(File directory, String prefix, String extension) {
        if(directory == null) {
            return 0;
        }

        List<Integer> accountedNumbers = new ArrayList<>();

        //Take care of null inputs.
        String sanPrefix = prefix != null ? prefix : "";
        String sanExtension = extension != null ? extension : "";

        File[] files = directory.listFiles();
        int totalFiles = 0;
        int highestNumber = -1;

        //Iterate files in directory.
        for (File currentFile : files) {
            String fileName = currentFile.getName();
            if (fileName.startsWith(sanPrefix) && fileName.endsWith(sanExtension)) {
                //Get part of file name expected to be number.
                String tempNumber = fileName.substring(sanPrefix.length(), fileName.lastIndexOf(sanExtension));
                //Make sure expected number part is a number.
                if (tempNumber.matches("^([0-9]+)$")) {
                    int checkingNumber = Integer.valueOf(tempNumber);
                    accountedNumbers.add(checkingNumber);
                    totalFiles++;
                    if(checkingNumber > highestNumber) {
                        highestNumber = checkingNumber;
                    }
                }
            }
        }

        //If the highest numbered file doesn't match the total count, we are missing files.
        if(highestNumber + 1 > totalFiles) {
            Collections.sort(accountedNumbers);

            //Tally up missing numbers.
            StringBuilder unaccountedNumbers = new StringBuilder();
            int previousNum = -1;
            String delim = "";
            for(int num : accountedNumbers) {
                if(previousNum + 1 != num) {
                    for(int missingNum = previousNum + 1; missingNum < num; missingNum++) {
                        unaccountedNumbers.append(delim);
                        unaccountedNumbers.append(sanPrefix + missingNum + sanExtension);
                        //After first time, comma is used as delimiter.
                        delim = ", ";
                    }
                }
                previousNum = num;
            }

            //TODO: Throw this exception?
            FileMissingException e = new FileMissingException("Missing these files from directory ("
                    + directory + "): " + unaccountedNumbers);
            Log.e(TAG, "Files missing!", e);
        }

        //Go with highest number rather than count.
        return highestNumber + 1;
    }
}

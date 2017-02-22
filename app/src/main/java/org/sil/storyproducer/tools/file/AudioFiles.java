package org.sil.storyproducer.tools.file;

import java.io.File;
import java.util.ArrayList;

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */
public class AudioFiles {
    private static final String SOUNDTRACK_PREFIX = "SoundTrack";
    private static final String LEARN_PRACTICE_PREFIX = "learnPractice";
    private static final String LWC_AUDIO_PREFIX = "narration";
    private static final String TRANSLATION_AUDIO_PREFIX = "translation";
    private static final String MP3_EXTENSION = ".mp3";
    private static final String COMMENT_PREFIX = "comment";

    public static File getLWC(String story, int i){
        final String LWC_EXTENSION = ".wav";
        return new File(FileSystem.getTemplatePath(story), LWC_AUDIO_PREFIX + i + LWC_EXTENSION);
    }

    public static File getDraft(String story, int i){
        return new File(FileSystem.getProjectDirectory(story), TRANSLATION_AUDIO_PREFIX + i + MP3_EXTENSION);
    }

    public static File getSoundtrack(String story){
        return getSoundtrack(story, 0);
    }
    //TODO: Some stories have multiple soundtrack files. Is that desired and used?
    public static File getSoundtrack(String story, int i){
        return new File(FileSystem.getTemplatePath(story), SOUNDTRACK_PREFIX + i + MP3_EXTENSION);
    }

    /**
     * Gets the File for the learn practice recording
     * @param story
     * @return
     */
    public static File getLearnPracticeAudio(String story){
        return new File(FileSystem.getProjectDirectory(story), LEARN_PRACTICE_PREFIX + MP3_EXTENSION);
    }

    public static File getAudioComment(String story, int slide, String commentTitle) {
        return new File(FileSystem.getProjectDirectory(story)+"/"+COMMENT_PREFIX+slide+"_"+ commentTitle + MP3_EXTENSION);
    }

    /**
     * deletes the designated audio comment
     * @param story the story the comment comes from
     * @param slide the slide the comment comes from
     * @param commentTitle the name of the comment in question
     */
    public static void deleteAudioComment(String story, int slide, String commentTitle) {
        File file = getAudioComment(story, slide, commentTitle);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean doesFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * renames the designated audio comment if the new name is valid and the file exists
     * @param story the story the comment comes from
     * @param slide the slide of the story the comment comes from
     * @param oldTitle the old title of the comment
     * @param newTitle the proposed new title for the comment
     * @return returns success or error code of renaming
     */
    public static RENAME_CODES renameAudioComment(String story, int slide, String oldTitle, String newTitle) {
        // Requirements for file names:
        //        - must be under 20 characters
        //        - must be only contain alphanumeric characters or spaces/underscores
        //        - must not contain the comment designator such as "comment0"
        if (newTitle.length() > 20) {
            return RENAME_CODES.ERROR_LENGTH;
        }
        if (!newTitle.matches("[A-Za-z0-9\\s_]+")) {
            return RENAME_CODES.ERROR_SPECIAL_CHARS;
        }
        if (newTitle.matches("comment[0-9]+")) {
            return RENAME_CODES.ERROR_CONTAINED_DESIGNATOR;
        }
        File file = getAudioComment(story, slide, oldTitle);
        boolean renamed = false;
        if (file.exists()) {
            String newPathName = file.getAbsolutePath().replace(oldTitle + MP3_EXTENSION, newTitle + MP3_EXTENSION);
            File newFile = new File(newPathName);
            if (!newFile.exists()) {
                renamed = file.renameTo(newFile);
            }
        }
        if (renamed) {
            return RENAME_CODES.SUCCESS;
        } else {
            return RENAME_CODES.ERROR_UNDEFINED;
        }
    }

    /**
     * Returns a list of comment titles for the story and slide in question
     * @param story the story where the comments come from
     * @param slide the slide where the comments come from
     * @return the array of comment titles
     */
    public static String[] getCommentTitles(String story, int slide) {
        ArrayList<String> commentTitles = new ArrayList<String>();
        File storyDirectory = FileSystem.getProjectDirectory(story);
        File[] storyDirectoryFiles = storyDirectory.listFiles();
        String filename;
        for (int i = 0; i < storyDirectoryFiles.length; i++) {
            filename = storyDirectoryFiles[i].getName();
            if (filename.contains(COMMENT_PREFIX+slide)) {
                filename = filename.replace(COMMENT_PREFIX+slide+"_", "");
                filename = filename.replace(MP3_EXTENSION, "");
                commentTitles.add(filename);
            }
        }
        String[] returnTitlesArray = new String[commentTitles.size()];
        return commentTitles.toArray(returnTitlesArray);
    }

    public enum RENAME_CODES {
        SUCCESS,
        ERROR_LENGTH,
        ERROR_SPECIAL_CHARS,
        ERROR_CONTAINED_DESIGNATOR,
        ERROR_UNDEFINED
    }

    //TODO: Re-implement or discard this method.
//    public static String getAudioPath(String story, int number) {
//        String path = FileSystem.getTemplatePath(story);
//        File f = new File(path);
//        File file[] = f.listFiles();
//        String audioName = "narration" + number;
//
//        for (int i = 0; i < file.length; i++) {
//            String[] audioExtensions = {".wav", ".mp3", ".wma"};
//            for (String extension : audioExtensions) {
//                if (file[i].getName().equals(audioName + extension)) {
//                    return file[i].getAbsolutePath();
//                }
//            }
//        }
//        return null;
//    }
}

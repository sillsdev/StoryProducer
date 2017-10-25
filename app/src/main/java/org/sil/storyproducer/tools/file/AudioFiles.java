package org.sil.storyproducer.tools.file;

import org.sil.storyproducer.model.Template;
import org.sil.storyproducer.model.TemplateSlide;
import org.sil.storyproducer.tools.StorySharedPreferences;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */
public class AudioFiles {
    private static final String SOUNDTRACK_PREFIX = "SoundTrack";

    private static final String SOUND_EXTENSION = ".m4a";

    private static final String LEARN_PRACTICE_PREFIX = "learnPractice";
    private static final String LWC_AUDIO_PREFIX = "narration";
    private static final String DRAFT_TEMP = "draftTemp" + SOUND_EXTENSION;
    private static final String DRAFT_AUDIO_PREFIX = "translation";
    private static final String COMMENT_PREFIX = "comment";
    private static final String DRAMATIZATION_AUDIO_PREFIX = "dramatization";

    private enum ModalType {
        DRAFT, COMMUNITY, DRAMATIZATION
    }

    public enum RenameCode {
        SUCCESS,
        ERROR_LENGTH,
        ERROR_SPECIAL_CHARS,
        ERROR_UNDEFINED,
        ;
    }

    public static File getSoundtrack(String story){
        return new File(FileSystem.getTemplatePath(story), SOUNDTRACK_PREFIX + 0 + SOUND_EXTENSION);
    }

    public static File getSoundtrack(String story, int i){
        TemplateSlide slide = Template.getSlide(story, i);
        if(slide != null) {
            return slide.getSoundtrack();
        }
        else {
            return getSoundtrack(story);
        }
    }

    //*** LWC ***

    public static File getLWC(String story, int i){
        return new File(FileSystem.getTemplatePath(story), LWC_AUDIO_PREFIX + i + SOUND_EXTENSION);
    }

    //*** Learn ***

    /**
     * Gets the File for the learn practice recording
     * @param story
     * @return
     */
    public static File getLearnPractice(String story){
        return new File(FileSystem.getProjectDirectory(story), LEARN_PRACTICE_PREFIX + SOUND_EXTENSION);
    }

    //*** Draft ***

    public static File getDraft(String story, int slide) {
        return getDraft(story, slide, StorySharedPreferences.getDraftForSlideAndStory(slide, story));
    }

    public static File getDraft(String story, int slide, String draftTitle) {
        return new File(FileSystem.getProjectDirectory(story), DRAFT_AUDIO_PREFIX + slide + "_" + draftTitle + SOUND_EXTENSION);
    }

    public static String getDraftTitle(File file) {
        String filename = file.getName();
        return getTitleFromPath(filename, DRAFT_AUDIO_PREFIX, SOUND_EXTENSION);
    }

    /**
     * deletes the designated audio draft
     * @param story the story the draft comes from
     * @param slide the slide the draft comes from
     * @param draftTitle the name of the draft in question
     */
    public static void deleteDraft(String story, int slide, String draftTitle) {
        File file = getDraft(story, slide, draftTitle);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * renames the designated audio draft if the new name is valid and the file exists
     * @param story the story the draft comes from
     * @param slide the slide of the story the draft comes from
     * @param oldTitle the old title of the draft
     * @param newTitle the proposed new title for the draft
     * @return returns success or error code of renaming
     */
    public static RenameCode renameDraft(String story, int slide, String oldTitle, String newTitle) {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.DRAFT, SOUND_EXTENSION);
    }

    /**
     * Returns a list of draft titles for the story and slide in question
     * @param story the story where the drafts come from
     * @param slide the slide where the drafts come from
     * @return the array of draft titles
     */
    public static String[] getDraftTitles(String story, int slide) {
        return getRecordingTitlesHelper(story, slide, DRAFT_AUDIO_PREFIX, SOUND_EXTENSION);
    }

    //*** Community Check ***

    public static File getComment(String story, int slide, String commentTitle) {
        return new File(FileSystem.getProjectDirectory(story), COMMENT_PREFIX+slide + "_" + commentTitle + SOUND_EXTENSION);
    }

    /**
     * deletes the designated audio comment
     * @param story the story the comment comes from
     * @param slide the slide the comment comes from
     * @param commentTitle the name of the comment in question
     */
    public static void deleteComment(String story, int slide, String commentTitle) {
        File file = getComment(story, slide, commentTitle);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * renames the designated audio comment if the new name is valid and the file exists
     * @param story the story the comment comes from
     * @param slide the slide of the story the comment comes from
     * @param oldTitle the old title of the comment
     * @param newTitle the proposed new title for the comment
     * @return returns success or error code of renaming
     */
    public static RenameCode renameComment(String story, int slide, String oldTitle, String newTitle) {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.COMMUNITY, SOUND_EXTENSION);
    }

    /**
     * Returns a list of comment titles for the story and slide in question
     * @param story the story where the comments come from
     * @param slide the slide where the comments come from
     * @return the array of comment titles
     */
    public static String[] getCommentTitles(String story, int slide) {
        return getRecordingTitlesHelper(story, slide, COMMENT_PREFIX, SOUND_EXTENSION);
    }

    //*** Consultant Check ***

    //*** Dramatization ***

    public static File getDramatization(String story, int slide){
        return getDramatization(story, slide, StorySharedPreferences.getDramatizationForSlideAndStory(slide, story));
    }

    public static File getDramatization(String story, int slide, String dramaTitle) {
        return new File(FileSystem.getProjectDirectory(story), DRAMATIZATION_AUDIO_PREFIX + slide + "_" + dramaTitle + SOUND_EXTENSION);
    }

    public static File getDramatizationTemp(String story){
        return new File(FileSystem.getHiddenTempDirectory(story), DRAMATIZATION_AUDIO_PREFIX + "_" + "T"  + SOUND_EXTENSION);
    }

    public static String getDramatizationTitle(File file) {
        String filename = file.getName();
        return getTitleFromPath(filename, DRAMATIZATION_AUDIO_PREFIX, SOUND_EXTENSION);
    }

    /**
     * deletes the designated audio dramatization
     * @param story the story the dramatization comes from
     * @param slide the slide the dramatization comes from
     * @param draftTitle the name of the dramatization in question
     */
    public static void deleteDramatization(String story, int slide, String draftTitle) {
        File file = getDramatization(story, slide, draftTitle);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * renames the designated audio dramatization if the new name is valid and the file exists
     * @param story the story the dramatization comes from
     * @param slide the slide of the story the dramatization comes from
     * @param oldTitle the old title of the dramatization
     * @param newTitle the proposed new title for the dramatization
     * @return returns success or error code of renaming
     */
    public static RenameCode renameDramatization(String story, int slide, String oldTitle, String newTitle) {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.DRAMATIZATION, SOUND_EXTENSION);
    }

    /**
     * Returns a list of dramatization titles for the story and slide in question
     * @param story the story where the dramatization come from
     * @param slide the slide where the dramatization come from
     * @return the array of dramatization titles
     */
    public static String[] getDramatizationTitles(String story, int slide) {
        return getRecordingTitlesHelper(story, slide, DRAMATIZATION_AUDIO_PREFIX, SOUND_EXTENSION);
    }

    //**** Helpers ***//
    private static RenameCode renameAudioFileHelper(String story, int slide, String oldTitle, String newTitle, ModalType type, String extension) {
        // Requirements for file names:
        //        - must be under 20 characters
        //        - must be only contain alphanumeric characters or spaces/underscores
        if (newTitle.length() > 20) {
            return RenameCode.ERROR_LENGTH;
        }
        if (!newTitle.matches("[A-Za-z0-9\\s_]+")) {
            return RenameCode.ERROR_SPECIAL_CHARS;
        }
        File file = getComment(story, slide, oldTitle);
        switch(type) {                  //set the file based on the different file types
            case DRAFT:
                file = getDraft(story, slide, oldTitle);
                break;
            case COMMUNITY:
                file = getComment(story, slide, oldTitle);
                break;
            case DRAMATIZATION:
                file = getDramatization(story, slide, oldTitle);
                break;
        }

        boolean renamed = false;
        if (file.exists()) {
            String newPathName = file.getAbsolutePath().replace(oldTitle + extension, newTitle + extension);
            File newFile = new File(newPathName);
            if (!newFile.exists()) {
                renamed = file.renameTo(newFile);
            }
        }
        if (renamed) {
            return RenameCode.SUCCESS;
        } else {
            return RenameCode.ERROR_UNDEFINED;
        }
    }

    private static String[] getRecordingTitlesHelper(String story, int slide, String prefix, String extension) {
        List<String> titles = new ArrayList<>();
        File storyDirectory = FileSystem.getProjectDirectory(story);
        File[] storyDirectoryFiles = storyDirectory.listFiles();
        for (int i = 0; i < storyDirectoryFiles.length; i++) {
            String filename = storyDirectoryFiles[i].getName();
            if (filename.startsWith(prefix+slide+"_") && filename.endsWith(extension)) {
                titles.add(getTitleFromPath(filename, prefix, extension));
            }
        }
        String[] returnTitlesArray = new String[titles.size()];
        return titles.toArray(returnTitlesArray);
    }

    /**
     * Extract title from path.
     */
    private static String getTitleFromPath(String filename, String prefix, String extension) {
        //Note: Assume no dots in filename.
        return filename.replaceFirst(prefix + "\\d+" + "_", "").replace(extension, "");
    }

}

package org.sil.storyproducer.tools;

import android.content.Context;
import android.preference.PreferenceManager;

import org.sil.storyproducer.model.StoryState;

public class StorySharedPreferences {

    private static final String PHASE_KEY = "phase";
    private static final String DRAFT_FILE_KEY = "draft-file-key";
    private static final String DRAMA_FILE_KEY = "drama-file-key";

    private static Context context;

    public static void init(Context con) {
        context = con;
    }

    public static void setPhaseForStory(final String phase, final String storyName) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(storyName + PHASE_KEY, phase)
                .commit();
    }

    public static String getPhaseForStory(String storyName) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(storyName + PHASE_KEY, StoryState.LEARN_PHASE);    //learn is the default phase
    }

    public static void setDraftForSlideAndStory(String draftFileName, int slide, String storyName) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(DRAFT_FILE_KEY + slide + storyName, draftFileName)
                .commit();
    }

    public static String getDraftForSlideAndStory(int slide, String storyName) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(DRAFT_FILE_KEY + slide + storyName, "");
    }

    public static void setDramatizationForSlideAndStory(String draftFileName, int slide, String storyName) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(DRAMA_FILE_KEY + slide + storyName, draftFileName)
                .commit();
    }

    public static String getDramatizationForSlideAndStory(int slide, String storyName) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(DRAMA_FILE_KEY + slide + storyName, "");
    }

}

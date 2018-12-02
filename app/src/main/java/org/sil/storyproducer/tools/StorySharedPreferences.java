package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag;


public class StorySharedPreferences {

    private static final String PHASE_KEY = "phase";
    private static final String DRAFT_FILE_KEY = "draft-file-key";
    private static final String DRAMA_FILE_KEY = "drama-file-key";
    private static final String BACKT_FILE_KEY = "backt-file-key";
    private static final String EXPORTED_VIDEOS_KEY = "exported_videos";

    private static SharedPreferences prefs;

    public static void init(Context con) {
        prefs = PreferenceManager.getDefaultSharedPreferences(con);
    }

    public static boolean isApproved(String storyName, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ConsultantCheckFrag.CONSULTANT_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(storyName + ConsultantCheckFrag.IS_CONSULTANT_APPROVED, false);
    }



    public static String getDraftForSlideAndStory(int slide, String storyName) {
        return prefs.getString(DRAFT_FILE_KEY + slide + storyName, "");
    }


    public static String getDramatizationForSlideAndStory(int slide, String storyName) {
        return prefs.getString(DRAMA_FILE_KEY + slide + storyName, "");
    }

    //new for backtranslations

    public static void setBackTranslationForSlideAndStory(String draftFileName, int slide, String storyName){
        prefs.edit().putString(BACKT_FILE_KEY + slide + storyName, draftFileName).apply();
    }

    public static String getBackTranslationForSlideAndStory(int slide, String storyName) {
        return prefs.getString(BACKT_FILE_KEY + slide + storyName, "");
    }
}

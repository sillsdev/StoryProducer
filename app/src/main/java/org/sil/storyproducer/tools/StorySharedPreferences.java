package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.sil.storyproducer.controller.consultant.ConsultantCheckFrag;
import org.sil.storyproducer.model.StoryState;

import java.util.ArrayList;
import java.util.List;

public class StorySharedPreferences {

    private static final String PHASE_KEY = "phase";
    private static final String EXPORTED_VIDEOS_KEY = "exported_videos";

    private static SharedPreferences prefs;

    public static void init(Context con) {
        prefs = PreferenceManager.getDefaultSharedPreferences(con);
    }

    public static void setPhaseForStory(final String phase, final String storyName) {
        prefs.edit()
                .putString(storyName + PHASE_KEY, phase)
                .commit();
    }

    public static String getPhaseForStory(String storyName) {
        return prefs.getString(storyName + PHASE_KEY, StoryState.LEARN_PHASE);    //learn is the default phase
    }

    public static boolean isApproved(String storyName, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ConsultantCheckFrag.CONSULTANT_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(storyName + ConsultantCheckFrag.IS_CONSULTANT_APPROVED, false);
    }

    public static void addExportedVideoForStory(String videoPath, String storyName) {
        List<String> paths = getStringArrayPref(storyName + EXPORTED_VIDEOS_KEY);
        paths.add(videoPath);
        setStringArrayPref(storyName + EXPORTED_VIDEOS_KEY, paths);
    }

    public static List<String> getExportedVideosForStory(String storyName) {
        return getStringArrayPref(storyName + EXPORTED_VIDEOS_KEY);
    }

    //helper functions for saving the string arrays
    private static void setStringArrayPref(String key, List<String> values) {
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray a = new JSONArray();
        for (int i = 0; i < values.size(); i++) {
            a.put(values.get(i));
        }
        if (!values.isEmpty()) {
            editor.putString(key, a.toString());
        } else {
            editor.putString(key, null);
        }
        editor.commit();
    }

    private static List<String> getStringArrayPref(String key) {
        String json = prefs.getString(key, null);
        List<String> values = new ArrayList<>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String str = a.optString(i);
                    values.add(str);
                }
            } catch (JSONException e) {
                Log.e("StorySharedPrefs", "something broke in getStringArrayPref", e);
            }
        }
        return values;
    }
}

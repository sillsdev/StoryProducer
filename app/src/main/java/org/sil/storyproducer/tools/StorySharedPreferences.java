package org.sil.storyproducer.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.sil.storyproducer.controller.MainActivity;
import org.sil.storyproducer.model.StoryState;

import java.util.ArrayList;

public class StorySharedPreferences {

    private static final String PHASE_KEY = "phase";
    private static final String EXPORTED_VIDEOS_KEY = "exported_videos";

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

    public static void setExportedVideoForStory(String videoPath, String storyName) {
        ArrayList<String> paths = getStringArrayPref(storyName + EXPORTED_VIDEOS_KEY);
        paths.add(videoPath);
        setStringArrayPref(storyName + EXPORTED_VIDEOS_KEY, paths);
    }

    public static ArrayList<String> getExportedVideosForStory(String storyName) {
        return getStringArrayPref(storyName + EXPORTED_VIDEOS_KEY);
    }

    //helper functions for saving the string arrays
    private static void setStringArrayPref(String key, ArrayList<String> values) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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

    private static ArrayList<String> getStringArrayPref(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(key, null);
        ArrayList<String> values = new ArrayList<String>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String str = a.optString(i);
                    values.add(str);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return values;
    }
}

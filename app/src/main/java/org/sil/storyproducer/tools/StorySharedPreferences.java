package org.sil.storyproducer.tools;

import android.content.Context;
import android.preference.PreferenceManager;

import org.sil.storyproducer.model.StoryState;

public class StorySharedPreferences {

    private static final String PHASE_KEY = "phase";

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
}

package org.sil.storyproducer.model;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.export.ExportActivity;
import org.sil.storyproducer.controller.learn.LearnActivity;
import org.sil.storyproducer.controller.pager.PagerBaseActivity;
import android.content.Context;

/**
 * StoryState is a static class that holds the information of the state of a story project
 */
public final class StoryState {

    private static Context context;
    private static String storyName;
    private static Phase currentPhase;
    private static Phase[] phases;
    private static int currentPhaseIndex = 0;


    //TODO: add saving state in prefrences for each story

    /**
     * initializes the StoryState variables
     * @param con the application context so that the colors can be grabbed from the resources
     */
    public static void init(Context con) {
        context = con;
        currentPhase = new Phase(context.getResources().getString(R.string.learnTitle), R.color.learn_phase, LearnActivity.class);
        String[] phaseMenuArray = con.getResources().getStringArray(R.array.phases_menu_array);
        phases =  new Phase[] {new Phase(phaseMenuArray[0], R.color.learn_phase,LearnActivity.class),
                            new Phase(phaseMenuArray[1], R.color.draft_phase, PagerBaseActivity.class),
                            new Phase(phaseMenuArray[2], R.color.comunity_check_phase, PagerBaseActivity.class),
                            new Phase(phaseMenuArray[3], R.color.consultant_check_phase, PagerBaseActivity.class),
                            new Phase(phaseMenuArray[4], R.color.dramatization_phase, PagerBaseActivity.class),
                            new Phase(phaseMenuArray[5], R.color.export_phase, ExportActivity.class)};
    }

    /**
     * Returns the story's name
     * @return String for storyName
     */
    public static String getStoryName() {
        return storyName;
    }

    /**
     * Sets the storyName
     * @param name String to set storyName with
     */
    public static void setStoryName(String name) {
        storyName = name;
    }

    /**
     * Return the current Phase the story is in
     * @return Phase
     */
    public static Phase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * sets the current phase as well as the phaseIndex
     * @param p Phase
     */
    public static void setCurrentPhase(Phase p) {
        for(int k = 0; k < phases.length; k++) {
            if(p.getTitle().equals(phases[k].getTitle())) {
                currentPhaseIndex = k;
            }
        }
        currentPhase = p;
    }

    /**
     * returns the index for the current story phase
     * @return int
     */
    public static int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public static Phase[] getPhases() {
        return phases;
    }

    /**
     * gets the next phase and sets the current phase to that phase
     * @return Phase returns the next phase
     */
    public static Phase advanceNextPhase() {
        if(currentPhaseIndex < phases.length - 1) {
            currentPhaseIndex++;
        }
        currentPhase = phases[currentPhaseIndex];
        return currentPhase;
    }

    /**
     * gets the previous Phase and sets the current phase to that phase
     * @return Phase returns the previous phase
     */
    public static Phase advancePrevPhase() {
        if(currentPhaseIndex > 0) {
            currentPhaseIndex--;
        }
        currentPhase = phases[currentPhaseIndex];
        return currentPhase;
    }


}

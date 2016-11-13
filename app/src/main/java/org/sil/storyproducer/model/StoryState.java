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
    private static Phase phase;
    private static Phase[] phases;
    private static int phaseIndex = 0;

    /**
     * initializes the StoryState variables
     * @param con : the application context so that the colors can be grabbed from the resources
     */
    public static void init(Context con) {
        context = con;
        phase = new Phase(context.getResources().getString(R.string.learnTitle), R.color.learnPhase, LearnActivity.class);
        phases =  new Phase[] {new Phase(context.getResources().getString(R.string.learnTitle), R.color.learnPhase,LearnActivity.class),
                            new Phase(context.getResources().getString(R.string.draftTitle), R.color.draftPhase, PagerBaseActivity.class),
                            new Phase(context.getResources().getString(R.string.communityCheckTitle), R.color.comunityCheckPhase, PagerBaseActivity.class),
                            new Phase(context.getResources().getString(R.string.consultantCheckTitle), R.color.consultantCheckPhase, PagerBaseActivity.class),
                            new Phase(context.getResources().getString(R.string.dramatizationTitle), R.color.dramatizationPhase, PagerBaseActivity.class),
                            new Phase(context.getResources().getString(R.string.exportTitle), R.color.exportPhase, ExportActivity.class)};
    }

    /**
     * Returns the story name
     * @return String for storyName
     */
    public static String getStoryName() {
        return storyName;
    }

    /**
     * Sets the storyName
     * @param name: String to set storyName with
     */
    public static void setStoryName(String name) {
        storyName = name;
    }

    /**
     * Retusn the current Phase the story is in
     * @return Phase
     */
    public static Phase getPhase() {
        return phase;
    }

    /**
     * gets the Next Phase and sets the current phase to that phase
     * @return Phase: returns the next phase
     */
    public static Phase getNextPhase() {
        if(phaseIndex < phases.length - 1) {
            phaseIndex++;
        }
        phase = phases[phaseIndex];
        return phase;
    }

    /**
     * gets the previous Phase and sets the current phase to that phase
     * @return Phase: returns the previous phase
     */
    public static Phase getPrevPhase() {
        if(phaseIndex > 0) {
            phaseIndex--;
        }
        phase = phases[phaseIndex];
        return phase;
    }


}

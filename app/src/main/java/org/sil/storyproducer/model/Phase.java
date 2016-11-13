package org.sil.storyproducer.model;

/**
 * The business object for phases that are part of the story
 */
public class Phase {

    private String phaseTitle;
    private int phaseColor;
    private Class phaseCls;

    /**
     * Constructor for the Phase
     * @param title: the title for the phase
     * @param clr: the color for the phase
     * @param cls: class for the activity of the phase
     */
    public Phase(String title, int clr, Class cls) {
        phaseTitle = title;
        phaseColor = clr;
        phaseCls = cls;
    }

    /**
     * get the title for the phase
     * @return : return the title
     */
    public String getPhaseTitle() {
        return phaseTitle;
    }

    /**
     * get the color for the phase
     * @return : return the color
     */
    public int getPhaseColor() {
        return phaseColor;
    }

    /**
     * get the class for the activity of the phase
     * @return : return the class
     */
    public Class getPhaseClass() {
        return phaseCls;
    }
}

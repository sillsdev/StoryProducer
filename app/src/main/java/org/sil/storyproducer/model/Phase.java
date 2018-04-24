package org.sil.storyproducer.model;

/**
 * The business object for phases that are part of the story
 */
public class Phase {

    public enum Type {
        LEARN, DRAFT, COMMUNITY_CHECK, CONSULTANT_CHECK, DRAMATIZATION, CREATE, SHARE, BACKT, WHOLE_STORY, REMOTE_CHECK
    }

    private String phaseTitle;
    private int phaseColor;
    private Class phaseCls;
    private Type type;

    /**
     * Constructor for the Phase
     * @param title the title for the phase
     * @param clr the color for the phase
     * @param cls class for the activity of the phase
     */
    public Phase(String title, int clr, Class cls, Type type) {
        phaseTitle = title;
        phaseColor = clr;
        phaseCls = cls;
        this.type = type;
    }

    /**
     * get the title for the phase
     * @return return the title
     */
    public String getTitle() {
        return phaseTitle;
    }

    /**
     * get the color for the phase
     * @return return the color
     */
    public int getColor() {
        return phaseColor;
    }

    /**
     * get the class for the activity of the phase
     * @return return the class
     */
    public Class getTheClass() {
        return phaseCls;
    }

    public Type getType() {
        return type;
    }
}

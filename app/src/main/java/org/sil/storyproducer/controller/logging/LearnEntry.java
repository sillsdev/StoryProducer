package org.sil.storyproducer.controller.logging;

import org.sil.storyproducer.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by user on 1/15/2017.
 */

public class LearnEntry extends LogEntry {
    private static long MIN_DURATION = 500;

    private int startSlide;
    private int endSlide;
    private long duration;
    private static final DecimalFormat twoDecPlaces = new DecimalFormat("0.00");



    private LearnEntry(long dateTime, int start, int end, long duration){
        super(dateTime, Phase.Learn, R.color.learn_phase);
        startSlide =start;
        endSlide =end;
        this.duration=duration;
    }

    public String getDescription(){
        String ret = null;
        if(startSlide ==endSlide ){
            ret = "Slide "+(startSlide+1);
        } else {
            ret = "Slides "+(startSlide+1)+"-"+(endSlide+1);
        }
        ret+=" ("+formatDuration(duration)+")";
        return ret;
    }

    private String formatDuration(long duration){
        if(duration<1000){
            return ("<1 sec");
        }
        int roundedSecs = (int) ((duration/1000.0)+0.5);
        int mins = roundedSecs / 60;
        String minString = "";
        if(mins > 0){
            minString = mins+" min ";
        }
        return minString+(roundedSecs % 60)+" sec";
    }

    /**
     * Saves the log entry and returns true, if and only if it passes the filter.
     * @param start
     * @param end
     * @param duration
     * @return
     */
    public static boolean saveFilteredLogEntry(int start, int end, long duration){
        System.out.println("duration: "+duration);
        if (duration < MIN_DURATION){
            return false;
        }
        Logging.saveLogEntry(new LearnEntry(System.currentTimeMillis(), start, end, duration));
        return true;
    }

}
